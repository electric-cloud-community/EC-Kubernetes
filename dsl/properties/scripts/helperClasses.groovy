@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1' )
@Grab(group='net.sf.json-lib', module='json-lib', version='2.3', classifier = 'jdk15') 

import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.DELETE
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.PUT

import static Logger.*

class Logger {
    static final Integer DEBUG = 1
    static final Integer INFO = 2
    static final Integer WARNING = 3
    static final Integer ERROR = 4
    static Integer logLevel = DEBUG
}


public class KubernetesClient extends BaseClient {

    /**
    *  A way to check cluster is up/reachable. Currently simply hits API base endpoint
    *  In future there might be a "health" endpoint available which can be used
    */
    def checkClusterHealth(String clusterEndPoint, String accessToken){

        if (OFFLINE) return null

        def response =  doHttpGet(clusterEndPoint,
                "/apis",
                accessToken, /*failOnErrorCode*/ false)
        response.status == 200 ? response.data : null

    }

    // Need to check if accessToken would suffice or we would need user/password
    /**
     * Retrieves the Deployment instance from Kubernetes cluster.
     * Returns null if no Deployment instance by the given name is found.
     */
    def getDeployment(String clusterEndPoint, String deploymentName, String accessToken) {

        if (OFFLINE) return null

        def response = doHttpGet(clusterEndPoint,
                "/apis/extensions/v1beta1/namespaces/default/deployments/${formatName(deploymentName)}",
                accessToken, /*failOnErrorCode*/ false)
        response.status == 200 ? response.data : null
    }

    /**
     * Retrieves the Service instance from Kubernetes cluster.
     * Returns null if no Service instance by the given name is found.
     */
    def getService(String clusterEndPoint, String serviceName, String accessToken) {

        if (OFFLINE) return null

        def response = doHttpGet(clusterEndPoint,
                "/api/v1/namespaces/default/services/$serviceName",
                accessToken, /*failOnErrorCode*/ false)
        response.status == 200 ? response.data : null
    }

    def createOrUpdateService(String clusterEndPoint, def serviceDetails, String accessToken) {

        String serviceName = formatName(serviceDetails.serviceName)
        def deployedService = getService(clusterEndPoint, serviceName, accessToken)

        def serviceDefinition = buildServicePayload(serviceDetails, deployedService)

        if (OFFLINE) return null

        if(deployedService){
            logger INFO, "Updating deployed service $serviceName"
            doHttpRequest(PUT,
                    clusterEndPoint,
                    "/api/v1/namespaces/default/services/$serviceName",
                    ['Authorization' : accessToken],
                    /*failOnErrorCode*/ true,
                    serviceDefinition)

        } else {
            logger INFO, "Creating service $serviceName"
            doHttpRequest(POST,
                    clusterEndPoint,
                    '/api/v1/namespaces/default/services',
                    ['Authorization' : accessToken],
                    /*failOnErrorCode*/ true,
                    serviceDefinition)
        }
    }

def createOrUpdateSecret(def secretName, def username, def password, def repoBaseUrl,
                         String clusterEndPoint, String accessToken){
        def existingSecret = getSecret(secretName, clusterEndPoint, accessToken)
        def secret = buildSecretPayload(secretName, username, password, repoBaseUrl)
        if (OFFLINE) return null
        if (existingSecret) {
                    logger INFO, "Updating existing Secret $secretName"
                    doHttpRequest(PUT,
                            clusterEndPoint,
                            "/api/v1/namespaces/default/secrets/${secretName}",
                            ['Authorization' : accessToken],
                            /*failOnErrorCode*/ true,
                            secret)

                } else {
                    logger INFO, "Creating deployment $secretName"
                    doHttpRequest(POST,
                            clusterEndPoint,
                            '/api/v1/namespaces/default/secrets',
                            ['Authorization' : accessToken],
                            /*failOnErrorCode*/ true,
                            secret)
                }        
    }

    def getSecret(def secretName, def clusterEndPoint, def accessToken) {

        if (OFFLINE) return null

        def response = doHttpGet(clusterEndPoint,
                "/api/v1/namespaces/default/secrets/${secretName}",
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

    def createOrUpdateDeployment(String clusterEndPoint, def serviceDetails, String accessToken) {

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
                        clusterEndPoint, accessToken)
                if (!imagePullSecrets.contains(secretName)) {
                    imagePullSecrets.add(secretName)
                }
            }
        }

