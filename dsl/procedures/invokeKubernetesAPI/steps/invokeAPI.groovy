$[/myProject/scripts/preamble]

def pluginProjectName = '$[/myProject/projectName]'
//// Input parameters
String configName = '$[config]'
String requestFormat = '$[requestFormat]'
String requestType = '$[requestType]'
String outputProperty = '$[outputProperty]'

//// -- Driver script logic to invoke API -- //
EFClient efClient = new EFClient()

def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

String resourceUri = efClient.expandString('$' + '[resourceUri]')?.toString()
String resourceData = efClient.expandString('$' + '[resourceData]')?.toString()

KubernetesClient client = new KubernetesClient()
client.setVersion(pluginConfig)
String accessToken = client.retrieveAccessToken (pluginConfig)
String clusterEndpoint = pluginConfig.clusterEndpoint

def response = client.invokeKubeAPI(clusterEndpoint, resourceData, resourceUri, requestType, requestFormat, accessToken)

if (!outputProperty) {
    outputProperty = efClient.runningInPipeline() ? '/myStageRuntime/k8sAPIResult' : '/myJob/k8sAPIResult'
}

String value = response.data ? (new JsonBuilder(response.data)).toString(): ''
efClient.createProperty2(outputProperty, value)
