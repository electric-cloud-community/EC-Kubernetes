package com.electriccloud.kubernetes

import com.electriccloud.domain.ClusterTopology
import com.electriccloud.domain.ClusterTopologyImpl
import com.electriccloud.domain.Topology
import com.electriccloud.errors.EcException
import com.electriccloud.errors.ErrorCodes


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

    ClusterTopology getRealtimeClusterTopology() {
        def namespaces = kubeClient.getNamespaces()

        ClusterTopology topology = new ClusterTopologyImpl()
        topology.addNode(new ClusterTopologyImpl.NodeImpl(getEFClusterId(), TYPE_EF_CLUSTER, getEFClusterName()))
        topology.addLink(new ClusterTopologyImpl.LinkImpl(getEFClusterId(), getClusterId()))

        topology.addNode(buildClusterNode())

        namespaces.findAll { !isSystemNamespace(it) }.each { namespace ->
            if (!isSystemNamespace(namespace)) {
                topology.addNode(buildNamespaceNode(namespace))
                def services = kubeClient.getServices(getNamespaceName(namespace))

                topology.addLink(new ClusterTopologyImpl.LinkImpl(getClusterId(), getNamespaceId(namespace)))
                services.findAll { !isSystemService(it) }.each { service ->
                    def pods = getServicePods(service)
                    topology.addLink(new ClusterTopologyImpl.LinkImpl(getNamespaceId(namespace), getServiceId(service)))
                    topology.addNode(buildServiceNode(service, pods))

                    pods.each { pod ->
                        topology.addLink(new ClusterTopologyImpl.LinkImpl(getServiceId(service), getPodId(service, pod)))
                        topology.addNode(buildPodNode(service, pod))

                        def containers = pod.spec.containers
                        containers.each { container ->
                            topology.addLink(new ClusterTopologyImpl.LinkImpl(getPodId(service, pod), getContainerId(service, pod, container)))
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
        "${getNamespaceId(service)}::${service.metadata.name}"
    }

    String getPodId(service, pod) {
        "${getServiceId(service)}::${pod.metadata.name}"
    }

    String getContainerId(service, pod, container) {
        "${getPodId(service, pod)}::${container.name}"
    }

    def buildClusterNode() {
        Topology.Node node = new ClusterTopologyImpl.NodeImpl(clusterName, TYPE_CLUSTER, clusterName)
        node
    }

    def buildPodNode(service, pod) {
        def name = pod.metadata.name
        def status = pod.status.phase
        Topology.Node node = new ClusterTopologyImpl.NodeImpl(getPodId(service, pod), TYPE_POD, name)
        node.setStatus(status)
        node
    }

    def buildServiceNode(Map service, pods) {
        def name = service.metadata.name
        def status = getPodsStatus(pods)
        Topology.Node node = new ClusterTopologyImpl.NodeImpl(getServiceId(service), TYPE_SERVICE, name)
        node.setStatus(status)
        node
    }

    def buildContainerNode(service, pod, container) {
        def node = new ClusterTopologyImpl.NodeImpl(getContainerId(service, pod, container), TYPE_CONTAINER, container.name)
        node.setStatus(getContainerStatus(pod, container))
        return node
//
//        def imageAndVersion = container.image.split(':')
//        def image = imageAndVersion[0]
//        def version = imageAndVersion.size() > 1 ? imageAndVersion[1] : 'latest'
//        [
//            type   : 'container',
//            name   : container.name,
//            id     : getContainerId(service, pod, container),
//            status : getContainerStatus(pod, container),
//            image  : image,
//            version: version
//        ]
    }


    def buildNamespaceNode(namespace) {
        def name = getNamespaceName(namespace)
        new ClusterTopologyImpl.NodeImpl(getNamespaceId(namespace), TYPE_NAMESPACE, name)
    }

    def getNamespaceName(namespace) {
        def name = namespace?.metadata?.name
        assert name
        name
    }


}
