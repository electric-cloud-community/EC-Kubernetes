$[/myProject/scripts/preamble]
$[/myProject/scripts/Discovery]

// Input parameters
def envProjectName = '$[envProjectName]'
def environmentName = '$[envName]'
def clusterName = '$[clusterName]'
def namespace = '$[namespace]'
def projectName = '$[projName]'


// All the parameters are required
EFClient efClient = new EFClient()


def clusters = efClient.getClusters(envProjectName, environmentName)
def cluster = clusters.find {
    it.clusterName == clusterName
}
try {
    if (!cluster) {
        throw new PluginException("Cluster ${clusterName} does not exist in the environment ${environmentName}")
    }
    if (cluster.pluginKey != 'EC-Kubernetes') {
        throw new PluginException("ElectricFlow cluster '$clusterName' in environment '$environmentName' is not backed by a Kubernetes-based cluster")
    }
    KubernetesClient client = new KubernetesClient()

    def pluginConfig = client.getPluginConfig(efClient, clusterName, envProjectName, environmentName)
    def discovery = new Discovery(kubeClient: client, pluginConfig: pluginConfig)

    def services = discovery.discover(namespace)
    discovery.saveToEF(services, projectName, envProjectName, environmentName, clusterName)
} catch (PluginException e) {
    efClient.handleProcedureError(e.getMessage())
}
