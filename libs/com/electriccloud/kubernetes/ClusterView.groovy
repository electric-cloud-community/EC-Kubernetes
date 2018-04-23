package com.electriccloud.kubernetes

import com.electriccloud.domain.ClusterNode
import com.electriccloud.domain.ClusterNodeImpl
import com.electriccloud.domain.ClusterTopology
import com.electriccloud.domain.ClusterTopologyImpl
import com.electriccloud.domain.Topology
import com.electriccloud.errors.EcException
import com.electriccloud.errors.ErrorCodes
import groovy.json.JsonOutput

class ClusterView {
    String clusterName
    String clusterId
    Client kubeClient

    private static final String RUNNING = 'Running'
    private static final String FAILED = 'Failed'
    private static final String SUCCEEDED = 'Succeeded'
    private static final String PENDING = 'Pending'
    private static final String CRUSH_LOOP = 'CrushLoopBackoff'

    private static final String TYPE_CLUSTER = 'ecp-cluster'
    private static final String TYPE_NAMESPACE = 'ecp-namespace'
    private static final String TYPE_SERVICE = 'ecp-service'
    private static final String TYPE_POD = 'ecp-pod'
    private static final String TYPE_CONTAINER = 'ecp-container'
    private static final String TYPE_EF_CLUSTER = 'cluster'

    private static final String TYPE_STRING = 'string'
    private static final String TYPE_MAP = 'map'
    private static final String TYPE_LINK = 'link'
    private static final String TYPE_TEXTAREA = 'textarea'
    private static final String TYPE_DATE = 'date'

    private static final String ATTRIBUTE_MASTER_VERSION = 'Master Version'
    private static final String ATTRIBUTE_STATUS = 'Status'
    private static final String ATTRIBUTE_LABELS = 'Labels'
    private static final String ATTRIBUTE_SERVICE_TYPE = 'ServiceType'
    private static final String ATTRIBUTE_ENDPOINT = 'Endpoint'
    private static final String ATTRIBUTE_RUNNING_PODS = 'Running Pods'
    private static final String ATTRIBUTE_VOLUMES = 'Volumes'
    private static final String ATTRIBUTE_START_TIME = 'Start time'
    private static final String ATTRIBUTE_IMAGE = 'Image'
    private static final String ATTRIBUTE_NODE_NAME = 'Node Name'

    @Lazy
    private kubeNamespaces = { kubeClient.getNamespaces() }()

    @Lazy
    private kubeServices = { kubeClient.getAllServices() }()

    @Lazy
    private kubeDeployments = { kubeClient.getAllDeployments() }()

    @Lazy
    private kubePods = { kubeClient.getAllPods() }()


    ClusterTopology getRealtimeClusterTopology() {
        ClusterTopology topology = new ClusterTopologyImpl()
        topology.addNode(getEFClusterId(), TYPE_EF_CLUSTER, getEFClusterName())
        topology.addLink(getEFClusterId(), getClusterId())
        topology.addNode(buildClusterNode())

        kubeNamespaces.findAll { !isSystemNamespace(it) }.each { namespace ->
            topology.addNode(buildNamespaceNode(namespace))
            topology.addLink(getClusterId(), getNamespaceId(namespace))

            def services = kubeServices.findAll { kubeService ->
                kubeService.metadata.namespace == namespace.metadata.name
            }

            services.findAll { !isSystemService(it) }.each { service ->
                def pods = getServicePodsTopology(service)
                topology.addLink(getNamespaceId(namespace), getServiceId(service))
                topology.addNode(buildServiceNode(service, pods))

                pods.each { pod ->
                    topology.addLink(getServiceId(service), getPodId(service, pod))
                    topology.addNode(buildPodNode(service, pod))

                    def containers = pod.spec.containers
                    containers.each { container ->
                        topology.addLink(getPodId(service, pod), getContainerId(service, pod, container))
                        topology.addNode(buildContainerNode(service, pod, container))
                    }
                }
            }
        }
        topology
    }

    def isSystemNamespace(namespace) {
        def name = getNamespaceName(namespace)
        name == 'kube-public' || name == 'kube-system'
    }

    def getPodsStatus(pods) {
        def finalStatus
        pods.each { pod ->
            def status = pod.status
            def phase = status.phase
            if (phase != RUNNING) {
                finalStatus = phase
            }
        }
        if (!finalStatus) {
            finalStatus = RUNNING
        }
        finalStatus
    }

