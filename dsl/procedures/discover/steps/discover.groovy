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
if (!cluster) {
    println "Cluster ${clusterName} does not exist in the environment ${environmentName}"
    System.exit(-1)
}
if (cluster.pluginKey != 'EC-Kubernetes') {
    println "Wrong cluster type: ${cluster.pluginKey}"
    System.exit(-1)
}

KubernetesClient client = new KubernetesClient()

def pluginConfig = client.getPluginConfig(efClient, clusterName, envProjectName, environmentName)
def discovery = new Discovery(kubeClient: client, pluginConfig: pluginConfig)

def services = discovery.discover(namespace)
discovery.saveToEF(services, projectName, envProjectName, environmentName, clusterName)

