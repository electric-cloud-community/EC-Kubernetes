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

    private static final String TYPE_STRING = 'string'
    private static final String TYPE_MAP = 'map'
    private static final String TYPE_LINK = 'link'
    private static final String TYPE_TEXTAREA = 'textarea'

    private static final String ATTRIBUTE_MASTER_VERSION = 'Master Version'
    private static final String ATTRIBUTE_STATUS = 'Status'
    private static final String ATTRIBUTE_LABELS = 'Labels'
    private static final String ATTRIBUTE_SERVICE_TYPE = 'ServiceType'
    private static final String ATTRIBUTE_ENDPOINT = 'Endpoint'
    private static final String ATTRIBUTE_RUNNING_PODS = 'Running Pods'
    private static final String ATTRIBUTE_VOLUMES = 'Volumes'




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

    //TODO
    def getServiceLabels(){
        null
    }

    //TODO
    def getNamespaceLabels(){
        null
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
        "${getNamespaceId(service)}::${service.metadata.name}"
    }

    String getServiceEndpoint(service) {
        "${service.spec.loadBalancerIP}:${service.spec.ports[0].port}"
    }

    String getServiceVolumes(service) {
        "${service.spec.loadBalancerIP}:${service.spec.ports[0].port}"
    }

    String getPodId(service, pod) {
        "${getServiceId(service)}::${pod.metadata.name}"
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
        new ClusterNodeImpl(getNamespaceId(namespace), TYPE_NAMESPACE, name)
    }

    def buildClusterDetails(){
        def attributes = []
        attributes.push([name:      ATTRIBUTE_MASTER_VERSION,
                         value:     "${kubeClient.getClusterVersion()}",
                         type:      TYPE_STRING])

        attributes.push([name:      ATTRIBUTE_LABELS,
                         value:     "${getServiceLabels()}",
                         type:      TYPE_MAP])

        def clusterDetails = [id:           "${getClusterId()}",
                              type:         TYPE_CLUSTER,
                              name:         "${getClusterName()}",
                              attributes:   attributes]
        clusterDetails
    }

    def buildNamespaceDetails(namespace){
        def attributes = []
        attributes.push([name:      ATTRIBUTE_LABELS,
                         value:     "${getNamespaceLabels()}",
                         type:      TYPE_MAP])

        def namespaceDetails = [id:             "${getNamespaceId(namespace)}",
                                type:           TYPE_NAMESPACE,
                                name:           "${getNamespaceName(namespace)}",
                                attributes:     attributes]
        namespaceDetails
    }

    def buildServiceDetails(service, pods){
        def attributes = []

        attributes.push([name:      ATTRIBUTE_STATUS,
                         value:     "${getPodsStatus(pods)}",
                         type:      TYPE_STRING])

        attributes.push([name:      ATTRIBUTE_LABELS,
                         value:     "${service.metadata.labels}",
                         type:      TYPE_MAP])

        attributes.push([name:      ATTRIBUTE_SERVICE_TYPE,
                         value:     "${service.spec.type}",
                         type:      TYPE_STRING])

        attributes.push([name:      ATTRIBUTE_ENDPOINT,
                         value:     "${getServiceEndpoint(service)}",
                         type:      TYPE_LINK])

        attributes.push([name:      ATTRIBUTE_RUNNING_PODS,
                         value:     "${getPodsRunning(pods)}",
                         type:      TYPE_STRING])

        def volumes = getServiceVolumes(service.metadata.namespace, getServiceName(service))
        attributes.push([name:      ATTRIBUTE_VOLUMES,
                         value:     volumes,
                         type:      TYPE_TEXTAREA])

        def serviceDetails = [id:           "${getServiceId(service)}",
                              type:         TYPE_SERVICE,
                              efid:         null,
                              name:         "${getServiceName(service)}",
                              attributes:   attributes]

        serviceDetails
    }

    def getNamespaceName(namespace) {
        def name = namespace?.metadata?.name
        assert name
        name
    }


}
