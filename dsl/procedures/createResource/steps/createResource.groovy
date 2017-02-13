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
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

def contentType = ""
if(requestFormat == "yaml"){ // If YAML, convert to JSON
	contentType = "application/yaml"
} else {
	contentType = "application/json"
}

KubernetesClient client = new KubernetesClient()

String accessToken = client.retrieveAccessToken (pluginConfig)
String clusterEndpoint = pluginConfig.clusterEndpoint

client.createOrUpdateResource(clusterEndpoint, resourceData, resourceUri, requestType, contentType, accessToken)