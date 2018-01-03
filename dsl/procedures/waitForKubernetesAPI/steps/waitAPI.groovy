$[/myProject/scripts/preamble]

def pluginProjectName = '$[/myProject/projectName]'
//// Input parameters
String configName = '$[config]'
String responseField = '$[responseField]'
String expectedValue = '$[expectedValue]'
String timeoutInSec = '$[timeoutInSec]'
String outputProperty = '$[outputProperty]'

//// -- Driver script logic to invoke API -- //
EFClient efClient = new EFClient()

def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

String resourceUri = efClient.expandString('$' + '[resourceUri]')?.toString()

KubernetesClient client = new KubernetesClient()
client.setVersion(pluginConfig)
String accessToken = client.retrieveAccessToken (pluginConfig)
String clusterEndpoint = pluginConfig.clusterEndpoint

client.waitKubeAPI(clusterEndpoint, 
					  null, 
					  resourceUri, 
					  "GET", 
					  /* response format */ "json", 
					  accessToken, 
					  responseField, 
					  expectedValue,
					  timeoutInSec.toInteger())

// Control flow reached till this point. 
// Wait completed successfully
if (!outputProperty) {
    outputProperty = efClient.runningInPipeline() ? "/myStageRuntime/k8sWaitAPIResult/${responseField}" : '/myJob/k8sWaitAPIResult/${responseField}'
}

efClient.createProperty2(outputProperty, "Success")
