package com.electriccloud.kubernetes

class ClusterView implements Constants {
    String efClusterName
    Client kubeClient


    def getRealtimeClusterTopology() {
        def namespaces = kubeClient.getNamespaces()
        def nodes = []
        def links = []

        def clusterNode = buildClusterNode()
        nodes << clusterNode

        namespaces.each { namespace ->
            nodes << buildNamespaceNode(namespace)
            def services = kubeClient.getServices(getNamespaceName(namespace))
            def link = buildNamespaceLink(namespace)
            links << link
            services.each {
//                ...
            }
        }

        [nodes: nodes, links: links]
    }

    def buildNamespaceLink(namespace) {
        def name = getNamespaceName(namespace)
        [source: this.efClusterName, target: "${this.efClusterName}::${name}"]
    }

    def buildClusterNode() {
        [id: efClusterName, efId: '', displayName: efClusterName]
    }


    def buildNamespaceNode(namespace) {
        def name = getNamespaceName(namespace)
        [id: name, displayName: name, type: NAMESPACE]
    }

    def getNamespaceName(namespace) {
        namespace.metadata.name
    }


}
