$[/myProject/scripts/preamble]
$[/myProject/scripts/Discovery]

// Input parameters
def envProjectName = '$[envProjectName]'
def environmentName = '$[envName]'
def clusterName = '$[clusterName]'
def namespace = '$[namespace]'
def projectName = '$[projName]'

// TODO validate
println envProjectName
println environmentName
println clusterName
println namespace
println projectName

// All the parameters are required
EFClient efClient = new EFClient()
KubernetesClient client = new KubernetesClient()


def pluginConfig = client.getPluginConfig(efClient, clusterName, envProjectName, environmentName)
println pluginConfig
def discovery = new Discovery(kubeClient: client, pluginConfig: pluginConfig)

def services = discovery.discover(namespace)
discovery.saveToEF(services, projectName, envProjectName, environmentName, clusterName, )
