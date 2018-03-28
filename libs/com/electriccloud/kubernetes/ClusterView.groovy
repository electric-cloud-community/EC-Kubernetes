package com.electriccloud.kubernetes

class ClusterView {
    String clusterName
    String clusterId

    Client kubeClient

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
                    links << [source: getNamespaceId(namespace), target: getServiceId(service)]
                    nodes << buildServiceNode(service)
                    def pods = getServicePods(service)
                    pods.each { pod ->
                        links << [source: getServiceId(service), target: getPodId(service, pod)]
                        nodes << buildPodNode(service, pod)
                        def containers = pod.containers
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
            def labels = deployment.spec.template.metadata.labels
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

    def buildServiceNode(Map service) {
        def name = service.metadata.name
        [id: getServiceId(service), name: name, type: 'service', status: 'TBD']
    }

    def buildContainerNode(service, pod, container) {
        def imageAndVersion = container.image.split(':')
        def image = imageAndVersion[0]
        def version = imageAndVersion.size() > 1 ? imageAndVersion[1] : 'latest'
        [
            type   : 'container',
            name   : container.name,
            id     : getContainerId(service, pod, container),
            status : 'TBD',
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
