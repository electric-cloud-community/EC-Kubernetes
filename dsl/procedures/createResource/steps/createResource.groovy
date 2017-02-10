$[/myProject/scripts/preamble]

def pluginProjectName = '$[/myProject/projectName]'
//// Input parameters
String configName = '$[config]'
String resourceUri = '$[resourceUri]'
String requestFormat = '$[requestFormat]'
String resourceData = '$[resourceData]'
String requestType = '$[requestType]'

/* Only for testing
println " resourceData="+resourceData
resourceData = '''apiVersion: v1
kind: Namespace
metadata:
  name: ecloud''' */
//// -- Driver script logic to create resource -- //
EFClient efClient = new EFClient()
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

def resourceDetails = ""
if(requestFormat == "yaml"){ // If YAML, convert to JSON
	resourceDetails = efClient.yamlToJson(resourceData)
} else {
	def slurper = new groovy.json.JsonSlurper()	
	resourceDetails = slurper.parseText(resourceData)
}

KubernetesClient client = new KubernetesClient()

String accessToken = client.retrieveAccessToken (pluginConfig)
String clusterEndpoint = pluginConfig.clusterEndpoint

client.createOrUpdateResource(clusterEndpoint, resourceDetails, resourceUri, requestType, accessToken)