        def deploymentName = formatName(serviceDetails.serviceName)
        def existingDeployment = getDeployment(clusterEndPoint, deploymentName, accessToken)
        def deployment = buildDeploymentPayload(serviceDetails, existingDeployment, imagePullSecrets)
        println deployment


        if (OFFLINE) return null

        if (existingDeployment) {
            logger INFO, "Updating existing deployment $deploymentName"
            doHttpRequest(PUT,
                    clusterEndPoint,
                    "/apis/extensions/v1beta1/namespaces/default/deployments/$deploymentName",
                    ['Authorization' : accessToken],
                    /*failOnErrorCode*/ true,
                    deployment)

        } else {
            logger INFO, "Creating deployment $deploymentName"
            doHttpRequest(POST,
                    clusterEndPoint,
                    '/apis/extensions/v1beta1/namespaces/default/deployments',
                    ['Authorization' : accessToken],
                    /*failOnErrorCode*/ true,
                    deployment)
        }

    }

    Object getJsonFromXML(Object xmlData){
        if(!xmlData){
            return []
        }
        def parsed

        try {
            parsed = new JsonSlurper().parseText(xmlData)
        } catch (Exception e) {
            logger(ERROR, "Cannot parse mount points json: $json")
            System.exit(-1)
        }
        if (!(parsed instanceof List)) {
                parsed = [ parsed ]
            }
        parsed
    }

    def convertVolumes(xmlData){
        def jsonData = getJsonFromXML(xmlData)
        def result = []
        for (item in jsonData){
            if(item.hostPath){
                result << [name: formatName(item.name), hostPath: [item.hostPath]]
            } else {
                result << [name: formatName(item.name), emptyDir: {}]
            }
        }
        return (new JsonBuilder(result))

    }


    String buildDeploymentPayload(def args, def existingDeployment, def imagePullSecretsList){

        def json = new JsonBuilder()
        //Get the message calculation out of the way
        int maxSurgeValue = args.maxCapacity ? (args.maxCapacity.toInteger() - args.defaultCapacity.toInteger()) : 1
        int maxUnavailableValue =  args.minCapacity ?
                (args.defaultCapacity.toInteger() - args.minCapacity.toInteger()) : 1

        def volumeData = convertVolumes(args.volumes)
        String svcName = formatName(args.serviceName)
        def result = json {
            kind "Deployment"
            apiVersion "extensions/v1beta1"
            metadata {
                name svcName
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
                        "ec-svc" svcName
                    }
                }
                template {
                    metadata {
                        name svcName
                        labels {
                            "ec-svc" svcName
                        }
                    }
                    spec{
                        containers(args.container.collect { svcContainer ->
                            def limits = [:]
                            if (svcContainer.memoryLimit) {
                                limits.memory = "${svcContainer.memoryLimit}M"
                            }
                            if (svcContainer.cpuLimit) {
                                limits.cpu = svcContainer.cpuLimit
                            }

                            def requests = [:]
                            if (svcContainer.memorySize) {
                                requests.memory = "${svcContainer.memorySize}M"
                            }
                            if (svcContainer.cpuCount) {
                                requests.cpu = svcContainer.cpuCount
                            }

                            def containerResources = [:]
                            if (limits) {
                                containerResources.limits = limits
                            }
                            if (requests) {
                                containerResources.requests = requests
                            }

                            [
                                    name: formatName(svcContainer.containerName),
                                    image: "${svcContainer.imageName}:${svcContainer.imageVersion}",
                                    command: svcContainer.entryPoint?.split(','),
                                    args: svcContainer.command?.split(','),
                                    ports: svcContainer.port?.collect { port ->
                                        [
                                                name: formatName(port.portName),
                                                containerPort: port.containerPort.toInteger(),
                                                protocol: "TCP"
                                        ]
                                    },
                                    volumeMounts: (getJsonFromXML(svcContainer.volumeMounts)).collect { mount ->
                                                        [
                                                            name: formatName(mount.name),
                                                            mountPath: mount.mountPath
                                                        ]

                                        },
                                    //TODO: handle null
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

        def json = new JsonBuilder()
        String svcName = formatName(args.serviceName)
        def result = json {
            kind "Service"
            apiVersion "v1"

            metadata {
                name svcName
            }
            //Kubernetes plugin injects this service selector
            //to link the service to the pod that this
            //Deploy service encapsulates.
            spec {
                //service type is currently hard-coded to LoadBalancer
                type "LoadBalancer"
                this.addServiceParameters(delegate, args)

                selector {
                    "ec-svc" svcName
                }
                ports(args.port.collect { svcPort ->
                    [
                            port: svcPort.listenerPort.toInteger(),
                            //name is required for Kubernetes if more than one port is specified so auto-assign
                            name: svcPort.portName,
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

}

public class EFClient extends BaseClient {

    def getServerUrl() {
        def commanderServer = System.getenv('COMMANDER_SERVER')
        def secure = Integer.getInteger("COMMANDER_SECURE", 0).intValue()
        def protocol = secure ? "https" : "http"
        def commanderPort = secure ? System.getenv("COMMANDER_HTTPS_PORT") : System.getenv("COMMANDER_PORT")
        def url = "$protocol://$commanderServer:$commanderPort"
        System.out.println(url)
        url
    }

    Object doHttpGet(String requestUri, boolean failOnErrorCode = true, def query = null) {
        def sessionId = System.getenv('COMMANDER_SESSIONID')
        doHttpRequest(GET, getServerUrl(), requestUri, ['Cookie': "sessionId=$sessionId"],
                failOnErrorCode, /*requestBody*/ null, query)
    }

    Object doHttpPost(String requestUri, Object requestBody, boolean failOnErrorCode = true) {
        def sessionId = System.getenv('COMMANDER_SESSIONID')
        doHttpRequest(POST, getServerUrl(), requestUri, ['Cookie': "sessionId=$sessionId"], failOnErrorCode, requestBody)
    }

    def getConfigValues(def configPropertySheet, def config, def pluginProjectName) {

        // Get configs property sheet
        def result = doHttpGet("/rest/v1.0/projects/$pluginProjectName/$configPropertySheet", /*failOnErrorCode*/ false)

        def configPropSheetId = result.data?.property?.propertySheetId
        if (!configPropSheetId) {
            throw new RuntimeException("No plugin configurations exist!")
        }

        result = doHttpGet("/rest/v1.0/propertySheets/$configPropSheetId", /*failOnErrorCode*/ false)
        // Get the property sheet id of the config from the result
        def configProp = result.data.propertySheet.property.find{
            it.propertyName == config
        }

        if (!configProp) {
            throw new RuntimeException("Configuration $config does not exist!")
        }

        result = doHttpGet("/rest/v1.0/propertySheets/$configProp.propertySheetId")

        def values = result.data.propertySheet.property.collectEntries{
            [(it.propertyName): it.value]
        }

        logger(INFO, "Config values: " + values)

        def cred = getCredentials(config)
        values << [credential: [userName: cred.userName, password: cred.password]]

        //Set the log level using the plugin configuration setting
        logLevel = (values.logLevel?: INFO).toInteger()

        values
    }

    def getProvisionClusterParameters(String clusterName,
                                      String clusterOrEnvProjectName,
                                      String environmentName) {

        def partialUri = environmentName ?
                "projects/$clusterOrEnvProjectName/environments/$environmentName/clusters/$clusterName" :
                "projects/$clusterOrEnvProjectName/clusters/$clusterName"

        def result = doHttpGet("/rest/v1.0/$partialUri")

        def params = result.data.cluster?.provisionParameters?.parameterDetail

        if(!params) {
            handleError("No provision parameters found for cluster $clusterName!")
        }

        def provisionParams = params.collectEntries {
            [(it.parameterName): it.parameterValue]
        }

        logger DEBUG, "Cluster params from Deploy: $provisionParams"

        return provisionParams
    }

    def getServiceDeploymentDetails(String serviceName,
                                    String serviceProjectName,
                                    String applicationName,
                                    String applicationRevisionId,
                                    String clusterName,
                                    String clusterProjectName,
                                    String environmentName) {

        def partialUri = applicationName ?
                "projects/$serviceProjectName/applications/$applicationName/services/$serviceName" :
                "projects/$serviceProjectName/services/$serviceName"
        def queryArgs = [
                request: 'getServiceDeploymentDetails',
                clusterName: clusterName,
                clusterProjectName: clusterProjectName,
                environmentName: environmentName,
                applicationEntityRevisionId: applicationRevisionId
        ]
        def result = doHttpGet("/rest/v1.0/$partialUri", /*failOnErrorCode*/ true, queryArgs)

        def svcDetails = result.data.service
        logger DEBUG, "Service Details: " + JsonOutput.toJson(svcDetails)

        svcDetails
    }

    def getCredentials(def credentialName) {
        def jobStepId = '$[/myJobStep/jobStepId]'
        // Use the new REST mapping for getFullCredential with 'credentialPaths'
        // which works around the restMapping matching issue with the credentialName being a path.
        def result = doHttpGet("/rest/v1.0/jobSteps/$jobStepId/credentialPaths/$credentialName")
        result.data.credential
    }

}

public class BaseClient {

    //Meant for use during development if there is no internet access
    //in which case Kubernetes calls will become no-ops.
    final boolean OFFLINE = false

    Object doHttpRequest(Method method, String requestUrl,
                         String requestUri, def requestHeaders,
                         boolean failOnErrorCode = true,
                         Object requestBody = null,
                         def queryArgs = null) {

        logger DEBUG, "requestUrl: $requestUrl"
        logger DEBUG, "method: $method"
        logger DEBUG, "URI: $requestUri"
        if (queryArgs) {
            logger DEBUG, "queryArgs: '$queryArgs'"
        }
        logger DEBUG, "URL: '$requestUrl$requestUri'"
        if (requestBody) logger DEBUG, "Payload: $requestBody"

        def http = new HTTPBuilder(requestUrl)
        http.ignoreSSLIssues()

        http.request(method, JSON) {
            if (requestUri) {
                uri.path = requestUri
            }
            if (queryArgs) {
                uri.query = queryArgs
            }
            headers = requestHeaders
            body = requestBody

            response.success = { resp, json ->
                logger DEBUG, "request was successful $resp.statusLine.statusCode $json"
                [statusLine: resp.statusLine,
                 status: resp.status,
                 data      : json]
            }

            response.failure = { resp, reader ->
                if (failOnErrorCode) {
                    logger ERROR, "Error details: $reader"
                    handleError("Request failed with $resp.statusLine")
                } else {
                    logger INFO, "Response: $reader"
                }

                [statusLine: resp.statusLine,
                 status: resp.status]
            }
        }
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
                ['Authorization' : accessToken],
                failOnErrorCode,
                null,
                queryArgs)
    }

    Object doHttpPost(String requestUrl, String requestUri, String accessToken, Object requestBody, boolean failOnErrorCode = true) {

        doHttpRequest(POST,
                requestUrl,
                requestUri,
                ['Authorization' : accessToken],
                failOnErrorCode,
                requestBody)
    }

    Object doHttpPut(String requestUrl, String requestUri, String accessToken, Object requestBody, boolean failOnErrorCode = true) {

        doHttpRequest(PUT,
                requestUrl,
                requestUri,
                ['Authorization' : accessToken],
                failOnErrorCode,
                requestBody)
    }

    Object doHttpDelete(String requestUrl, String requestUri, String accessToken, boolean failOnErrorCode = true) {

        doHttpRequest(DELETE,
                requestUrl,
                requestUri,
                ['Authorization' : accessToken],
                failOnErrorCode)
    }

    def mergeObjs(def dest, def src) {
        //Converting both object instances to a map structure
        //to ease merging the two data structures
        logger DEBUG, "Source to merge: " + JsonOutput.toJson(src)
        def result = mergeJSON((new JsonSlurper()).parseText((new JsonBuilder(dest)).toString()),
                (new JsonSlurper()).parseText((new JsonBuilder(src)).toString()))
        logger DEBUG, "After merge: " + JsonOutput.toJson(result)
        return result
    }

    def mergeJSON(def dest, def src) {
        src.each { prop, value ->
            logger DEBUG, "Has property $prop? value:" + dest[prop]
            if(dest[prop] != null && dest[prop] instanceof Map) {
                mergeJSON(dest[prop], value)
            } else {
                dest[prop] = value
            }
        }
        return dest
    }

    /**
     * Based on plugin parameter value truthiness
     * True if value == true or value == '1'
     */
    boolean toBoolean(def value) {
        return value != null && (value == true || value == 'true' || value == 1 || value == '1')
    }

    def handleError (String msg) {
        println "ERROR: $msg"
        System.exit(-1)
    }

    def logger(Integer level, def message) {
        if ( level >= logLevel ) {
            println message
        }
    }

    String formatName(String name){
        return name.replaceAll(':', '-').replaceAll(' ', '-').replaceAll('_', '-').toLowerCase()
    }
}