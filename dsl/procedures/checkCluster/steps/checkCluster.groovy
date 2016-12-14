$[/myProject/scripts/helperClasses]

def pluginProjectName = '$[/myProject/projectName]'
// Input parameters
def configName = '$[config]'
def clusterEndpoint = '$[clusterURL]'

EFClient efClient = new EFClient()
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)
String accessToken = pluginConfig.credential.password

KubernetesClient client = new KubernetesClient()

def resp = client.checkClusterHealth(clusterEndpoint, accessToken)

if (resp == null) {
	throw new RuntimeException("The Kubernetes cluster is either not reachable or is down")
}

	