/**
 * Kubernetes API client
 */
public class KubernetesClient extends BaseClient {

    String retrieveAccessToken(def pluginConfig) {
        "Bearer ${pluginConfig.credential.password}"
    }

    /**
    *  A way to check cluster is up/reachable. Currently simply hits API base endpoint
    *  In future there might be a "health" endpoint available which can be used
    */
    def checkClusterHealth(String clusterEndPoint, String accessToken){

        if (OFFLINE) return null

        doHttpGet(clusterEndPoint,
                "/apis",
                accessToken, /*failOnErrorCode*/ false)
    }

    def deployService(
            EFClient efClient,
            String accessToken,
            String clusterEndpoint,
            String namespace,
            String serviceName,
            String serviceProjectName,
            String applicationName,
            String applicationRevisionId,
            String clusterName,
            String clusterOrEnvProjectName,
            String environmentName,
            String resultsPropertySheet) {

        def serviceDetails = efClient.getServiceDeploymentDetails(
                serviceName,
                serviceProjectName,
                applicationName,
                applicationRevisionId,
                clusterName,
                clusterOrEnvProjectName,
                environmentName)

        createOrCheckNamespace(clusterEndpoint, namespace, accessToken)

        createOrUpdateService(clusterEndpoint, namespace, serviceDetails, accessToken)

        createOrUpdateDeployment(clusterEndpoint, namespace, serviceDetails, accessToken)

        def serviceEndpoint = getDeployedServiceEndpoint(clusterEndpoint, namespace, serviceDetails, accessToken)

        if (serviceEndpoint) {
            serviceDetails.port?.each { port ->
                String portName = port.portName
                String url = "${serviceEndpoint}:${port.listenerPort}"
                efClient.createProperty("${resultsPropertySheet}/${serviceName}/${portName}/url", url)
            }
        }
    }

