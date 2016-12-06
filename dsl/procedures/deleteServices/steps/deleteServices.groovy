$[/myProject/scripts/helperClasses]

def pluginProjectName = '$[/myProject/projectName]'
// Input parameters
def projectId = '$[clusterProjectID]'
def clusterName = '$[clusterName]'
def masterZone = '$[masterZone]'
def configName = '$[config]'

EFClient efClient = new EFClient()
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

KubernetesClient client = new KubernetesClient()
String accessToken = client.retrieveAccessToken (pluginConfig)

def clusterDetails = client.getCluster(projectId, masterZone, clusterName, accessToken)

if(!clusterDetails){
    client.handleError("Cluster '$clusterName' not found in project '$projectId' and zone '$masterZone'")
}

def clusterEndPoint = "https://${clusterDetails.endpoint}"

efClient.logger WARNING, "Deleting all services, and deployments in the cluster '$clusterName'!"
def serviceList = client.doHttpGet(clusterEndPoint,
                                   '/api/v1/namespaces/default/services/',
                                   accessToken)
for(service in serviceList.data.items){
    def svcName = service.metadata.name
    //skip the 'kubernetes' service
    if (svcName == 'kubernetes') continue
    efClient.logger INFO, "Deleting service $svcName"
    client.doHttpDelete(clusterEndPoint,
                         "/api/v1/namespaces/default/services/${svcName}",
                         accessToken)
}

def deploymentList = client.doHttpGet(clusterEndPoint,
                                   '/apis/extensions/v1beta1/namespaces/default/deployments',
                                   accessToken)

for(deployment in deploymentList.data.items){
    def deploymentName = deployment.metadata.name
    efClient.logger INFO, "Deleting deployment $deploymentName"
    client.doHttpDelete(clusterEndPoint,
                        "/apis/extensions/v1beta1/namespaces/default/deployments/${deploymentName}",
                        accessToken)

}

def rcList = client.doHttpGet(clusterEndPoint,
                              '/apis/extensions/v1beta1/namespaces/default/replicasets',
                              accessToken)

for(rc in rcList.data.items){

    def rcName = rc.metadata.name
    efClient.logger INFO, "Deleting replicaset $rcName"
    client.doHttpDelete(clusterEndPoint,
                        "/apis/extensions/v1beta1/namespaces/default/replicasets/${rcName}",
                        accessToken)
}


def podList = client.doHttpGet(clusterEndPoint,
                               '/api/v1/namespaces/default/pods/',
                               accessToken)
for(pod in podList.data.items){
    def podName = pod.metadata.name
    efClient.logger INFO, "Deleting pod $podName"
    client.doHttpDelete(clusterEndPoint,
                        "/api/v1/namespaces/default/pods/${podName}",
                        accessToken)
}