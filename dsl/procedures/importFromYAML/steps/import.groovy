$[/myProject/scripts/preamble]
$[/myProject/scripts/ImportFromYAML]

// Input parameters
def kubeYAMLFile = '''$[kubeYAMLFile]'''
def projectName = '$[projName]'
def envProjectName = '$[envProjectName]'
def environmentName = '$[envName]'
def clusterName = '$[clusterName]'
def applicationScoped = '$[application_scoped]'
def applicationName = '$[application_name]'

// TBD add support not required parameters
def NAMESPACE = "default"

EFClient efClient = new EFClient()

if(efClient.toBoolean(applicationScoped)) {
    if (!applicationName) {
        println "Application name is required for creating application-scoped microservices"
        System.exit(-1)
    }
} else {
    //reset application name since its not relevant if application_scoped is not set
    applicationName = null
}

if (envProjectName && environmentName && clusterName) {
    def clusters = efClient.getClusters(envProjectName, environmentName)
    def cluster = clusters.find {
        it.clusterName == clusterName
    }
    if (!cluster) {
        println "Cluster '${clusterName}' does not exist in '${envName}' environment."
        System.exit(-1)
    }
    if (cluster.pluginKey != 'EC-Kubernetes') {
        println "Wrong cluster type: ${cluster.pluginKey}"
        println "ElectricFlow cluster '${clusterName}' in '${envName}' environment is not backed by a Kubernetes-based cluster."
        System.exit(-1)
    }
} else if (envProjectName || environmentName || clusterName) {
    // If any of the environment parameters are specified then *all* of them must be specified.
    println "Either specify all the parameters required to identify the Kubernetes-backed ElectricFlow cluster (environment project name, environment name, and cluster name) where the newly created microservice(s) will be deployed. Or do not specify any of the cluster related parameters in which case the service mapping to a cluster will not be created for the microservice(s)."
    System.exit(-1)
}

def importFromYAML = new ImportFromYAML()
def services = importFromYAML.importFromYAML(NAMESPACE, kubeYAMLFile)
importFromYAML.saveToEF(services, projectName, envProjectName, environmentName, clusterName, applicationName)
