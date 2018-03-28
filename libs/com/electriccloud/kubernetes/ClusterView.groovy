package com.electriccloud.kubernetes

class ClusterView {
    String clusterName
    String clusterId

    Client kubeClient

    private static final String RUNNING = 'Running'
    private static final String FAILED = 'Failed'
    private static final String SUCCEEDED = 'Succeeded'
    private static final String PENDING = 'Pending'
    private static final String CRUSH_LOOP = 'CrushLoopBackoff'

    def getRealtimeClusterTopology() {
        def namespaces = kubeClient.getNamespaces()
        def nodes = []
        def links = []

        def clusterNode = buildClusterNode()
        nodes << clusterNode

        namespaces.findAll { !isSystemNamespace(it) }.each { namespace ->
            if (!isSystemNamespace(namespace)) {
                nodes << buildNamespaceNode(namespace)
                def services = kubeClient.getServices(getNamespaceName(namespace))
                def link = [source: getClusterId(), target: getNamespaceId(namespace)]
                links << link
                services.findAll { !isSystemService(it) }.each { service ->
                    def pods = getServicePods(service)
                    links << [source: getNamespaceId(namespace), target: getServiceId(service)]
                    nodes << buildServiceNode(service, pods)
                    pods.each { pod ->
                        links << [source: getServiceId(service), target: getPodId(service, pod)]
                        nodes << buildPodNode(service, pod)
                        def containers = pod.spec.containers
                        containers.each { container ->
                            links << [source: getPodId(service, pod), target: getContainerId(service, pod, container)]
                            nodes << buildContainerNode(service, pod, container)
                        }
                    }
                }
            }
        }

        [nodes: nodes, links: links]
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
            throw new RuntimeException("No container status found for name ${name}")
        }
        def states = containerStatus?.state.keySet()
        if (states.size() == 1) {
            return states[0]
        }
        throw new RuntimeException("Container has more than one status: ${containerStatus}")
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


    def getNamespaceId(namespace) {
        "${this.clusterName}::${getNamespaceName(namespace)}"
    }

    def getClusterId() {
        this.clusterName
    }

    def getServiceId(service) {
        "${getNamespaceId(service)}::${service.metadata.name}"
    }

    def getPodId(service, pod) {
        "${getServiceId(service)}::${pod.metadata.name}"
    }

    def getContainerId(service, pod, container) {
        "${getPodId(service, pod)}::${container.name}"
    }

    def buildClusterNode() {
        [id: clusterName, efId: clusterId, displayName: clusterName]
    }

    def buildPodNode(service, pod) {
        def name = pod.metadata.name
        def status = pod.status.phase
        [type: 'pod', id: getPodId(service, pod), name: name, status: status]
    }

    def buildServiceNode(Map service, pods) {
        def name = service.metadata.name
        def status = getPodsStatus(pods)
        [id: getServiceId(service), name: name, type: 'service', status: status]
    }

    def buildContainerNode(service, pod, container) {
        def imageAndVersion = container.image.split(':')
        def image = imageAndVersion[0]
        def version = imageAndVersion.size() > 1 ? imageAndVersion[1] : 'latest'
        [
            type   : 'container',
            name   : container.name,
            id     : getContainerId(service, pod, container),
            status : getContainerStatus(pod, container),
            image  : image,
            version: version
        ]
    }


    def buildNamespaceNode(namespace) {
        def name = getNamespaceName(namespace)
        [id: getNamespaceId(namespace), displayName: name, type: 'namespace']
    }

    def getNamespaceName(namespace) {
        namespace.metadata.name
    }


}