    def getPodsRunning(pods) {
        def running = 0
        def all = 0
        pods.each { pod ->
            def status = pod.status
            def phase = status.phase
            all += 1
            if (phase == RUNNING) {
                running += 1
            }
        }
        "${running} of ${all}"
    }

    def getContainerStatus(pod, container) {
        def name = container.name
        if (!pod.status.containerStatuses) {
            return FAILED
        }
        def containerStatus = pod.status.containerStatuses.find { it.name == name }
        if (!containerStatus) {
            throw EcException.code(ErrorCodes.RealtimeClusterLookupFailed).message("No container status found for name ${name}").build()
        }
        def states = containerStatus?.state.keySet()
        if (states.size() == 1) {
            return states[0]
        }
        throw EcException
            .code(ErrorCodes.RealtimeClusterLookupFailed)
            .message("Container has more than one status: ${containerStatus}")
            .location(this.class.canonicalName)
            .build()
    }

    def isSystemService(service) {
        service.metadata.name == 'kubernetes'
    }

    def getServicePods(def service) {
        def selector = service.spec?.selector
        assert selector
        def selectorString = selector.collect { k, v ->
            "${k}=${v}"
        }.join(',')
        def namespace = service.metadata.namespace
        def deployments = kubeClient.getDeployments(namespace, selectorString)
        def pods = []
        deployments.each { deployment ->
            def labels = deployment?.spec?.selector?.matchLabels ?: deployment?.spec?.template?.metadata?.labels

            def podSelectorString = labels.collect { k, v ->
                "${k}=${v}"
            }.join(',')
            def deploymentPods = kubeClient.getPods(namespace, podSelectorString)
            pods.addAll(deploymentPods)
        }

        pods
    }


    def getServicePodsTopology(def service) {
        def serviceSelector = service?.spec?.selector
        def pods = []

        def match = { selector, object ->
            if (!selector) {
                return false
            }
            def labels = object.metadata?.labels
            def match = true
            selector.each { k, v ->
                if (labels.get(k) != v) {
                    match = false
                }
            }
            match
        }

        def deployments = kubeDeployments.findAll {
            it.metadata.namespace == service.metadata.namespace &&
                    match(serviceSelector, it)
        }
        deployments.each { deploy ->
            def deploySelector = deploy?.spec?.selector?.matchLabels ?: deploy?.spec?.template?.metadata?.labels
            pods.addAll(kubePods.findAll {
                it.metadata.namespace == service.metadata.namespace &&
                        match(deploySelector, it)
            })
        }

        pods
    }

    def errorChain(Closure... closures) {
        def first = closures.head()
        closures = closures.tail()
        def result
        try {
            result = first.call()
        } catch (Throwable e) {
            if (closures.size()) {
                errorChain(closures)
            } else {
                throw e
            }
        }
        result
    }

    def getPodDetails(String podName) {
        podName = podName.replaceAll("${clusterName}::", '')
        def (namespace, podId) = podName.split('::')
        def pod = kubeClient.getPod(namespace, podId)
        def status = pod?.status?.phase ?: 'UNKNOWN'
        def labels = pod?.metadata?.labels
        def startTime = pod?.metadata?.creationTimestamp
        def nodeName = pod?.spec?.nodeName

        def node = new ClusterNodeImpl(podName, TYPE_POD, podId)

        if (status){
            node.addAttribute(ATTRIBUTE_STATUS, status, TYPE_STRING)
        }
        if (labels){
            node.addAttribute(ATTRIBUTE_LABELS, labels, TYPE_MAP)
        }
        if (startTime){
            node.addAttribute(ATTRIBUTE_START_TIME, startTime, TYPE_DATE)
        }
        if (nodeName){
            node.addAttribute(ATTRIBUTE_NODE_NAME, nodeName, TYPE_STRING)
        }

        node
    }

