/**
 * ElectricFlow API client
 */
public class EFClient extends BaseClient {

    def getServerUrl() {
        def commanderServer = System.getenv('COMMANDER_SERVER')
        def secure = Integer.getInteger("COMMANDER_SECURE", 0).intValue()
        def protocol = secure ? "https" : "http"
        def commanderPort = secure ? System.getenv("COMMANDER_HTTPS_PORT") : System.getenv("COMMANDER_PORT")
        def url = "$protocol://$commanderServer:$commanderPort"
        logger DEBUG, "Using ElectricFlow server url: $url"
        url
    }

    Object doHttpGet(String requestUri, boolean failOnErrorCode = true, def query = null) {
        def sessionId = System.getenv('COMMANDER_SESSIONID')
        doHttpRequest(GET, getServerUrl(), requestUri, ['Cookie': "sessionId=$sessionId"],
                failOnErrorCode, /*requestBody*/ null, query)
    }

    Object doHttpPost(String requestUri, Object requestBody, boolean failOnErrorCode = true, def query = null) {
        def sessionId = System.getenv('COMMANDER_SESSIONID')
        doHttpRequest(POST, getServerUrl(), requestUri, ['Cookie': "sessionId=$sessionId"], failOnErrorCode, requestBody, query)
    }

    def getConfigValues(def configPropertySheet, def config, def pluginProjectName) {

        // Get configs property sheet
        def result = doHttpGet("/rest/v1.0/projects/$pluginProjectName/$configPropertySheet", /*failOnErrorCode*/ false)

        def configPropSheetId = result.data?.property?.propertySheetId
        if (!configPropSheetId) {
            handleProcedureError("No plugin configurations exist!")
        }

        result = doHttpGet("/rest/v1.0/propertySheets/$configPropSheetId", /*failOnErrorCode*/ false)
        // Get the property sheet id of the config from the result
        def configProp = result.data.propertySheet.property.find{
            it.propertyName == config
        }

        if (!configProp) {
            handleProcedureError("Configuration $config does not exist!")
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

    def getActualParameters() {
        def jobId = '$[/myJob/jobId]'
        def result = doHttpGet("/rest/v1.0/jobs/$jobId")
        (result.data.job.actualParameter?:[:]).collectEntries {
            [(it.actualParameterName): it.value]
        }
    }

    def getCredentials(def credentialName) {
        def jobStepId = '$[/myJobStep/jobStepId]'
        // Use the new REST mapping for getFullCredential with 'credentialPaths'
        // which works around the restMapping matching issue with the credentialName being a path.
        def result = doHttpGet("/rest/v1.0/jobSteps/$jobStepId/credentialPaths/$credentialName")
        result.data.credential
    }

    def handleConfigurationError(String msg) {
        createProperty('/myJob/configError', msg)
        handleProcedureError(msg)
    }

    def handleProcedureError (String msg) {
        createProperty('summary', "ERROR: $msg")
        handleError(msg)
    }

    def createProperty(String propertyName, String value, Map additionalArgs = [:]) {
        // Creating the property in the context of a job-step by default
        def jobStepId = '$[/myJobStep/jobStepId]'
        def payload = [:]
        payload << additionalArgs
        payload << [
                propertyName: propertyName,
                value: value,
                jobStepId: jobStepId
        ]

        doHttpPost("/rest/v1.0/properties", /* request body */ payload)
    }
}


