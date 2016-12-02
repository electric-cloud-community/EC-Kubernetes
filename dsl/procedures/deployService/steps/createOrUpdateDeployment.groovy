$[/myProject/scripts/helperClasses]

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

//// -- Driver script logic to provision cluster -- //

EFClient efClient = new EFClient()

def clusterParameters = efClient.getProvisionClusterParameters(
        clusterName,
        clusterOrEnvProjectName,
        environmentName)

def configName = clusterParameters.config
def pluginProjectName = '$[/myProject/projectName]'

def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)


KubernetesClient client = new KubernetesClient()

// Access Token and ClusterEndPoint will be retrieved from flow

def clusterEndPoint
if(clusterDetails){
    clusterEndPoint = "https://${clusterDetails.endpoint}"
    efClient.logger INFO, "Cluster '${efClient.formatName(clusterParameters.clusterName)}' URL: $clusterEndPoint"
} else {
    throw new RuntimeException("Cluster does not exist, please create a cluster and then proceed")
}
clusterEndPoint = "https://107.178.213.141:8080/r/projects/1a7/kubernetes"

def serviceDetails = efClient.getServiceDeploymentDetails(
        serviceName,
        serviceProjectName,
        applicationName,
        applicationRevisionId,
        clusterName,
        clusterOrEnvProjectName,
        environmentName)
// This should work as it is without any modification
client.createOrUpdateService(clusterEndPoint, serviceDetails, accessToken)

client.createOrUpdateDeployment(clusterEndPoint, serviceDetails, accessToken)


