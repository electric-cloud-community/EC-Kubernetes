$[/myProject/scripts/helperClasses]

def pluginProjectName = '$[/myProject/projectName]'
// Input parameters
def configName = '$[config]'

EFClient efClient = new EFClient()
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

KubernetesClient client = new KubernetesClient()

String accessToken = 'Bearer '+pluginConfig.credential.password
String clusterEndpoint = pluginConfig.clusterEndpoint

efClient.logger WARNING, "Deleting all services, and deployments in the cluster!"
def serviceList = client.doHttpGet(clusterEndpoint,
                                   '/api/v1/namespaces/default/services/',
                                   accessToken)
for(service in serviceList.data.items){
    def svcName = service.metadata.name
    //skip the 'kubernetes' service
    if (svcName == 'kubernetes') continue
    efClient.logger INFO, "Deleting service $svcName"
    client.doHttpDelete(clusterEndpoint,
                         "/api/v1/namespaces/default/services/${svcName}",
                         accessToken)
}

def deploymentList = client.doHttpGet(clusterEndpoint,
                                   '/apis/extensions/v1beta1/namespaces/default/deployments',
                                   accessToken)

for(deployment in deploymentList.data.items){
    def deploymentName = deployment.metadata.name
    efClient.logger INFO, "Deleting deployment $deploymentName"
    client.doHttpDelete(clusterEndpoint,
                        "/apis/extensions/v1beta1/namespaces/default/deployments/${deploymentName}",
                        accessToken)

}

def rcList = client.doHttpGet(clusterEndpoint,
                              '/apis/extensions/v1beta1/namespaces/default/replicasets',
                              accessToken)

for(rc in rcList.data.items){

    def rcName = rc.metadata.name
    efClient.logger INFO, "Deleting replicaset $rcName"
    client.doHttpDelete(clusterEndpoint,
                        "/apis/extensions/v1beta1/namespaces/default/replicasets/${rcName}",
                        accessToken)
}


def podList = client.doHttpGet(clusterEndpoint,
                               '/api/v1/namespaces/default/pods/',
                               accessToken)
for(pod in podList.data.items){
    def podName = pod.metadata.name
    efClient.logger INFO, "Deleting pod $podName"
    client.doHttpDelete(clusterEndpoint,
                        "/api/v1/namespaces/default/pods/${podName}",
                        accessToken)
}