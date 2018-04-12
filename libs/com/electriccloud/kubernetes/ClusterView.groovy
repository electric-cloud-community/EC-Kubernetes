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


    ClusterTopology getRealtimeClusterTopology() {
        def namespaces = kubeClient.getNamespaces()

        ClusterTopology topology = new ClusterTopologyImpl()
        topology.addNode(getEFClusterId(), TYPE_EF_CLUSTER, getEFClusterName())
        topology.addLink(getEFClusterId(), getClusterId())
        topology.addNode(buildClusterNode())

        namespaces.findAll { !isSystemNamespace(it) }.each { namespace ->
            if (!isSystemNamespace(namespace)) {
                topology.addNode(buildNamespaceNode(namespace))
                topology.addLink(getClusterId(), getNamespaceId(namespace))

                def services = kubeClient.getServices(getNamespaceName(namespace))
                services.findAll { !isSystemService(it) }.each { service ->
                    def pods = getServicePods(service)
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

    def getContainerStatus(pod, container) {
        def name = container.name
        if (!pod.status.containerStatuses) {
            return FAILED
        }
        def containerStatus = pod.status.containerStatuses.find { it.name == name }
        if (!containerStatus) {
            throw EcException.code(ErrorCodes.UnknownError).message("No container status found for name ${name}").build()
        }
        def states = containerStatus?.state.keySet()
        if (states.size() == 1) {
            return states[0]
        }
        throw EcException
                .code(ErrorCodes.UnknownError)
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
            def labels = deployment?.spec?.template?.metadata?.labels

            def podSelectorString = labels.collect { k, v ->
                "${k}=${v}"
            }.join(',')
            def deploymentPods = kubeClient.getPods(namespace, podSelectorString)
            pods.addAll(deploymentPods)
        }

        pods
    }

    def chainOfTries(Closure ... closures) {
        def first = closures.head()
        closures = closures.tail()
        def result
        try {
            result = first.call()
        } catch (Throwable e) {
            if (closures.size()) {
                chainOfTries(closures)
            }
            else {
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

        def node = new ClusterNodeImpl(podName, TYPE_POD, podId)
        node.addAttribute('Status', status, TYPE_STRING)
        node.addAttribute('Labels', labels, TYPE_MAP)
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
                .code(ErrorCodes.UnknownError)
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
        node.addAttribute('Status', status, TYPE_STRING)
        if (startedAt) {
            node.addAttribute('Start Time', startedAt, TYPE_DATE)
        }
        if (environmentVariables && environmentVariables.size() ) {
            node.addAttribute('Environment Variables', environmentVariables, TYPE_MAP)
        }
        if (ports) {
            node.addAttribute('Ports', ports, 'map')
        }
        if (volumeMounts) {
            node.addAttribute("Volume Mounts", volumeMounts, TYPE_MAP)
        }
        def usage
        try {
            chainOfTries(
                {
                    usage = kubeClient.getPodMetricsHeapster(namespace, podId)
                    println usage
                },
                {
                    println "Cannot get metrics from heapster, switching to metrics-server"
                    usage = kubeClient.getPodMetricsServer(namespace, podId)
                    println usage
                }
            )
        } catch (Throwable e ) {
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

    String getServiceId(service) {
        def namespaceId = "${this.clusterName}::${service.metadata.namespace}"
        "${namespaceId}::${service.metadata.name}"
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
        def efId = service.metadata?.labels?.find{ it.key == 'ec-svc-id' }?.value
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

    def getNamespaceName(namespace) {
        def name = namespace?.metadata?.name
        assert name
        name
    }


}
