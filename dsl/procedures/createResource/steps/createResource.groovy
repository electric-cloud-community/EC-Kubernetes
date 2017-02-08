$[/myProject/scripts/preamble]

def pluginProjectName = '$[/myProject/projectName]'
//// Input parameters
String configName = '$[config]'
String resourceUri = '$[resourceUri]'
String requestFormat = '$[requestFormat]'
String resourceData = '$[resourceData]'

println "resourceData="+resourceData
//// -- Driver script logic to create resource -- //
EFClient efClient = new EFClient()
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

KubernetesClient client = new KubernetesClient()

String accessToken = client.retrieveAccessToken (pluginConfig)
String clusterEndpoint = pluginConfig.clusterEndpoint

def slurper = new groovy.json.JsonSlurper()
def resourceDetails = slurper.parseText(resourceData)

efClient.logger INFO, "Creating the resource"
client.createResource(clusterEndpoint, resourceDetails, resourceUri,accessToken)