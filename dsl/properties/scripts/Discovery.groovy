
public class Discovery extends EFClient {
    def kubeClient
    def pluginConfig
    def accessToken
    def clusterEndpoint
    def discoveredSummary = [:]

    static final String CREATED_DESCRIPTION = "Created by Container Discovery"

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

                deployments.items.each { deploy ->
                    def efService = buildServiceDefinition(kubeService, deploy, namespace)


                    if (deploy.spec.template.spec.imagePullSecrets) {
                        def secrets = buildSecretsDefinition(namespace, deploy.spec.template.spec.imagePullSecrets)
                        efService.secrets = secrets
                    }
                    efServices.push(efService)
                }
            }
        }
        efServices
    }

    def saveToEF(services, projectName, envProjectName, envName, clusterName) {
        def efServices = getServices(projectName)
        services.each { service ->
            createOrUpdateService(projectName, envProjectName, envName, clusterName, efServices, service)
        }

        def lines = ["Discovered services: ${discoveredSummary.size()}"]
        discoveredSummary.each { serviceName, containers ->
            def containerNames = containers.collect { k -> k }
            lines.add("${serviceName}: ${containerNames.join(', ')}")
        }
        updateJobSummary(lines.join("\n"))
    }

    def createOrUpdateService(projectName, envProjectName, envName, clusterName, efServices, service) {
        def existingService = efServices.find { s ->
            equalNames(s.serviceName, service.service.serviceName)
        }
        def result
        def serviceName

        logger DEBUG, "Service payload:"
        logger DEBUG, new JsonBuilder(service).toPrettyString()

        if (existingService) {
            serviceName = existingService.serviceName
            logger WARNING, "Service ${existingService.serviceName} already exists, skipping"
            // Future
            // result = updateEFService(existingService, service)
            // logger INFO, "Service ${existingService.serviceName} has been updated"
        }
        else {
            serviceName = service.service.serviceName
            result = createEFService(projectName, service)
            logger INFO, "Service ${serviceName} has been created"
            discoveredSummary[serviceName] = [:]
        }
        assert serviceName

        // Containers
        def efContainers = getContainers(projectName, serviceName)

        service.secrets?.each { cred ->
            def credName = getCredName(cred)
            createCredential(projectName, credName, cred.userName, cred.password)
            logger INFO, "Credential $credName has been created"
        }

        service.containers.each { container ->
            service.secrets?.each { secret ->
                if (secret.repoUrl =~ /${container.container.registryUri}/) {
                    container.container.credentialName = getCredName(secret)
                }
            }
            createOrUpdateContainer(projectName, serviceName, container, efContainers)
            mapContainerPorts(projectName, serviceName, container, service)
        }

        if (service.serviceMapping) {
            createOrUpdateMapping(projectName, envProjectName, envName, clusterName, serviceName, service)
        }

        // Add deploy process
        createDeployProcess(projectName, serviceName)
    }

    def createDeployProcess(projectName, serviceName) {
        def processName = 'Deploy'
        def process = createProcess(projectName, serviceName, [processName: processName, processType: 'DEPLOY'])
        logger INFO, "Process ${processName} has been created for ${serviceName}"
        def processStepName = 'deployService'
        def processStep = createProcessStep(projectName, serviceName, processName, [
            processStepName: processStepName,
            processStepType: 'service', subservice: serviceName
        ])
        logger INFO, "Process step ${processStepName} has been created for process ${processName} in service ${serviceName}"
    }


    def createOrUpdateMapping(projName, envProjName, envName, clusterName, serviceName, service) {
        def mapping = service.serviceMapping

        def envMaps = getEnvMaps(projName, serviceName)
        def existingMap = getExistingMapping(projName, serviceName, envProjName, envName)

        def envMapName
        if (existingMap) {
            logger INFO, "Environment map already exists for service ${serviceName} and cluster ${clusterName}"
            envMapName = existingMap.environmentMapName
        }
        else {
            def payload = [
                environmentProjectName: envProjName,
                environmentName: envName,
                description: CREATED_DESCRIPTION,
            ]

            def result = createEnvMap(projName, serviceName, payload)
            envMapName = result.environmentMap?.environmentMapName
        }

        assert envMapName

        def existingClusterMapping = existingMap?.serviceClusterMappings?.serviceClusterMapping?.find {
            it.clusterName == clusterName
        }

        def serviceClusterMappingName
        if (existingClusterMapping) {
            logger INFO, "Cluster mapping already exists"
            serviceClusterMappingName = existingClusterMapping.serviceClusterMappingName
        }
        else {
            def payload = [
                clusterName: clusterName,
                environmentName: envName,
                environmentProjectName: envProjName
            ]

            if (mapping) {
                def actualParameters = []
                mapping.each {k, v ->
                    if (v) {
                        actualParameters.add([actualParameterName: k, value: v])
                    }
                }
                payload.actualParameter = actualParameters
            }
            def result = createServiceClusterMapping(projName, serviceName, envMapName, payload)
            logger INFO, "Created Service Cluster Mapping for ${serviceName} and ${clusterName}"
            serviceClusterMappingName = result.serviceClusterMapping.serviceClusterMappingName
        }

        assert serviceClusterMappingName

        service.containers?.each { container ->
            def payload = [
                containerName: container.container.containerName
            ]
            prettyPrint(container)
            if (container.mapping) {
                def actualParameters = []
                container.mapping.each {k, v ->
                    if (v) {
                        actualParameters.add(
                            [actualParameterName: k, value: v]
                        )
                    }
                }
                payload.actualParameter = actualParameters
            }
            prettyPrint(payload)
            createServiceMapDetails(
                projName,
                serviceName,
                envMapName,
                serviceClusterMappingName,
                payload
            )
        }
    }

    def getExistingMapping(projectName, serviceName, envProjectName, envName) {
        def envMaps = getEnvMaps(projectName, serviceName)
        def existingMap = envMaps.environmentMap?.find {
            it.environmentProjectName == envProjectName && it.projectName == projectName && it.serviceName == serviceName && it.environmentName == envName
        }
        existingMap
    }

    def mapContainerPorts(projectName, serviceName, container, service) {
        container.ports?.each { containerPort ->
            service.ports?.each { servicePort ->
                prettyPrint(servicePort)
                prettyPrint(containerPort)
                if (containerPort.portName == servicePort.portName || servicePort.targetPort == containerPort.name) {
                    def generatedPortName = "servicehttp${serviceName}${container.container.containerName}${containerPort.containerPort}"
                    def generatedPort = [
                        portName: generatedPortName,
                        listenerPort: servicePort.listenerPort,
                        subcontainer: container.container.containerName,
                        subport: containerPort.portName
                    ]
                    createPort(projectName, serviceName, generatedPort)
                    logger INFO, "Port ${generatedPortName} has been created for service ${serviceName}, listener port: ${generatedPort.listenerPort}, container port: ${generatedPort.subport}"
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
        logger DEBUG, "Container payload:"
        logger DEBUG, new JsonBuilder(container).toPrettyString()
        if (existingContainer) {
            containerName = existingContainer.containerName
            logger WARNING, "Container ${containerName} already exists, skipping"
            // // Future
            // logger INFO, "Going to update container ${serviceName}/${containerName}"
            // logger INFO, pretty(container.container)
            // result = updateContainer(projectName, existingContainer.serviceName, containerName, container.container)
            // logger INFO, "Container ${serviceName}/${containerName} has been updated"
        }
        else {
            containerName = container.container.containerName
            logger INFO, "Going to create container ${serviceName}/${containerName}"
            logger INFO, pretty(container.container)
            result = createContainer(projectName, serviceName, container.container)
            logger INFO, "Container ${serviceName}/${containerName} has been created"
            discoveredSummary[serviceName][containerName] = [:]
        }

        assert containerName
        def efPorts = getPorts(projectName, serviceName, /* appName */ null, containerName)
        container.ports.each { port ->
            createPort(projectName, serviceName, port, containerName)
            logger INFO, "Port ${port.portName} has been created for container ${containerName}, container port: ${port.containerPort}"
        }

        if (container.env) {
            container.env.each { env ->
                createEnvironmentVariable(projectName, serviceName, containerName, env)
                logger INFO, "Environment variable ${env.environmentVariableName} has been created"
            }
        }

        if (container.mapping) {

        }
    }

    def buildSecretsDefinition(namespace, secrets) {
        def retval = []
        secrets.each {
            def name = it.name
            def secret = kubeClient.getSecret(name, clusterEndpoint, namespace, accessToken)

            def dockercfg = secret.data['.dockercfg']
            if (dockercfg) {
                def decoded = new JsonSlurper().parseText(new String(dockercfg.decodeBase64(), "UTF-8"))

                if (decoded.keySet().size() == 1) {
                    def repoUrl = decoded.keySet().first()
                    def username = decoded[repoUrl].username
                    def password = decoded[repoUrl].password

                    // Password may be absent
                    // In this case we can do nothing

                    if (password) {
                        def cred = [
                            repoUrl: repoUrl,
                            userName: username,
                            password: password
                        ]
                        retval.add(cred)
                    }
                    else {
                        logger WARNING, "Cannot retrieve password from secret for $repoUrl, please create a credential manually"
                    }
                }
            }
        }
        retval
    }

    def buildServiceDefinition(kubeService, deployment, namespace) {
        def serviceName = kubeService.metadata.name
        def deployName = deployment.metadata.name

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
        efService.serviceMapping.loadBalancerIP = kubeService.spec?.clusterIP
        efService.serviceMapping.serviceType = kubeService.spec?.type
        efService.serviceMapping.sessionAffinity = kubeService.spec?.sessionAffinity
        def sourceRanges = kubeService.spec?.loadBalancerSourceRanges?.join(',')
        efService.serviceMapping.loadBalancerSourceRanges = sourceRanges
        if (namespace != 'default') {
            efService.serviceMapping.namespace = namespace
        }
        // Ports
        efService.ports = kubeService.spec?.ports?.collect { port ->
            def name
            if (port.targetPort) {
                name = port.targetPort as String
            }
            else {
                name = "${port.protocol}${port.port}"
            }
            [portName: name.toLowerCase(), listenerPort: port.port]
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
        def registry
        def repoName
        if (parts.size() >= 2) {
            repoName = parts[parts.size() - 2]
            // It may be an image without repo, like nginx
            if (repoName =~ /\./) {
                registry = repoName
            }
        }
        if (!registry && parts.size() > 2) {
            registry = parts.take(parts.size() - 2).join('/')
        }
        if (repoName) {
            imageName = repoName + '/' + imageName
        }
        def versioned = imageName.split(':')
        def version
        if (versioned.size() > 1) {
            version = versioned.last()
        }
        else {
            version = 'latest'
        }
        imageName = versioned.first()
        return [imageName: imageName, version: version, repoName: repoName, registry: registry]
    }

    def getImageName(image) {
        parseImage(image).imageName
    }

    def getRepositoryURL(image) {
        parseImage(image).repoName
    }

    def getImageVersion(image) {
        parseImage(image).version
    }

    def getRegistryUri(image) {
        parseImage(image).registry
    }

    def buildContainerDefinition(kubeContainer) {

        prettyPrint(kubeContainer)

        def container = [
            container: [
                containerName: kubeContainer.name,
                imageName: getImageName(kubeContainer.image),
                imageVersion: getImageVersion(kubeContainer.image),
                registryUri: getRegistryUri(kubeContainer.image) ?: null
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
        // Ports
        if (kubeContainer.ports) {
            container.ports = kubeContainer.ports.collect { port ->
                def name

                if (port.name) {
                    name = port.name
                }
                else {
                    name = "${port.protocol}${port.containerPort}"
                }
                [portName: name.toLowerCase(), containerPort: port.containerPort]
            }
        }

        container.mapping = [:]

        // Liveness probe
        if (kubeContainer.livenessProbe) {
            def processedLivenessFields = []
            def probe = kubeContainer.livenessProbe.httpGet
            processedLivenessFields << 'httpGet'
            container.mapping.with {
                livenessHttpProbePath = probe?.path
                livenessHttpProbePort = probe?.port
                livenessInitialDelay = kubeContainer.livenessProbe?.initialDelaySeconds
                livenessPeriod = kubeContainer.livenessProbe?.periodSeconds
                processedLivenessFields << 'initialDelaySeconds'
                processedLivenessFields << 'periodSeconds'
                if (probe.httpHeaders?.size() > 1) {
                    logger WARNING, 'Only one liveness header is supported, will take the first'
                }
                def header = probe?.httpHeaders?.first()
                livenessHttpProbeHttpHeaderName = header?.name
                livenessHttpProbeHttpHeaderValue = header?.value
            }
            kubeContainer.livenessProbe?.each { k, v ->
                if (!(k in processedLivenessFields) && v) {
                    logger WARNING, "Field ${k} from livenessProbe is not supported"
                }
            }
        }
        // Readiness probe
        if (kubeContainer.readinessProbe) {
            def processedFields = ['command']
            container.mapping.with {
                def command = kubeContainer.readinessProbe.exec?.command
                readinessCommand = command?.first()
                if (command?.size() > 1) {
                    logger WARNING, 'Only one readiness command is supported'
                }
                processedFields << 'initialDelaySeconds'
                processedFields << 'periodSeconds'
                readinessInitialDelay = kubeContainer.readinessProbe?.initialDelaySeconds
                readinessPeriod = kubeContainer.readinessProbe?.periodSeconds
            }

            kubeContainer.readinessProbe?.each { k, v ->
                if (!(k in processedFields) && v) {
                    logger WARNING, "Field ${k} is from readinessProbe not supported"
                }
            }
        }
        def resources = kubeContainer.resources
        container.container.cpuCount = parseCPU(resources?.requests?.cpu)
        container.container.memorySize = parseMemory(resources?.requests?.memory)
        container.container.cpuLimit = parseCPU(resources?.limits?.cpu)
        container.container.memoryLimit = parseMemory(resources?.limits?.memory)

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

    def getCredName(cred) {
        "${cred.repoUrl} - ${cred.userName}"
    }

    def prettyPrint(object) {
        println new JsonBuilder(object).toPrettyString()
    }


    def pretty(o) {
        new JsonBuilder(o).toPrettyString()
    }

}
