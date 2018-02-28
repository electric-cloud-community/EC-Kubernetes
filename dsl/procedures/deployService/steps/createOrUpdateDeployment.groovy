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
String serviceEntityRevisionId = '$[serviceEntityRevisionId]'

String resultsPropertySheet = '$[resultsPropertySheet]'
if (!resultsPropertySheet) {
    resultsPropertySheet = '/myParent/parent'
}
//// -- Driver script logic to provision cluster -- //
EFClient efClient = new EFClient()

KubernetesClient client = new KubernetesClient()
def pluginConfig = client.getPluginConfig(efClient, clusterName, clusterOrEnvProjectName, environmentName)
String accessToken = client.retrieveAccessToken (pluginConfig)
def clusterEndpoint = pluginConfig.clusterEndpoint

def serviceDetails = efClient.getServiceDeploymentDetails(
                serviceName,
                serviceProjectName,
                applicationName,
                applicationRevisionId,
                clusterName,
                clusterOrEnvProjectName,
                environmentName,
                serviceEntityRevisionId)
String namespace = client.getServiceParameter(serviceDetails, 'namespace', 'default')
client.clusterEndpoint = clusterEndpoint
client.accessToken = accessToken
client.namespace = namespace

try {
    client.deployService(
            efClient,
            accessToken,
            clusterEndpoint,
            namespace,
            serviceName,
            serviceProjectName,
            applicationName,
            applicationRevisionId,
            clusterName,
            clusterOrEnvProjectName,
            environmentName,
            resultsPropertySheet,
            serviceEntityRevisionId)
} catch (PluginException e) {
    efClient.handleProcedureError(e.getMessage())
}
