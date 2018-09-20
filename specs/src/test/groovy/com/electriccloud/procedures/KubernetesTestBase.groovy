package com.electriccloud.procedures

import com.electriccloud.TopologyMatcher
import com.electriccloud.client.api.KubernetesApi
import com.electriccloud.client.ectool.EctoolApi
import com.electriccloud.client.plugin.KubernetesClient
import com.electriccloud.helpers.enums.LogLevels
import com.electriccloud.helpers.enums.ServiceTypes
import com.electriccloud.listeners.TestListener
import io.qameta.allure.Story
import org.testng.annotations.BeforeClass
import org.testng.annotations.Listeners

import java.util.concurrent.TimeUnit

import static com.electriccloud.helpers.enums.ServiceTypes.*
import static io.restassured.RestAssured.given
import static org.awaitility.Awaitility.await
import static org.awaitility.Awaitility.setDefaultTimeout


@Story("EC-Kubernetes")
@Listeners(TestListener.class)
class KubernetesTestBase implements TopologyMatcher {

    def pluginPath = "./src/main/resources"
    def getHost = { hostValue -> new URL(hostValue).host }
    def req = given().relaxedHTTPSValidation().when()
    def volumes = [ source: '[{"name": "html-content","hostPath": "/var/html"}]',
                    target: '[{"name": "html-content","mountPath": "/usr/share/nginx/html"}]' ]


    @BeforeClass
    void setUpData(){
        setDefaultTimeout(200, TimeUnit.SECONDS)
        configName      = 'k8sConfig'
        projectName     = 'k8sProj'
        environmentProjectName = 'k8sProj'
        environmentName = "k8s-environment"
        clusterName     = "k8s-cluster"
        serviceName     = 'nginx-service'
        applicationName = 'nginx-application'
        containerName   = "nginx-container"

        pluginName          = System.getenv("PLUGIN_NAME")
        pluginVersion       = System.getenv("PLUGIN_BUILD_VERSION")
        pluginLegacyVersion = System.getenv("PLUGIN_LEGACY_VERSION")
        clusterEndpoint     = System.getenv("KUBE_CLUSTER_ENDPOINT")
        nodeEndpoint        = System.getenv("KUBE_CKUSTER_NODE_ENDPOINT")
        clusterToken        = System.getenv("KUBE_CLUSTER_TOKEN")
        clusterVersion      = System.getenv("KUBE_CLUSTER_VERSION")

        ectoolApi = new EctoolApi(true)
        k8sClient = new KubernetesClient()
        k8sApi    = new KubernetesApi(clusterEndpoint, clusterToken)

        ectoolApi.ectoolLogin()
    }



    def createAndDeployService(appLevel = false){
        pluginProjectName = "${pluginName}-${pluginVersion}"
        k8sClient.deleteConfiguration(configName)
        k8sClient.createConfiguration(configName, clusterEndpoint, adminAccount, clusterToken, clusterVersion, true, '/api/v1/namespaces')
        k8sClient.createEnvironment(configName)
        if (appLevel){
            k8sClient.createApplication(2, volumes, false, ServiceType.LOAD_BALANCER, "default", 200)
            k8sClient.deployApplication(projectName, applicationName)
        } else {
            k8sClient.createService(2, volumes, false, ServiceType.LOAD_BALANCER)
            k8sClient.deployService(projectName, serviceName)
        }
        await().until { k8sApi.getPods().json.items.last().status.phase == 'Running' }
    }

    def setTopology(appLevel = false) {
        ecpPodName = k8sApi.getPods().json.items.last().metadata.name
        environmentId = k8sClient.client.getEnvironment(projectName, environmentName).json.environment.environmentId
        clusterId = k8sClient.client.getEnvCluster(projectName, environmentName, clusterName).json.cluster.clusterId

        ecpNamespaceId   = "$clusterEndpoint::$ecpNamespaceName"
        ecpClusterId     = clusterEndpoint
        ecpClusterName   = clusterEndpoint
        ecpServiceId     = "$ecpNamespaceId::$serviceName"
        ecpServiceName   = "$ecpNamespaceName::$serviceName"
        ecpPodId         = "$clusterEndpoint::$ecpNamespaceName::$ecpPodName"
        ecpContainerId   = "$ecpPodId::$containerName"
        ecpContainerName = "$ecpNamespaceName::$ecpPodName::$containerName"

        if(appLevel) {
            applicationId = k8sClient.client.getApplication(projectName, applicationName).json.application.applicationId
            serviceId = k8sClient.client.getApplicationService(projectName, applicationName, serviceName).json.service.serviceId
            appServiceId = serviceId
        } else {
            serviceId = k8sClient.client.getService(projectName, serviceName).json.service.serviceId
        }
    }






}
