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


println "Using plugin @PLUGIN_NAME@"
println "Environment Project Name: $envProjectName"
println "Environment Name: $environmentName"
println "Cluster Name: $clusterName"
println "Namespace: $namespace"
println "Project Name: $projectName"
println "Endpoint: $endpoint"

if (token) {
    println "Token: ****"
}
if (applicationScoped) {
    println "Application Name: $applicationName"
}

EFClient efClient = new EFClient()
ElectricFlow ef = new ElectricFlow()


if (applicationScoped == 'true') {
    if (!applicationName) {
        efClient.handleProcedureError("Application name must be provided")
    }
}
else {
    applicationName = null
}

def pluginConfig = [credential:[:]]
def cluster
try {
    cluster = ef.getCluster(projectName: envProjectName, environmentName: environmentName, clusterName: clusterName)?.cluster
} catch (RuntimeException e) {
    if (e.message =~ /NoSuchCluster|NoSuchEnvironment|NoSuchProject/) {
        if (!endpoint) {
            efClient.handleProcedureError("API Endpoint parameter must be provided")
        }
        if (!token) {
            efClient.handleProcedureError("Service Account API Token must be provided")
        }

        def discoveryClusterHandler = new DiscoveryClusterHandler()
        def configName = discoveryClusterHandler.ensureConfiguration(endpoint, token)
        def project = discoveryClusterHandler.ensureProject(envProjectName)
        def environment = discoveryClusterHandler.ensureEnvironment(envProjectName, environmentName)
        cluster = discoveryClusterHandler.ensureCluster(envProjectName, environmentName, clusterName, configName)
        pluginConfig.clusterEndpoint = endpoint
        pluginConfig.credential.password = token
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
    if (!pluginConfig.clusterEndpoint) {
        pluginConfig = client.getPluginConfig(efClient, clusterName, envProjectName, environmentName)
    }

    def discovery = new DiscoveryBuilder()
        .projectName(projectName)
        .applicationName(applicationName)
        .environmentProjectName(envProjectName)
        .environmentName(environmentName)
        .clusterName(clusterName)
        .pluginConfig(pluginConfig)
        .build()

    def services = discovery.discover(namespace)
    if (services.size() == 0) {
        print "No services found on the cluster ${pluginConfig.clusterEndpoint}"
        ef.setProperty(propertyName: '/myCall/summary', value: "No services found on the cluster ${pluginConfig.clusterEndpoint}")
        ef.setProperty(propertyName: '/myJobStep/outcome', value: 'warning')
    }
    discovery.saveToEF(services)
} catch (PluginException e) {
    efClient.handleProcedureError(e.getMessage())
}
