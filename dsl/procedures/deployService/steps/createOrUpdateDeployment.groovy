$[/myProject/scripts/preamble]

//// Input parameters
String serviceName = '$[serviceName]'
String serviceProjectName = '$[serviceProjectName]'
String applicationName = '$[applicationName]'
String clusterName = '$[clusterName]'
String clusterOrEnvProjectName = '$[clusterOrEnvProjectName]'
// default cluster project name if not explicitly set
if (!clusterOrEnvProjectName) {
    clusterOrEnvProjectName = serviceProjectName
}
String environmentName = '$[environmentName]'
String applicationRevisionId = '$[applicationRevisionId]'

String resultsPropertySheet = '$[resultsPropertySheet]'
if (!resultsPropertySheet) {
    resultsPropertySheet = '/myParent/parent'
}

//// -- Driverl scrip logic to provision cluster -- //

EFClient efClient = new EFClient()

def clusterParameters = efClient.getProvisionClusterParameters(
        clusterName,
        clusterOrEnvProjectName,
        environmentName)

def configName = clusterParameters.config

def pluginProjectName = '$[/myProject/projectName]'

def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)
String accessToken = 'Bearer '+pluginConfig.credential.password
def clusterEndpoint = pluginConfig.clusterEndpoint //clusterParameters.clusterURL

KubernetesClient client = new KubernetesClient()

def serviceDetails = efClient.getServiceDeploymentDetails(
        serviceName,
        serviceProjectName,
        applicationName,
        applicationRevisionId,
        clusterName,
        clusterOrEnvProjectName,
        environmentName)

client.createOrUpdateService(clusterEndpoint, serviceDetails, accessToken)

client.createOrUpdateDeployment(clusterEndpoint, serviceDetails, accessToken)

def serviceEndpoint = client.getDeployedServiceEndpoint(clusterEndpoint, serviceDetails, accessToken)

if (serviceEndpoint) {
    serviceDetails.port?.each { port ->
        String portName = port.portName
        String url = "${serviceEndpoint}:${port.listenerPort}"
        efClient.createProperty("${resultsPropertySheet}/${serviceName}/${portName}/url", url)
    }
}
