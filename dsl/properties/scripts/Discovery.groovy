public class Discovery extends EFClient {
    def kubeClient
    def pluginConfig
    def accessToken
    def clusterEndpoint

    def Discovery(params) {
        kubeClient = params.kubeClient
        pluginConfig = params.pluginConfig
        accessToken = kubeClient.retrieveAccessToken(pluginConfig)
        clusterEndpoint = pluginConfig.clusterEndpoint
    }

    def discover(namespace) {
        def kubeServices = kubeClient.getServices(clusterEndpoint, namespace, accessToken)
        def efServices = []
        kubeServices.items.each { kubeService ->
            if (!isSystemService(kubeService)) {
                def selector = kubeService.spec.selector.collect { k, v ->
                    k + '=' + v
                }.join(',')

                def deployments = kubeClient.getDeployments(
                    clusterEndpoint,
                    namespace, accessToken,
                    [labelSelector: selector]
                )

                // def pods = kubeClient.getPods(clusterEndpoint, namespace, accessToken, [labelSelector: selector])

                deployments.items.each { deploy ->
                    def efService = buildServiceDefinition(kubeService, deploy)
                    efServices.push(efService)
                }
            }
        }
    }

    // Either discovered data or collected from YAML
    def modelToEF(model) {

    }

    def buildServiceDefinition(kubeService, deployment) {
        def serviceName = kubeService.metadata.name
        def deployName = kubeService.metadata.name

        prettyPrint(kubeService)
        prettyPrint(deployment)

        def efServiceName
        if (serviceName =~ /(?i)${deployName}/) {
            efServiceName = serviceName
        }
        else {
            efServiceName = "${serviceName}-${deployName}"
        }
        def efService = [
            serviceName: efServiceName,
            serviceMapping: [:]
        ]

//         kind: Service
// apiVersion: v1
// metadata:
//   name: <service_name>
// spec:
//   selector:
//     <name-value-pairs to identify deployment pods>
//   ports:
//   - protocol: TCP
//     port: <port>
//     targetPort: <target_port>
//   type:<LoadBalance|ClusterIP|NodePort>
//   loadBalancerIP:<LB_IP>
//   loadBalancerSourceRanges:<ranges>
//   sessionAffinity:<value>

        efService.defaultCapacity = deployment.spec?.replicas ?: 1
        if (deployment.spec?.strategy?.rollingUpdate) {
            def rollingUpdate = deployment.spec.strategy.rollingUpdate
            efService.maxCapacity = getMaxCapacity(efService.defaultCapacity, rollingUpdate.maxSurge)

            efService.minCapacity = getMinCapacity(efService.defaultCapacity, rollingUpdate.maxUnavailable)

        }
        efService.serviceMapping.loadBalancerIP = deployment.spec?.clusterIP
        efService.serviceMapping.type = deployment.spec?.type
        efService.serviceMapping.sessionAffinity = deployment.spec?.sessionAffinity

        efService.ports = kubeService.spec?.ports?.collect { port ->
            def name
            if (port.name) {
                name = port.name
            }
            else if (port.targetPort) {
                name = "${port.protocol}${port.targetPort}"
            }
            else {
                name = "${port.protocol}${port.port}"
            }
            [name: name, port: port.port]
        }

        def containers = deployment.spec.template.spec.containers
        efService.containers = containers.collect { kubeContainer ->

            def container = buildContainerDefinition(kubeContainer)
            container
        }

        prettyPrint(efService)
        efService
    }

    def getMaxCapacity(defaultCapacity, maxSurge) {
        if (maxSurge > 1) {
            return defaultCapacity + maxSurge
        }
        else {
            return null
        }
    }

    def getMinCapacity(defaultCapacity, maxUnavailable) {
        if (maxUnavailable > 1) {
            return defaultCapacity - maxUnavailable
        }
        else {
            return null
        }
    }

    def buildContainerDefinition(kubeContainer) {
        def container = [
            name: kubeContainer.name,
            image: kubeContainer.image
        ]
        if (kubeContainer.env) {
            container.env = kubeContainer.env
        }
        if (kubeContainer.ports) {
            container.ports = kubeContainer.ports.collect { port ->
                def name
                if (port.name) {
                    name = port.name
                }
                else {
                    name = "${port.protocol}${port.containerPort}"
                }
                [name: name, port: port.containerPort]
            }
        }
        def resources = kubeContainer.resources
        container.cpuCount = parseCPU(resources?.requests?.cpu)
        container.memory = parseMemory(resources?.requests?.memory)
        container.cpuLimit = parseCPU(resources?.limits?.cpu)
        container.memoryLimit = parseMemory(resources?.limits?.memory)
        // TODO volumes
        // TODO volume mounts
        // TODO command
        // TODO args
        container
    }

    def isSystemService(service) {
        def name = service.metadata.name
        name == 'kubernetes'
    }

    def parseCPU(cpuString) {
        if (!cpuString) {
            return
        }
        if (cpuString =~ /m/) {
            def miliCpu = cpuString.replace('m', '') as int
            def cpu = miliCpu.toFloat() / 1000
            return cpu
        }
        else {
            return cpuString.toFloat()
        }
    }

    def parseMemory(memoryString) {
        if (!memoryString) {
            return
        }
        // E, P, T, G, M, K
        def memoryNumber = memoryString.replaceAll(/\D+/, '')
        def suffix = memoryString.replaceAll(/\d+/, '')
        def power
        ['k', 'm', 'g', 't', 'p', 'e'].eachWithIndex { elem, index ->
            if (suffix =~ /(?i)${elem}/) {
                power = index - 1
                // We store memory in MB, therefore KB will be power -1, mb will be the power of 1 and so on
            }
        }
        if (power) {
            def retval = memoryNumber.toInteger() * (1024 ** power)
            return retval
        }
        else {
            return memoryNumber.toInteger()
        }
    }

    def prettyPrint(object) {
        println new JsonBuilder(object).toPrettyString()
    }
}
