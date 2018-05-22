package com.electriccloud.kubernetes

import com.electriccloud.domain.ClusterNode
import com.electriccloud.domain.ClusterNodeImpl
import com.electriccloud.domain.ClusterTopology
import com.electriccloud.domain.ClusterTopologyImpl
import com.electriccloud.domain.Topology
import com.electriccloud.errors.EcException
import com.electriccloud.errors.ErrorCodes
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import javax.xml.bind.DatatypeConverter

class ClusterView {
    String projectName
    String environmentName
    String clusterName
    String clusterId
    Client kubeClient

    private static final String RUNNING = 'Running'
    private static final String FAILED = 'Failed'
    private static final String SUCCEEDED = 'Succeeded'
    private static final String PENDING = 'Pending'
    private static final String CRUSH_LOOP = 'CrushLoopBackoff'
    private static final String TERMINATING = 'Terminating'

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

    private static final String DISPLAY_CLUSTER = 'Kubernetes Cluster'
    private static final String DISPLAY_NAMESPACE = 'Kubernetes Namespace'
    private static final String DISPLAY_POD = 'Kubernetes Pod'
    private static final String DISPLAY_CONTAINER = 'Kubernetes Container'
    private static final String DISPLAY_SERVICE = 'Kubernetes Service'

    private static final String ATTRIBUTE_MASTER_VERSION = 'Master Version'
    private static final String ATTRIBUTE_STATUS = 'Status'
    private static final String ATTRIBUTE_LABELS = 'Labels'
    private static final String ATTRIBUTE_SERVICE_TYPE = 'ServiceType'
    private static final String ATTRIBUTE_ENDPOINT = 'Endpoint'
    private static final String ATTRIBUTE_RUNNING_PODS = 'Running Pods'
    private static final String ATTRIBUTE_VOLUMES = 'Volumes'
    private static final String ATTRIBUTE_START_TIME = 'Start time'
    private static final String ATTRIBUTE_IMAGE = 'Image'
    private static final String ATTRIBUTE_NODE_NAME = 'Node'
    private static final String ATTRIBUTE_ERROR = 'Error'

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

