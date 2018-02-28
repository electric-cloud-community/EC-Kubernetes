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
def NAMESPACE = "default" //where to get? not necessary??

////A sample is taken from ED-DOCKER Discovery
//String dir = System.getenv('COMMANDER_WORKSPACE')
//File config = new File(dir, 'kubeconfig.yaml')
//config << kubeYAMLFile

def importFromYAML = new ImportFromYAML()
def services = importFromYAML.importFromYAML(NAMESPACE, kubeYAMLFile)
importFromYAML.saveToEF(services, projectName, envProjectName, environmentName, clusterName, )
