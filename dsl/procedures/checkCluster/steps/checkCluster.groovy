$[/myProject/scripts/preamble]

def pluginProjectName = '$[/myProject/projectName]'
// Input parameters
def configName = '$[config]'

EFClient efClient = new EFClient()
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

String accessToken = 'Bearer ' + pluginConfig.credential.password
String clusterEndpoint = pluginConfig.clusterEndpoint
String healthCheckUrl = "$clusterEndpoint/api/v1"

KubernetesClient client = new KubernetesClient()
def resp = client.checkClusterHealth(healthCheckUrl, accessToken)
if (resp.status == 200){ 
	efClient.logger INFO, "The service is reachable at ${clusterEndpoint}"
}
if (resp.status >= 400){
	efClient.handleProcedureError("The Kubernetes cluster at ${clusterEndpoint} was not reachable. Health check at $healthCheckUrl failed with $resp.statusLine")
}

