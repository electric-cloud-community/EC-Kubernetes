$[/myProject/scripts/preamble]

def pluginProjectName = '$[/myProject/projectName]'
//// Input parameters
String configName = '$[config]'
String requestFormat = '$[requestFormat]'
String requestType = '$[requestType]'

//// -- Driver script logic to invoke API -- //
EFClient efClient = new EFClient()

def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

String resourceUri = efClient.expandString('$' + '[resourceUri]')?.toString()
String resourceData = efClient.expandString('$' + '[resourceData]')?.toString()

KubernetesClient client = new KubernetesClient()
client.setVersion(pluginConfig)
String accessToken = client.retrieveAccessToken (pluginConfig)
String clusterEndpoint = pluginConfig.clusterEndpoint

client.invokeKubeAPI(clusterEndpoint, resourceData, resourceUri, requestType, requestFormat, accessToken)