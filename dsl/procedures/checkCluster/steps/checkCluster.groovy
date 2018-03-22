$[/myProject/scripts/preamble]

def pluginProjectName = '$[/myProject/projectName]'
// Input parameters
def configName = '$[config]'

EFClient efClient = new EFClient()
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

String accessToken = 'Bearer ' + pluginConfig.credential.password
String uriToCheckCluster = pluginConfig.uriToCheckCluster
String clusterEndpoint = pluginConfig.clusterEndpoint
String healthCheckUrl = "$clusterEndpoint"

if (uriToCheckCluster){
	if(uriToCheckCluster[0] == '/'){
		healthCheckUrl += uriToCheckCluster
	}
	else{
		healthCheckUrl += '/' + uriToCheckCluster
	}
}


KubernetesClient client = new KubernetesClient()
client.setVersion(pluginConfig)

def resp = client.checkClusterHealth(healthCheckUrl, accessToken)
if (resp.status == 200){ 
	efClient.logger INFO, "The service is reachable at ${clusterEndpoint}. Health check at ${healthCheckUrl}."
}
if (resp.status >= 400){
	efClient.handleProcedureError("The Kubernetes cluster at ${clusterEndpoint} was not reachable. Health check at ${healthCheckUrl} failed with ${resp.statusLine}")
}