    def getPluginConfig(EFClient efClient, String clusterName, String clusterOrEnvProjectName, String environmentName) {

        def clusterParameters = efClient.getProvisionClusterParameters(
                clusterName,
                clusterOrEnvProjectName,
                environmentName)

        def configName = clusterParameters.config
        def pluginProjectName = '$[/myProject/projectName]'
        efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)
    }

    def createOrCheckNamespace(String clusterEndPoint, String namespace, String accessToken){

        if (OFFLINE) return null

        def response = doHttpGet(clusterEndPoint,
                "/api/v1/namespaces/${namespace}",
                accessToken, /*failOnErrorCode*/ false)
        if (response.status == 200){
            logger INFO, "Namespace ${namespace} already exists"
            return
        }
        else if (response.status == 404){
            def namespaceDefinition = buildNamespacePayload(namespace)
            logger INFO, "Creating Namespace ${namespace}"
            doHttpRequest(POST,
                    clusterEndPoint,
                    '/api/v1/namespaces',
                    ['Authorization' : accessToken],
                    /*failOnErrorCode*/ true,
                    namespaceDefinition)
        }
        else {
            logger ERROR, "FATAL ERROR while checking or creating namespace"
        }
    }

    String buildNamespacePayload(String namespace){
        def json = new JsonBuilder()
        def result = json{
            kind "Namespace"
            apiVersion "v1"
            metadata {
                name namespace
            }
        }
        return (new JsonBuilder(result)).toPrettyString()
    }

    /**
     * Retrieves the Deployment instance from Kubernetes cluster.
     * Returns null if no Deployment instance by the given name is found.
     */
    def getDeployment(String clusterEndPoint, String namespace, String deploymentName, String accessToken) {

        if (OFFLINE) return null

        def response = doHttpGet(clusterEndPoint,
                "/apis/extensions/v1beta1/namespaces/${namespace}/deployments/${formatName(deploymentName)}",
                accessToken, /*failOnErrorCode*/ false)
        response.status == 200 ? response.data : null
    }

    /**
     * Retrieves the Service instance from Kubernetes cluster.
     * Returns null if no Service instance by the given name is found.
     */
    def getService(String clusterEndPoint, String namespace, String serviceName, String accessToken) {

        if (OFFLINE) return null

        def response = doHttpGet(clusterEndPoint,
                "/api/v1/namespaces/${namespace}/services/$serviceName",
                accessToken, /*failOnErrorCode*/ false)
        response.status == 200 ? response.data : null
    }

    def createOrUpdateService(String clusterEndPoint, String namespace , def serviceDetails, String accessToken) {

        String serviceName = formatName(serviceDetails.serviceName)
        def deployedService = getService(clusterEndPoint, namespace, serviceName, accessToken)

        def serviceDefinition = buildServicePayload(serviceDetails, deployedService)

        if (OFFLINE) return null

        if(deployedService){
            logger INFO, "Updating deployed service $serviceName"
            doHttpRequest(PUT,
                    clusterEndPoint,
                    "/api/v1/namespaces/$namespace/services/$serviceName",
                    ['Authorization' : accessToken],
                    /*failOnErrorCode*/ true,
                    serviceDefinition)

        } else {
            logger INFO, "Creating service $serviceName"
            doHttpRequest(POST,
                    clusterEndPoint,
                    "/api/v1/namespaces/${namespace}/services",
                    ['Authorization' : accessToken],
                    /*failOnErrorCode*/ true,
                    serviceDefinition)
        }
    }

    def getDeployedServiceEndpoint(String clusterEndPoint, String namespace, def serviceDetails, String accessToken) {

        def lbEndpoint
        def elapsedTime = 0;
        def timeInSeconds = 5*60
        String serviceName = formatName(serviceDetails.serviceName)
        while (elapsedTime <= timeInSeconds) {
            def before = System.currentTimeMillis()
            Thread.sleep(10*1000)

            def deployedService = getService(clusterEndPoint, namespace, serviceName, accessToken)
            def lbIngress = deployedService?.status?.loadBalancer?.ingress.find {
                it.ip != null || it.hostname != null
            }

            if (lbIngress) {
                lbEndpoint = lbIngress.ip?:lbIngress.hostname
                break
            }
            logger INFO, "Waiting for service status to publish loadbalancer ingress... \nElapsedTime: $elapsedTime seconds"

            def now = System.currentTimeMillis()
            elapsedTime = elapsedTime + (now - before)/1000
        }

        if (!lbEndpoint) {
            logger INFO, "Loadbalancer ingress not published yet. Defaulting to specified loadbalancer IP."
            def value = getServiceParameter(serviceDetails, 'loadBalancerIP')
            lbEndpoint = value
        }
        lbEndpoint
    }

    def createOrUpdateSecret(def secretName, def username, def password, def repoBaseUrl,
                         String clusterEndPoint, String namespace, String accessToken){
        def existingSecret = getSecret(secretName, clusterEndPoint, namespace, accessToken)
        def secret = buildSecretPayload(secretName, username, password, repoBaseUrl)
        if (OFFLINE) return null
        if (existingSecret) {
                    logger INFO, "Updating existing Secret $secretName"
                    doHttpRequest(PUT,
                            clusterEndPoint,
                            "/api/v1/namespaces/${namespace}/secrets/${secretName}",
                            ['Authorization' : accessToken],
                            /*failOnErrorCode*/ true,
                            secret)

                } else {
                    logger INFO, "Creating deployment $secretName"
                    doHttpRequest(POST,
                            clusterEndPoint,
                            "/api/v1/namespaces/${namespace}/secrets",
                            ['Authorization' : accessToken],
                            /*failOnErrorCode*/ true,
                            secret)
                }
    }

    def getSecret(def secretName, def clusterEndPoint, String namespace, def accessToken) {

        if (OFFLINE) return null

        def response = doHttpGet(clusterEndPoint,
                "/api/v1/namespaces/${namespace}/secrets/${secretName}",
                accessToken, /*failOnErrorCode*/ false)
        response.status == 200 ? response.data : null
    }

    def buildSecretPayload(def secretName, def username, def password, def repoBaseUrl){
        def encodedCreds = (username+":"+password).bytes.encodeBase64().toString()
        def dockerCfgData = ["${repoBaseUrl}": [ username: username,
                                                password: password,
                                                email: "none",
                                                auth: encodedCreds]
                            ]
        def dockerCfgJson = new JsonBuilder(dockerCfgData)
        def dockerCfgEnoded = dockerCfgJson.toString().bytes.encodeBase64().toString()
        def secret = [ apiVersion: "v1",
                       kind: "Secret",
                       metadata: [name: secretName],
                       data: [".dockercfg": dockerCfgEnoded],
                       type: "kubernetes.io/dockercfg"]

        def secretJson = new JsonBuilder(secret)
        return secretJson.toPrettyString()
    }

    def constructSecretName(String imageUrl, String username){
        def imageDetails = imageUrl.tokenize('/')
        if (imageDetails.size() < 2) {
            handleError("Please check that the registry url was specified for the image.")
        }
        String repoBaseUrl = imageDetails[0]
        def secretName = repoBaseUrl + "-" + username
        return [repoBaseUrl, secretName.replaceAll(':', '-').replaceAll('/', '-')]
    }

    def createOrUpdateDeployment(String clusterEndPoint, String namespace, def serviceDetails, String accessToken) {

        // Use the same name as the service name to create a Deployment in Kubernetes
        // that will drive the deployment of the service pods.
        def imagePullSecrets = []
        serviceDetails.container.collect { svcContainer ->
            //Prepend the registry to the imageName
            //if it does not already include it.
            if (svcContainer.registryUri) {
                String image = svcContainer.imageName
                if (!image.startsWith("${svcContainer.registryUri}/")) {
                    svcContainer.imageName = "${svcContainer.registryUri}/$image"
                }
            }

            if(svcContainer.credentialName){

                EFClient efClient = new EFClient()
                def cred = efClient.getCredentials(svcContainer.credentialName)
                def (repoBaseUrl, secretName) = constructSecretName(svcContainer.imageName, cred.userName)
                createOrUpdateSecret(secretName, cred.userName, cred.password, repoBaseUrl,
                        clusterEndPoint, namespace, accessToken)
                if (!imagePullSecrets.contains(secretName)) {
                    imagePullSecrets.add(secretName)
                }
            }
        }

        def deploymentName = formatName(serviceDetails.serviceName)
        def existingDeployment = getDeployment(clusterEndPoint, namespace, deploymentName, accessToken)
        def deployment = buildDeploymentPayload(serviceDetails, existingDeployment, imagePullSecrets)
        logger DEBUG, "Deployment payload:\n $deployment"

        if (OFFLINE) return null

        if (existingDeployment) {
            logger INFO, "Updating existing deployment $deploymentName"
            doHttpRequest(PUT,
                    clusterEndPoint,
                    "/apis/extensions/v1beta1/namespaces/${namespace}/deployments/$deploymentName",
                    ['Authorization' : accessToken],
                    /*failOnErrorCode*/ true,
                    deployment)

        } else {
            logger INFO, "Creating deployment $deploymentName"
            doHttpRequest(POST,
                    clusterEndPoint,
                    "/apis/extensions/v1beta1/namespaces/${namespace}/deployments",
                    ['Authorization' : accessToken],
                    /*failOnErrorCode*/ true,
                    deployment)
        }

    }

    def createOrUpdateResource(String clusterEndPoint, def resourceDetails, String resourceUri, String createFlag, String contentType, String accessToken) {
        
        if (OFFLINE) return null

        if(createFlag == 'create'){
            logger INFO, "Creating resource at ${resourceUri}"
            doHttpRequest(POST,
                    clusterEndPoint,
                    resourceUri,
                    ['Authorization' : accessToken, 'Content-Type': contentType],
                    /*failOnErrorCode*/ true,
                    resourceDetails)    
        } else {
            logger INFO, "Updating resource at ${resourceUri}"
            doHttpRequest(PUT,
                    clusterEndPoint,
                    resourceUri,
                    ['Authorization' : accessToken, 'Content-Type': contentType],
                    /*failOnErrorCode*/ true,
                    resourceDetails)
        }
    }

    def convertVolumes(data){
        def jsonData = parseJsonToList(data)
        def result = []
        for (item in jsonData){
            def name = formatName(item.name)
            if(item.hostPath){
                result << [name: name, hostPath: [item.hostPath]]
            } else {
                result << [name: name, emptyDir: {}]
            }
        }
        return (new JsonBuilder(result))

    }


    String buildDeploymentPayload(def args, def existingDeployment, def imagePullSecretsList){

        if (!args.defaultCapacity) {
            args.defaultCapacity = 1
        }

        def json = new JsonBuilder()
        //Get the message calculation out of the way
        int maxSurgeValue = args.maxCapacity ? (args.maxCapacity.toInteger() - args.defaultCapacity.toInteger()) : 1
        int maxUnavailableValue =  args.minCapacity ?
                (args.defaultCapacity.toInteger() - args.minCapacity.toInteger()) : 1

        def volumeData = convertVolumes(args.volumes)
        def serviceName = formatName(args.serviceName)
        def result = json {
            kind "Deployment"
            apiVersion "extensions/v1beta1"
            metadata {
                name serviceName
            }
            spec {
                replicas args.defaultCapacity.toInteger()
                strategy {
                    rollingUpdate {
                        maxUnavailable maxUnavailableValue
                        maxSurge maxSurgeValue
                    }
                }
                selector {
                    matchLabels {
                        "ec-svc" serviceName
                    }
                }
                template {
                    metadata {
                        name serviceName
                        labels {
                            "ec-svc" serviceName
                        }
                    }
                    spec{
                        containers(args.container.collect { svcContainer ->
                            def limits = [:]
                            if (svcContainer.memoryLimit) {
                                limits.memory = "${svcContainer.memoryLimit}M"
                            }
                            if (svcContainer.cpuLimit) {
                                Integer cpu = convertCpuToMilliCpu(svcContainer.cpuLimit.toFloat())
                                limits.cpu = "${cpu}m"
                            }

                            def requests = [:]
                            if (svcContainer.memorySize) {
                                requests.memory = "${svcContainer.memorySize}M"
                            }
                            if (svcContainer.cpuCount) {
                                Integer cpu = convertCpuToMilliCpu(svcContainer.cpuCount.toFloat())
                                requests.cpu = "${cpu}m"
                            }

                            def containerResources = [:]
                            if (limits) {
                                containerResources.limits = limits
                            }
                            if (requests) {
                                containerResources.requests = requests
                            }

                            def livenessProbe = [httpGet: "", path:"", port:"", httpHeaders:[:]]
                            def readinessProbe = [exec: [command:[:]]]

                            // If Liveness probe is HTTP based
                            if(getServiceParameter(svcContainer, 'livenessHttpProbe')){
                                livenessProbe.path = getServiceParameter(svcContainer, 'livenessHttpProbePath')
                                livenessProbe.port = getServiceParameter(svcContainer, 'livenessHttpProbePort')
                                livenessProbe.httpHeaders.name = getServiceParameter(svcContainer, 'livenessHttpProbeHttpHeaderName')
                                livenessProbe.httpHeaders.value = getServiceParameter(svcContainer, 'livenessHttpProbeHttpHeaderValue')
                            }
                            def livenessInitialDelay = getServiceParameter(svcContainer, 'livenessInitialDelay')
                            if(livenessInitialDelay){
                                livenessProbe.initialDelaySeconds = "${livenessInitialDelay}"
                            }
                            def livenessPeriod = getServiceParameter(svcContainer, 'livenessPeriod')
                            if(livenessPeriod){
                                livenessProbe.periodSeconds = "${livenessPeriod}"   
                            }

                            def readinessCommand = getServiceParameter(svcContainer, 'readinessCommand')
                            if(readinessCommand){
                                readinessProbe.exec.command = "${readinessCommand}"
                            }
                            def readinessInitialDelay = getServiceParameter(svcContainer, 'readinessInitialDelay')
                            if(readinessInitialDelay){
                                readinessProbe.initialDelaySeconds = "${readinessInitialDelay}"
                            }
                            def readinessPeriod = getServiceParameter(svcContainer, 'readinessPeriod')
                            if(readinessPeriod){
                                readinessProbe.periodSeconds = "${readinessPeriod}"
                            }

                            [
                                    name: formatName(svcContainer.containerName),
                                    image: "${svcContainer.imageName}:${svcContainer.imageVersion?:'latest'}",
                                    command: svcContainer.entryPoint?.split(','),
                                    args: svcContainer.command?.split(','),
                                    livenessProbe: livenessProbe,
                                    readinessProbe: readinessProbe,
                                    ports: svcContainer.port?.collect { port ->
                                        [
                                                name: formatName(port.portName),
                                                containerPort: port.containerPort.toInteger(),
                                                protocol: "TCP"
                                        ]
                                    },
                                    volumeMounts: (parseJsonToList(svcContainer.volumeMounts)).collect { mount ->
                                                        [
                                                            name: formatName(mount.name),
                                                            mountPath: mount.mountPath
                                                        ]

                                        },
                                    env: svcContainer.environmentVariable?.collect { envVar ->
                                        [
                                                name: envVar.environmentVariableName,
                                                value: envVar.value
                                        ]
                                    },
                                    resources: containerResources
                            ]
                        })
                        imagePullSecrets( imagePullSecretsList?.collect { pullSecret ->
                            [name: pullSecret]
                        })
                        volumes(volumeData.content)
                    }
                }

            }
        }

        def payload = existingDeployment
        if (payload) {
            payload = mergeObjs(payload, result)
        } else {
            payload = result
        }
        return ((new JsonBuilder(payload)).toPrettyString())
    }

    def addServiceParameters(def json, Map args) {

        def value = getServiceParameter(args, 'loadBalancerIP')
        if (value != null) {
            json.loadBalancerIP value
        }

        value = getServiceParameter(args, 'sessionAffinity', 'None')
        if (value != null) {
            json.sessionAffinity value
        }

        value = getServiceParameterArray(args, 'loadBalancerSourceRanges')
        if (value != null) {
            json.loadBalancerSourceRanges value
        }
    }

    def getServiceParameter(Map args, String parameterName, def defaultValue = null) {
        def result = args.parameterDetail?.find {
            it.parameterName == parameterName
        }?.parameterValue

        return result != null ? result : defaultValue
    }

    def getServiceParameterArray(Map args, String parameterName, String defaultValue = null) {
        def value = getServiceParameter(args, parameterName, defaultValue)
        value?.toString()?.tokenize(',')
    }


    String buildServicePayload(Map args, def deployedService){

        def serviceName = formatName(args.serviceName)
        def json = new JsonBuilder()
        def result = json {
            kind "Service"
            apiVersion "v1"

            metadata {
                name serviceName
            }
            //Kubernetes plugin injects this service selector
            //to link the service to the pod that this
            //Deploy service encapsulates.
            spec {
                //service type is currently hard-coded to LoadBalancer
                type "LoadBalancer"
                this.addServiceParameters(delegate, args)

                selector {
                    "ec-svc" serviceName
                }
                ports(args.port.collect { svcPort ->
                    [
                            port: svcPort.listenerPort.toInteger(),
                            //name is required for Kubernetes if more than one port is specified so auto-assign
                            name: formatName(svcPort.portName),
                            targetPort: svcPort.subport?:svcPort.listenerPort.toInteger(),
                            // default to TCP which is the default protocol if not set
                            //protocol: svcPort.protocol?: "TCP"
                            protocol: "TCP"
                    ]
                })
            }
        }

        def payload = deployedService
        if (payload) {
            payload = mergeObjs(payload, result)
        } else {
            payload = result
        }
        return (new JsonBuilder(payload)).toPrettyString()
    }

    def convertCpuToMilliCpu(float cpu) {
        return cpu * 1000 as int
    }

    Object doHttpHead(String requestUrl, String requestUri, String accessToken, boolean failOnErrorCode = true, Map queryArgs){
        doHttpRequest(HEAD,
                      requestUrl,
                      requestUri,
                      ['Authorization' : accessToken],
                      failOnErrorCode,
                      null,
                      queryArgs)
    }

    Object doHttpGet(String requestUrl, String requestUri, String accessToken, boolean failOnErrorCode = true) {

        doHttpRequest(GET,
                requestUrl,
                requestUri,
                ['Authorization' : accessToken],
                failOnErrorCode)
    }

    Object doHttpGet(String requestUrl, String requestUri, String accessToken, boolean failOnErrorCode = true, Map queryArgs) {

        doHttpRequest(GET,
                requestUrl,
                requestUri,
                ['Authorization' : accessToken, 'Content-Type': 'application/json'],
                failOnErrorCode,
                null,
                queryArgs)
    }

    Object doHttpPost(String requestUrl, String requestUri, String accessToken, String requestBody, boolean failOnErrorCode = true) {

        doHttpRequest(POST,
                requestUrl,
                requestUri,
                ['Authorization' : accessToken],
                failOnErrorCode,
                requestBody)
    }

    Object doHttpPut(String requestUrl, String requestUri, String accessToken, String requestBody, boolean failOnErrorCode = true) {

        doHttpRequest(PUT,
                requestUrl,
                requestUri,
                ['Authorization' : accessToken],
                failOnErrorCode,
                requestBody)
    }

    Object doHttpPut(String requestUrl, String requestUri, String accessToken, Object requestBody, boolean failOnErrorCode = true, Map queryArgs) {
        doHttpRequest(PUT,
                      requestUrl,
                      requestUri,
                      ['Authorization' : accessToken],
                      failOnErrorCode,
                      requestBody,
                      queryArgs)
    }    

    Object doHttpDelete(String requestUrl, String requestUri, String accessToken, boolean failOnErrorCode = true) {

        doHttpRequest(DELETE,
                requestUrl,
                requestUri,
                ['Authorization' : accessToken],
                failOnErrorCode)
    }

}