        kubeNamespaces.findAll { !isSystemNamespace(it) && isValidNamespace(it) }.each { namespace ->
            topology.addNode(buildNamespaceNode(namespace))
            topology.addLink(getClusterId(), getNamespaceId(namespace))

            def services = kubeServices.findAll { kubeService ->
                kubeService.metadata.namespace == namespace.metadata.name &&
                    isValidService(kubeService) && !isSystemService(kubeService)
            }

            services.each { service ->
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

    def isValidNamespace(Map namespace) {
        if (namespace.status?.phase == TERMINATING) {
            return false
        }
        return true
    }

    def isValidService(Map service) {
        return true
    }

    def isValidPod(Map pod) {
        //allow a failed pod to be returned.
        //we should then handle 404 not found errors
        //when retrieving pod and container details.
        /*if (pod?.status?.phase == FAILED) {
            return false
        }*/
        return true
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

    def getServicePods(def service, boolean skipDeleted = false) {
        def selector = service.spec?.selector
        if (!selector) {
            return []
        }
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
            def deploymentPods = []
            if (skipDeleted) {
                try {
                    deploymentPods = kubeClient.getPods(namespace, podSelectorString)
                } catch(Throwable e) {
                    if (e.message =~ /404/) {
                        deploymentPods = []
                    }
                    else {
                        throw e
                    }
                }
            }
            else {
                deploymentPods = kubeClient.getPods(namespace, podSelectorString)
            }
            pods.addAll(deploymentPods.findAll { isValidPod(it)} )
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
                        match(deploySelector, it) && isValidPod(it)
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
        def objectIdentifier = podName
        podName = podName.replaceAll("${getClusterId()}::", '')
        def namespace, podId
        try{
            (namespace, podId) = podName.split('::')
        }
        catch (Throwable e){
            throw EcException
                    .code(ErrorCodes.InvalidArgument)
                    .message("Invalid object identifier for pod: ${objectIdentifier}")
                    .location(this.class.getCanonicalName())
                    .build()
        }
        def node = createClusterNode(podName, TYPE_POD, podId)
        def pod
        try {
            pod = kubeClient.getPod(namespace, podId)
        } catch (Throwable e) {
            if (e.message =~ /404/) {
                node.addAttribute(ATTRIBUTE_ERROR, "Kubernetes pod '${podId}' does not exist in namespace '${namespace}'".toString(), TYPE_STRING)
                return node
            }
            else {
                throw e
            }
        }
        def status = pod?.status?.phase ?: 'UNKNOWN'
        def labels = pod?.metadata?.labels
        def startTime = pod?.metadata?.creationTimestamp
        def nodeName = pod?.spec?.nodeName

        node.addAction('View Logs', 'viewLogs', TYPE_TEXTAREA)
        if (node.metaClass.respondsTo(node, "setDisplayType", String)) {
            node.setDisplayType(DISPLAY_POD)
        }
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
        def objectIdentifier = containerName
        containerName = containerName.replaceAll("${getClusterId()}::", '')
        def namespace, podId, containerId
        try{
            (namespace, podId, containerId) = containerName.split('::')
        }
        catch (Throwable e){
            throw EcException
                    .code(ErrorCodes.InvalidArgument)
                    .message("Invalid object identifier for container: ${objectIdentifier}")
                    .location(this.class.getCanonicalName())
                    .build()
        }

        def node = createClusterNode(containerName, TYPE_CONTAINER, containerId)
        def pod
        try {
            pod = kubeClient.getPod(namespace, podId)
        } catch (Throwable e) {
            if (e.message =~ /404/) {
                node.addAttribute(ATTRIBUTE_ERROR, "Kubernetes pod '${podId}' that the container '${containerId}' belonged to does not exist in namespace '${namespace}'", TYPE_STRING)
                return node
            }
            else {
                throw e
            }
        }
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

        def image
        pod.status?.containerStatuses?.each {
            if (it.name == containerId) {
                image = it?.image
            }
        }
        def startTime = pod?.status?.startTime
        def nodeName = pod?.spec?.nodeName

        if (node.metaClass.respondsTo(node, "setDisplayType", String)) {
            node.setDisplayType(DISPLAY_CONTAINER)
        }
        node.addAction('View Logs', 'viewLogs', TYPE_TEXTAREA)
        node.addAttribute(ATTRIBUTE_STATUS, status, TYPE_STRING)
        if (image) {
            node.addAttribute(ATTRIBUTE_IMAGE, image, TYPE_STRING)
        }
        if (startTime) {
            node.addAttribute(ATTRIBUTE_START_TIME, startTime, TYPE_DATE)
        }
        if (nodeName) {
            node.addAttribute(ATTRIBUTE_NODE_NAME, nodeName, TYPE_STRING)
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
        "${this.getClusterId()}::${getNamespaceName(namespace)}"
    }

    //future
    def getClusterLabels() {
        null
    }

    def getNamespaceLabels(namespace) {
        namespace?.metadata?.labels
    }

    String getClusterId() {
        //A cluster is uniquely identified by its end-point
        // So we use it as the cluster id
        kubeClient.endpoint
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
        def namespaceId = "${this.getClusterId()}::${service.metadata.namespace}"
        "${namespaceId}::${service.metadata.name}"
    }

    String getServiceEndpoint(service) {
        String endpoint
        switch (service?.spec?.type) {
            case 'LoadBalancer':
                def ingress = service?.status?.loadBalancer?.ingress?.find {
                    it.hostname || it.ip
                }
                String host
                if (ingress) {
                    host = ingress.hostname ?: ingress.ip
                }

                if (!host) {
                    host = service?.spec?.loadBalancerIP
                }
                if (!host) {
                    host = '<undefined>'
                }
                String port = service?.spec?.ports?.getAt(0)?.port
                endpoint = "${host}:${port}"
                break
            case 'NodePort':
                String host = new URL(kubeClient.endpoint).host
                String port = service?.spec?.ports?.find({ it.protocol == 'TCP'})?.port
                endpoint = port ? "${host}:${port}" : host
                break
            default:
                String host = new URL(kubeClient.endpoint).host
                String port = service?.spec?.ports?.getAt(0)?.port
                endpoint = port ? "${host}:${port}" : host
                break
        }
        return "http://${endpoint}"
    }

    String getPodId(service, pod) {
        def namespaceId = "${this.getClusterId()}::${service.metadata.namespace}"
        "${namespaceId}::${pod.metadata.name}"
    }

    String getContainerId(service, pod, container) {
        "${getPodId(service, pod)}::${container.name}"
    }

    def buildClusterNode() {
        ClusterNode node = createClusterNode(getClusterId(), TYPE_CLUSTER, getClusterName())
        if (node.metaClass.respondsTo(node, "setDisplayType", String)) {
            node.setDisplayType(DISPLAY_CLUSTER)
        }
        node
    }

    def buildPodNode(service, pod) {
        def name = pod.metadata.name
        def status = pod.status.phase
        ClusterNode node = createClusterNode(getPodId(service, pod), TYPE_POD, name)
        node.setStatus(status)
        if (node.metaClass.respondsTo(node, "setDisplayType", String)) {
            node.setDisplayType(DISPLAY_POD)
        }
        node
    }

    def buildServiceNode(Map service, pods) {
        def name = service.metadata.name
        def status = getPodsStatus(pods)
        ClusterNode node = createClusterNode(getServiceId(service), TYPE_SERVICE, name)
        node.setStatus(status)
        def efId = service.metadata?.labels?.find { it.key == 'ec-svc-id' }?.value
        if (efId) {
            node.setElectricFlowIdentifier(efId)
        }
        if (node.metaClass.respondsTo(node, "setDisplayType", String)) {
            node.setDisplayType(DISPLAY_SERVICE)
        }
        node
    }

    def buildContainerNode(service, pod, container) {
        def node = createClusterNode(getContainerId(service, pod, container), TYPE_CONTAINER, container.name)
        node.setStatus(getContainerStatus(pod, container))
        if (node.metaClass.respondsTo(node, "setDisplayType", String)) {
            node.setDisplayType(DISPLAY_CONTAINER)
        }
        return node
    }

    def getContainerLogs(String containerName) {
        def objectIdentifier = containerName
        containerName = containerName.replaceAll("${getClusterId()}::", '')
        def namespace, podId, containerId
        try{
            (namespace, podId, containerId) = containerName.split('::')
        }
        catch (Throwable e){
            throw EcException
            .code(ErrorCodes.InvalidArgument)
            .message("Invalid object identifier for container: ${objectIdentifier}")
            .location(this.class.getCanonicalName())
            .build()
        }
        assert namespace != null
        assert podId != null
        assert containerId != null
        def logs = kubeClient.getContainerLogs(namespace, podId, containerId)
        logs
    }

    def getPodLogs(String podName) {
        def objectIdentifier = podName
        podName = podName.replaceAll("${getClusterId()}::", '')
        def namespace, podId
        try{
            (namespace, podId) = podName.split('::')
        }
        catch (Throwable e){
            throw EcException
                    .code(ErrorCodes.InvalidArgument)
                    .message("Invalid object identifier for pod: ${objectIdentifier}")
                    .location(this.class.getCanonicalName())
                    .build()
        }
        assert namespace != null
        assert podId != null
        def logs = kubeClient.getPodLogs(namespace, podId)
        logs
    }

    def buildNamespaceNode(namespace) {
        def name = getNamespaceName(namespace)
        def node = createClusterNode(getNamespaceId(namespace), TYPE_NAMESPACE, name)
        if (node.metaClass.respondsTo(node, "setDisplayType", String)) {
            node.setDisplayType(DISPLAY_NAMESPACE)
        }
        node
    }

    def getClusterDetails() {
        def node = createClusterNode(getClusterId(), TYPE_CLUSTER, getClusterName())

        def version = kubeClient.getClusterVersion()
        def labels = getClusterLabels()
        def endpoint = getClusterId()

        if (node.metaClass.respondsTo(node, "setDisplayType", String)) {
            node.setDisplayType(DISPLAY_CLUSTER)
        }
        node.addAttribute(ATTRIBUTE_ENDPOINT, endpoint, TYPE_LINK)
        if (version) {
            node.addAttribute(ATTRIBUTE_MASTER_VERSION, version.toString(), TYPE_STRING)
        }
        if (labels) {
            node.addAttribute(ATTRIBUTE_LABELS, labels, TYPE_MAP)
        }
        node
    }

    def getNamespaceDetails(String namespaceId) {
        String namespaceName = namespaceId.replaceAll("${getClusterId()}::", '')
        def node = createClusterNode(namespaceId, TYPE_NAMESPACE, namespaceName)
        def namespace
        try {
            namespace = kubeClient.getNamespace(namespaceName)
        } catch (Throwable e) {
            if (e.message =~ /404/) {
                node.addAttribute(ATTRIBUTE_ERROR, "Kubernetes namespace '${namespaceName}' does not exist", TYPE_STRING)
                return node
            }
            else {
                throw e
            }
        }
        def status = namespace.status?.phase
        if (node.metaClass.respondsTo(node, "setDisplayType", String)) {
            node.setDisplayType(DISPLAY_NAMESPACE)
        }
        if (status) {
            node.addAttribute("Status", status, TYPE_STRING)
        }

        def labels = getNamespaceLabels(namespace)

        if (labels) {
            node.addAttribute(ATTRIBUTE_LABELS, labels, TYPE_MAP)
        }

        def age = getNamespaceAge(namespace)
        if (age) {
            node.addAttribute("Age", age, TYPE_STRING)
        }

        node
    }

    String getNamespaceAge(namespace) {
        def creationTimestamp = namespace?.metadata?.creationTimestamp
        if (!creationTimestamp) {
            return null
        }
        Calendar calendar = DatatypeConverter.parseDateTime(creationTimestamp)
        Calendar now = Calendar.getInstance()
        def age = now - calendar
        def retval = "${age} day(s)"
        return retval.toString()
    }

    def getServiceDetails(serviceName) {
        def objectIdentifier = serviceName
        serviceName = serviceName.replaceAll("${getClusterId()}::", '')
        def namespace, serviceId

        try{
            (namespace, serviceId) = serviceName.split('::')
        }
        catch (Throwable e){
            throw EcException
                    .code(ErrorCodes.InvalidArgument)
                    .message("Invalid object identifier for service: ${objectIdentifier}")
                    .location(this.class.getCanonicalName())
                    .build()
        }

        def node = createClusterNode(/*node id*/ serviceName, TYPE_SERVICE, /*node name*/ serviceId)
        def service
        try {
            service = kubeClient.getService(namespace, serviceId)
        } catch (Throwable e) {
            if (e.message =~ /404/) {
                node.addAttribute(ATTRIBUTE_ERROR, "Kubernetes service '${serviceId}' does not exist in namespace '${namespace}'", TYPE_STRING)
                return node
            }
            else {
                throw e
            }
        }
        def pods = getServicePods(service, true)

        // The constructor takes parameters in this order: id, type, name
        // But argument name 'serviceName' really represents the fully qualified service-id
        // and 'serviceId' is the actual service name. That is why the order
        // below will appear swapped but is it the correct order.


        def efId = service.metadata?.labels?.find { it.key == 'ec-svc-id' }?.value
        if (efId) {
            node.setElectricFlowIdentifier(efId)
        }

        def status = getPodsStatus(pods)
        def labels = service?.metadata?.labels
        def type = service?.spec?.type
        def endpoint = getServiceEndpoint(service)
        def runningPods = getPodsRunning(pods)
        def volumes
        try {
            volumes = kubeClient.getServiceVolumes(namespace, serviceId)
        } catch (Throwable e) {
            if (e.message =~ /404/) {
//                Do nothing
            }
            else {
                throw e
            }
        }

        if (node.metaClass.respondsTo(node, "setDisplayType", String)) {
            node.setDisplayType(DISPLAY_SERVICE)
        }
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
            node.addAttribute(ATTRIBUTE_VOLUMES, new JsonBuilder(volumes).toPrettyString(), TYPE_TEXTAREA)
        }

        node
    }

    def getNamespaceName(namespace) {
        def name = namespace?.metadata?.name
        assert name
        name
    }

    def createClusterNode(nodeId, nodeType, nodeName) {
        def node = new ClusterNodeImpl(nodeId, nodeType, nodeName)
        node.setElectricFlowClusterName(clusterName)
        node.setElectricFlowEnvironmentName(environmentName)
        node.setElectricFlowProjectName(projectName)
        node
    }

}
