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
def pluginProjectName = '$[/myProject/projectName]'

def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

KubernetesClient client = new KubernetesClient()
// Access Token and ClusterEndPoint will be retrieved from flow
def clusterEndPoint
clusterEndPoint = "https://107.178.213.141:8080/r/projects/1a7/kubernetes"
accessToken = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6ImVjbG91ZC10b2tlbi16bnJobSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJlY2xvdWQiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiJmMDg0ODFiNC1iYmFiLTExZTYtYjAzNC0wMmQ4M2U4ODVkZmMiLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6ZGVmYXVsdDplY2xvdWQifQ.iRjOD_FQkzT4Rys40hMTx51nU5ekkyj5PsJqHp_M1K5_N2BvDpfUzqGazFS3-DcCqLIzOwka2ms9ijCYDE4ca1A3KPRlgZC5IOWd-3iogHIVaqIu62ZgXsfAiybMC7xt0WC975i7hSyk3BRWjXGhy6McY-TDC6VvnkIT3REaLEEtWJDHJonw3Zpa_FTtXVzlzmmsuA3CuaoSmMS9BpRE7VmjmeGCjsn1VNseMNaqbofmTB3_OivejT-gXcINf0vYxsmkw9FqM3Ev5Duh9feNry0sh43zDlwRmZT4RZQyK2gsIhota2xFK43h6NnfOjS0dqH4Dmpb2f9qicmVDZcPoQ"

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


