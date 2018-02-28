$[/myProject/scripts/preamble]
$[/myProject/scripts/ImportFromYAML]

// Input parameters
def kubeYAMLFile = '''$[kubeYAMLFile]'''
def projectName = '$[projName]'
def envProjectName = '$[envProjectName]'
def environmentName = '$[envName]'
def clusterName = '$[clusterName]'
// All the parameters are required
// TBD add support not required parameters
def NAMESPACE = "default"

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

def importFromYAML = new ImportFromYAML()
def services = importFromYAML.importFromYAML(NAMESPACE, kubeYAMLFile)
importFromYAML.saveToEF(services, projectName, envProjectName, environmentName, clusterName, )
