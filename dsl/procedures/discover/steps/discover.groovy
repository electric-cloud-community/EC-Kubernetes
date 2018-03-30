import com.electriccloud.client.groovy.ElectricFlow

$[/myProject/scripts/preamble]
$[/myProject/scripts/Discovery]
$[/myProject/scripts/DiscoveryClusterHandler]

// Input parameters
def envProjectName = '$[envProjectName]'
def environmentName = '$[envName]'
def clusterName = '$[clusterName]'
def namespace = '$[namespace]'
def projectName = '$[projName]'
def endpoint = '$[ecp_kubernetes_apiEndpoint]'
def token = '$[ecp_kubernetes_apiToken]'
def applicationScoped = '$[ecp_kubernetes_applicationScoped]'
def applicationName = '$[ecp_kubernetes_applicationName]'

EFClient efClient = new EFClient()
ElectricFlow ef = new ElectricFlow()


if (applicationScoped == 'true') {
    if (!applicationName) {
        efClient.handleProcedureError("Application name must be provided")
    }
}


def cluster
try {
    cluster = ef.getCluster(projectName: envProjectName, environmentName: environmentName, clusterName: clusterName)?.cluster
} catch (RuntimeException e) {
    if (e.message =~ /NoSuchCluster|NoSuchEnvironment|NoSuchProject/) {
        if (!endpoint) {
            efClient.handleProcedureError("Endpoint parameter should be provided")
        }
        if (!token) {
            efClient.handleProcedureError("Token must be provided")
        }

        def discoveryClusterHandler = new DiscoveryClusterHandler()
        def configName = discoveryClusterHandler.ensureConfiguration(endpoint, token)
        def project = discoveryClusterHandler.ensureProject(envProjectName)
        def environment = discoveryClusterHandler.ensureEnvironment(envProjectName, environmentName)
        cluster = discoveryClusterHandler.ensureCluster(envProjectName, environmentName, clusterName, configName)
    }
    else {
        throw e
    }
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
    def discovery = new DiscoveryBuilder()
        .projectName(projectName)
        .applicationName(applicationName)
        .environmentProjectName(envProjectName)
        .environmentName(environmentName)
        .clusterName(clusterName)
        .pluginConfig(pluginConfig)
        .build()

    def services = discovery.discover(namespace)
    discovery.saveToEF(services)
} catch (PluginException e) {
    efClient.handleProcedureError(e.getMessage())
}
