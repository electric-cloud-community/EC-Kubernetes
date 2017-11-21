$[/myProject/scripts/preamble]

def pluginProjectName = '$[/myProject/projectName]'
//// Input parameters
String configName = '$[config]'
String resourceUri = '$[resourceUri]'
String requestFormat = '$[requestFormat]'
String resourceData = '$[resourceData]'
String requestType = '$[requestType]'

//// -- Driver script logic to create resource -- //
EFClient efClient = new EFClient()
efClient.logger WARNING, "***Using an deprecated procedure 'Create Resource'! This procedure will be removed in a future release of the plugin. Use procedure 'Invoke Kubernetes API' instead. ***"

def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

def contentType = ""
if(requestFormat == "yaml"){ // If YAML, convert to JSON
	contentType = "application/yaml"
} else {
	contentType = "application/json"
}

KubernetesClient client = new KubernetesClient()
client.setVersion(pluginConfig)
String accessToken = client.retrieveAccessToken (pluginConfig)
String clusterEndpoint = pluginConfig.clusterEndpoint

client.createOrUpdateResource(clusterEndpoint, resourceData, resourceUri, requestType, contentType, accessToken)