    def getContainerDetails(String containerName) {
        containerName = containerName.replaceAll("${clusterName}::", '')
        def (namespace, podId, containerId) = containerName.split('::')
        def pod = kubeClient.getPod(namespace, podId)
        def container = pod.spec?.containers.find {
            it.name == containerId
        }
        if (!container) {
            throw EcException
                .code(ErrorCodes.RealtimeClusterLookupFailed)
                .message("Container ${containerId} was not found in pod ${podId}")
                .location(this.class.canonicalName)
                .build()
        }
        def status = getContainerStatus(pod, container)
        def ports = container.ports?.collectEntries {
            def value = "${it.containerPort}/${it.protocol}"
            [(it.name): value]
        }
        def environmentVariables = container.env?.collectEntries {
            [(it.name): it.value]
        }

        def startedAt
        pod.status?.containerStatuses?.each {
            if (it.name == containerId) {
                startedAt = it?.state?.running?.startedAt
            }
        }
        def volumeMounts = container.volumeMounts?.collectEntries {
            def readOnlySuffix = it.readOnly ? '(read only)' : ''
            def value = "${it.mountPath} $readOnlySuffix"
            [(it.name): value]
        }

        def node = new ClusterNodeImpl(containerName, TYPE_CONTAINER, containerId)
        node.addAction('View Logs', 'viewLogs', TYPE_TEXTAREA)
        node.addAttribute(ATTRIBUTE_STATUS, status, TYPE_STRING)
        if (startedAt) {
            node.addAttribute(ATTRIBUTE_START_TIME, startedAt, TYPE_DATE)
        }
        if (environmentVariables && environmentVariables.size()) {
            node.addAttribute('Environment Variables', environmentVariables, TYPE_MAP)
        }
        if (ports) {
            node.addAttribute('Ports', ports, TYPE_MAP)
        }
        if (volumeMounts) {
            node.addAttribute("Volume Mounts", volumeMounts, TYPE_MAP)
        }
        def usage

//        Different k8s setups and versions may have different URLs for metrics server or even don't have one at all
//        So let's poke them all, maybe we are lucky
        try {
            errorChain(
                {
                    usage = kubeClient.getPodMetricsHeapster(namespace, podId)
                },
                {
                    println "Switching to metrics-server - beta version"
                    usage = kubeClient.getPodMetricsServerBeta(namespace, podId)
                },
                {
                    println "Switching to metrics-server-alpha"
                    usage = kubeClient.getPodMetricsServerAlpha(namespace, podId)
                }
            )
        } catch (Throwable e) {
            println "Cannot get metrics: ${e.message}"
        }

        def memory
        def cpu

        usage?.containers?.each {
            if (it.name == containerId) {
                cpu = it.usage?.cpu
                memory = it.usage?.memory
            }
        }

        if (cpu) {
            node.addAttribute('CPU', cpu, TYPE_STRING, 'Resource Usage')
        }
        if (memory) {
            node.addAttribute('Memory', memory, TYPE_STRING, 'Resource Usage')
        }

        node
    }

    String getNamespaceId(namespace) {
        "${this.clusterName}::${getNamespaceName(namespace)}"
    }

    //future
    def getClusterLabels() {
        null
    }

    def getNamespaceLabels(namespace) {
        namespace?.metadata?.labels
    }

    String getClusterId() {
        this.clusterName
    }

    String getEFClusterName() {
        this.clusterName
    }

    String getEFClusterId() {
        this.clusterId
    }

    String getClusterName() {
        kubeClient.endpoint
    }

    String getServiceName(service) {
        "${service.metadata.name}"
    }

    String getServiceId(service) {
        def namespaceId = "${this.clusterName}::${service.metadata.namespace}"
        "${namespaceId}::${service.metadata.name}"
    }

    String getServiceEndpoint(service) {
//        TODO ingress
        "${service.spec.loadBalancerIP}:${service?.spec?.ports?.getAt(0)?.port}"
    }

    String getPodId(service, pod) {
        def namespaceId = "${this.clusterName}::${service.metadata.namespace}"
        "${namespaceId}::${pod.metadata.name}"
    }

    String getContainerId(service, pod, container) {
        "${getPodId(service, pod)}::${container.name}"
    }

    def buildClusterNode() {
        ClusterNode node = new ClusterNodeImpl(getClusterId(), TYPE_CLUSTER, getClusterName())
        node
    }

    def buildPodNode(service, pod) {
        def name = pod.metadata.name
        def status = pod.status.phase
        ClusterNode node = new ClusterNodeImpl(getPodId(service, pod), TYPE_POD, name)
        node.setStatus(status)
        node
    }

