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
        efServices
    }

    def saveToEF(services, projectName) {
        def efServices = getServices(projectName)
        services.each { service ->
            prettyPrint(service)
            createOrUpdateService(projectName, efServices, service)
        }
    }

    def createOrUpdateService(projectName, efServices, service) {
        def existingService = efServices.find { s ->
            equalNames(s.serviceName, service.service.serviceName)
        }
        def result
        def serviceName

        if (existingService) {
            serviceName = existingService.serviceName
            result = updateEFService(existingService, service)
            logger INFO, "Service ${existingService.serviceName} has been updated"
        }
        else {
            serviceName = service.service.serviceName
            result = createEFService(projectName, service)
            logger INFO, "Service ${serviceName} has been created"
        }
        def serviceId = result.service.serviceId
        assert serviceId
        assert serviceName

        // Containers

        def efContainers = getContainers(projectName, serviceName)
        service.containers.each { container ->
            createOrUpdateContainer(projectName, serviceName, container, efContainers)
            mapContainerPorts(projectName, serviceName, container, service)
        }

        // TODO mapping
    }


    def mapContainerPorts(projectName, serviceName, container, service) {
        container.ports?.each { containerPort ->
            service.ports?.each { servicePort ->
                if (containerPort.portName == servicePort.portName) {
                    def generatedPortName = "servicehttp${serviceName}${container.container.containerName}${containerPort.containerPort}"
                    def generatedPort = [
                        portName: generatedPortName,
                        listenerPort: servicePort.listenerPort,
                        subcontainer: container.container.containerName,
                        subport: containerPort.portName
                    ]
                    createPort(projectName, serviceName, generatedPort)
                }
            }
        }
    }

    def createOrUpdateContainer(projectName, serviceName, container, efContainers) {
        def existingContainer = efContainers.find {
            equalNames(it.containerName, container.container.containerName)
        }
        def containerName
        def result
        if (existingContainer) {
            containerName = existingContainer.containerName
            logger INFO, "Going to update container ${serviceName}/${containerName}"
            logger INFO, pretty(container.container)
            result = updateContainer(projectName, existingContainer.serviceName, containerName, container.container)
            logger INFO, "Container ${serviceName}/${containerName} has been updated"
        }
        else {
            containerName = container.container.containerName
            logger INFO, "Going to create container ${serviceName}/${containerName}"
            logger INFO, pretty(container.container)
            result = createContainer(projectName, serviceName, container.container)
            logger INFO, "Container ${serviceName}/${containerName} has been created"
        }

        assert containerName
        def efPorts = getPorts(projectName, serviceName, /* appName */ null, containerName)
        prettyPrint(efPorts)
        container.ports.each { port ->
            createPort(projectName, serviceName, port, containerName)
            logger INFO, "Port ${port.portName} has been created"
        }

        if (container.env) {
            container.env.each { env ->
                createEnvironmentVariable(projectName, serviceName, containerName, env)
                logger INFO, "Environment variable ${env.environmentVariableName} has been created"
            }
        }
        // TODO delete extra ports??
    }

    def buildServiceDefinition(kubeService, deployment) {
        def serviceName = kubeService.metadata.name
        def deployName = kubeService.metadata.name

        def efServiceName
        if (serviceName =~ /(?i)${deployName}/) {
            efServiceName = serviceName
        }
        else {
            efServiceName = "${serviceName}-${deployName}"
        }
        def efService = [
            service: [
                serviceName: efServiceName
            ],
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

        // Service Fields
        def defaultCapacity = deployment.spec?.replicas ?: 1
        efService.service.defaultCapacity = defaultCapacity
        if (deployment.spec?.strategy?.rollingUpdate) {
            def rollingUpdate = deployment.spec.strategy.rollingUpdate
            efService.service.maxCapacity = getMaxCapacity(defaultCapacity, rollingUpdate.maxSurge)

            efService.service.minCapacity = getMinCapacity(defaultCapacity, rollingUpdate.maxUnavailable)

        }
        efService.serviceMapping.loadBalancerIP = deployment.spec?.clusterIP
        efService.serviceMapping.type = deployment.spec?.type
        efService.serviceMapping.sessionAffinity = deployment.spec?.sessionAffinity ? true : false

        // Ports
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
            [portName: name, listenerPort: port.port]
        }

        // Containers
        def containers = deployment.spec.template.spec.containers
        efService.containers = containers.collect { kubeContainer ->
            def container = buildContainerDefinition(kubeContainer)
            container
        }

        // Volumes
        if (deployment.spec.template.spec.volumes) {
            def volumes = deployment.spec.template.spec.volumes.collect { volume ->
                def retval = [name: volume.name]
                if (volume.hostPath?.path) {
                    retval.hostPath = volume.hostPath.path
                }
                retval
            }
            efService.service.volume = new JsonBuilder(volumes).toString()
        }
        efService
    }

    def updateEFService(efService, kubeService) {
        def payload = kubeService.service
        def projName = efService.projectName
        def serviceName = efService.serviceName
        payload.description = efService.description ?: "Updated by EF Discovery"
        def result = updateService(projName, serviceName, payload)
        result
    }

    def createEFService(projectName, service) {
        def payload = service.service
        payload.description = "Created by EF Discovery"
        def result = createService(projectName, payload)
        result
    }

    def equalNames(String oneName, String anotherName) {
        assert oneName
        assert anotherName
        def normalizer = { name ->
            name = name.toLowerCase()
            name = name.replaceAll('-', '.')
        }
        return normalizer(oneName) == normalizer(anotherName)
    }

    def getMaxCapacity(defaultCapacity, maxSurge) {
        assert defaultCapacity
        if (maxSurge > 1) {
            return defaultCapacity + maxSurge
        }
        else {
            return null
        }
    }

    def getMinCapacity(defaultCapacity, maxUnavailable) {
        assert defaultCapacity
        if (maxUnavailable > 1) {
            return defaultCapacity - maxUnavailable
        }
        else {
            return null
        }
    }


    private def parseImage(image) {
        // Image can consist of
        // repository url
        // repo name
        // image name
        def parts = image.split('/')
        // The name always exists
        def imageName = parts.last()
    }

    def getImageName(image) {
        image.split(':').first()
    }

    def getRepositoryURL(image) {
        image.split
    }

    def getImageVersion(image) {
        def parts = image.split(':')
        if (parts.size() > 1) {
            return parts.last()
        }
        else {
            return 'latest'
        }
    }

    def buildContainerDefinition(kubeContainer) {
        // TODO private registry
        def container = [
            container: [
                containerName: kubeContainer.name,
                imageName: getImageName(kubeContainer.image),
                imageVersion: getImageVersion(kubeContainer.image)
            ]
        ]

        container.env = kubeContainer.env?.collect {
            [environmentVariableName: it.name, value: it.value]
        }

        if (kubeContainer.command) {
            def entryPoint = kubeContainer.command.join(',')
            container.container.entryPoint = entryPoint
        }
        if (kubeContainer.args) {
            def args = kubeContainer.args.join(',')
            container.container.command = args
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
                [portName: name, containerPort: port.containerPort]
            }
        }
        def resources = kubeContainer.resources
        container.container.cpuCount = parseCPU(resources?.requests?.cpu)
        container.container.memorySize = parseMemory(resources?.requests?.memory)
        container.container.cpuLimit = parseCPU(resources?.limits?.cpu)
        container.container.memoryLimit = parseMemory(resources?.limits?.memory)
        // TODO private registry

        // Volume mounts
        def mounts = kubeContainer.volumeMounts?.collect { vm ->
            def retval = [name: vm.name]
            if (vm.mountPath) {
                retval.mountPath = vm.mountPath
            }
            retval
        }
        if (mounts) {
            container.container.volumeMount = new JsonBuilder(mounts).toString()
        }

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


    def pretty(o) {
        new JsonBuilder(o).toPrettyString()
    }

    def stop() {
        throw new RuntimeException('stop')
    }
}
