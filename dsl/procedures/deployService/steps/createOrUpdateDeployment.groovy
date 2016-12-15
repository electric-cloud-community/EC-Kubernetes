$[/myProject/scripts/helperClasses]

//// Input parameters
String serviceName = '$[serviceName]'
String serviceProjectName = '$[serviceProjectName]'
String applicationName = '$[applicationName]'
String clusterName = '$[clusterName]'
String clusterOrEnvProjectName = '$[clusterOrEnvProjectName]'
// default cluster project name if not explicitly set
if (!clusterOrEnvProjectName) {
    clusterOrEnvProjectName = serviceProjectName
}
String environmentName = '$[environmentName]'
String applicationRevisionId = '$[applicationRevisionId]'

//// -- Driverl script logic to provision cluster -- //

EFClient efClient = new EFClient()

def clusterParameters = efClient.getProvisionClusterParameters(
        clusterName,
        clusterOrEnvProjectName,
        environmentName)

def configName = clusterParameters.config
def clusterEndpoint = clusterParameters.clusterURL

def pluginProjectName = '$[/myProject/projectName]'

def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)
String accessToken = pluginConfig.credential.password

KubernetesClient client = new KubernetesClient()

// Access Token and ClusterEndPoint will be retrieved from flow
// Inputs to be used if not provided from Flow
//clusterEndPoint = "https://104.198.101.220:6443"
//accessToken = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6ImVjbG91ZC1rcy10b2tlbi1jc2htZiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJlY2xvdWQta3MiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiJjMDI1ZjVmMS1iZDUwLTExZTYtYTMxOC00MjAxMGE4YTAwMDciLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6ZGVmYXVsdDplY2xvdWQta3MifQ.hdfJi0Nci5EyHAhGSLzUptxYwVUjgWcj08qJU5AYQBbYm690CI1mh3VYJE1lllaKvNLrITCGxjSbfns768y-gsH9MTvAOEl35s2z6QGwxB9Uh-XJd5QNeCWVDFJopjntcW_qz7JMV9Fkm-N22H5jxKOTbadyFZQxBLJinSfJsjO7TV9Yy6LMjLnkGzkRwEK2wsTKSu9gJK365gfe9uHEmxQgl5_kTW_fb-SeON2e5AJQ_9v2NceGKmIdQrankKgGEVUWUne1DPLfw97He_D1cbNSRlfo6ZnQU11yMfCAJ5kMAapwM6Hl9exEvbzfuuS4SifpgvH-kXYMOTx8QIEylw"

def serviceDetails = efClient.getServiceDeploymentDetails(
        serviceName,
        serviceProjectName,
        applicationName,
        applicationRevisionId,
        clusterName,
        clusterOrEnvProjectName,
        environmentName)

// This should work as it is without any modification
client.createOrUpdateService(clusterEndPoint, serviceDetails, accessToken)

client.createOrUpdateDeployment(clusterEndPoint, serviceDetails, accessToken)