    def buildServiceNode(Map service, pods) {
        def name = service.metadata.name
        def status = getPodsStatus(pods)
        ClusterNode node = new ClusterNodeImpl(getServiceId(service), TYPE_SERVICE, name)
        node.setStatus(status)
        def efId = service.metadata?.labels?.find { it.key == 'ec-svc-id' }?.value
        if (efId) {
            node.setElectricFlowIdentifier(efId)
        }
        node
    }

    def buildContainerNode(service, pod, container) {
        def node = new ClusterNodeImpl(getContainerId(service, pod, container), TYPE_CONTAINER, container.name)
        node.setStatus(getContainerStatus(pod, container))
        return node
    }


    def getContainerLogs(String containerName) {
        containerName = containerName.replaceAll("${clusterName}::", '')
        def (namespace, podId, containerId) = containerName.split('::')
        def logs = kubeClient.getContainerLogs(namespace, podId, containerId)
        logs
    }


    def buildNamespaceNode(namespace) {
        def name = getNamespaceName(namespace)
        new ClusterNodeImpl(getNamespaceId(namespace), TYPE_NAMESPACE, name)
    }

    def getClusterDetails() {
        def node = new ClusterNodeImpl(getClusterName(), TYPE_CLUSTER, getClusterId())

        def version = kubeClient.getClusterVersion()
        def labels = getClusterLabels()
        def endpoint = getClusterName()
        node.addAttribute(ATTRIBUTE_ENDPOINT, endpoint, TYPE_LINK)

        if (version) {
            node.addAttribute(ATTRIBUTE_MASTER_VERSION, version.toString(), TYPE_STRING)
        }
        if (labels) {
            node.addAttribute(ATTRIBUTE_LABELS, labels, TYPE_MAP)
        }
        node
    }

    def getNamespaceDetails(namespaceName) {
        namespaceName = namespaceName.replaceAll("${clusterName}::", '')
        def namespace = kubeClient.getNamespace(namespaceName)
        def namespaceId = getNamespaceId(namespace)

        def labels = getNamespaceLabels(namespace)

        def node = new ClusterNodeImpl(namespaceName, TYPE_NAMESPACE, namespaceId)

        if (labels) {
            node.addAttribute(ATTRIBUTE_LABELS, labels, TYPE_MAP)
        }

        node
    }

    def getServiceDetails(serviceName) {
        serviceName = serviceName.replaceAll("${clusterName}::", '')
        def (namespace, serviceId) = serviceName.split('::')
        def service = kubeClient.getService(namespace, serviceId)
        def pods = getServicePods(service)

        // The constructor takes parameters in this order: id, type, name
        // But argument name 'serviceName' really represents the fully qualified service-id
        // and 'serviceId' is the actual service name. That is why the order
        // below will appear swapped but is it the correct order.
        def node = new ClusterNodeImpl(/*node id*/ serviceName, TYPE_SERVICE, /*node name*/ serviceId)

        def efId = service.metadata?.labels?.find { it.key == 'ec-svc-id' }?.value
        if (efId) {
            node.setElectricFlowIdentifier(efId)
        }

        def status = getPodsStatus(pods)
        def labels = service?.metadata?.labels
        def type = service?.spec?.type
        def endpoint = getServiceEndpoint(service)
        def runningPods = getPodsRunning(pods)
        def volumes = kubeClient.getServiceVolumes(namespace, serviceId)

        if (status) {
            node.addAttribute(ATTRIBUTE_STATUS, status, TYPE_STRING)
        }
        if (labels) {
            node.addAttribute(ATTRIBUTE_LABELS, labels, TYPE_MAP)
        }
        if (type) {
            node.addAttribute(ATTRIBUTE_SERVICE_TYPE, type, TYPE_STRING)
        }
        if (endpoint) {
            node.addAttribute(ATTRIBUTE_ENDPOINT, endpoint, TYPE_LINK)
        }
        if (runningPods) {
            node.addAttribute(ATTRIBUTE_RUNNING_PODS, runningPods.toString(), TYPE_STRING)
        }
        if (volumes) {
            node.addAttribute(ATTRIBUTE_VOLUMES, volumes, TYPE_TEXTAREA)
        }

        node
    }

    def getNamespaceName(namespace) {
        def name = namespace?.metadata?.name
        assert name
        name
    }


}
