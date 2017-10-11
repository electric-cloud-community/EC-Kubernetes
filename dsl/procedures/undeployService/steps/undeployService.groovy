$[/myProject/scripts/preamble]

//// Input parameters
String serviceName = '$[serviceName]'
String serviceProjectName = '$[serviceProjectName]'
String applicationName = '$[applicationName]'
String clusterName = '$[clusterName]'
String envProjectName = '$[envProjectName]'
// default env project name if not explicitly set
if (!envProjectName) {
    envProjectName = serviceProjectName
}
String environmentName = '$[environmentName]'
String applicationRevisionId = '$[applicationRevisionId]'
String serviceEntityRevisionId = '$[serviceEntityRevisionId]'

//// -- Driver script logic to undeploy service -- //
EFClient efClient = new EFClient()
// if cluster is not specified, find the cluster based on the environment that the application is mapped to.
if (!clusterName) {
    if (!applicationName) {
        efClient.handleError("'applicationName' must be specified if 'clusterName' is not specified in order to determine the cluster")
    }
    clusterName = efClient.getServiceCluster(serviceName,
            serviceProjectName,
            applicationName,
            applicationRevisionId,
            environmentName,
            envProjectName)
}

KubernetesClient client = new KubernetesClient()
def pluginConfig = client.getPluginConfig(efClient, clusterName, envProjectName, environmentName)
client.setVersion(pluginConfig)
String accessToken = client.retrieveAccessToken (pluginConfig)
def clusterEndpoint = pluginConfig.clusterEndpoint

def serviceDetails = efClient.getServiceDeploymentDetails(
                serviceName,
                serviceProjectName,
                applicationName,
                applicationRevisionId,
                clusterName,
                envProjectName,
                environmentName,
                serviceEntityRevisionId)
String namespace = client.getServiceParameter(serviceDetails, 'namespace', 'default')

client.undeployService(
        efClient,
        accessToken,
        clusterEndpoint,
        namespace,
        serviceName,
        serviceProjectName,
        applicationName,
        applicationRevisionId,
        clusterName,
        envProjectName,
        environmentName,
        serviceEntityRevisionId)
