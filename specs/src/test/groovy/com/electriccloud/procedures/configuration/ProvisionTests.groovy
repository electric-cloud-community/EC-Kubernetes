package com.electriccloud.procedures.configuration

import com.electriccloud.helpers.enums.LogLevels
import com.electriccloud.procedures.KubernetesTestBase
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.qameta.allure.Story
import io.qameta.allure.TmsLink
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import static com.electriccloud.helpers.enums.LogLevels.*


@Feature('Provsioning')
class ProvisionTests extends KubernetesTestBase {

    @BeforeClass
    void setUpTests(){
        k8sClient.deleteConfiguration(configName)
        k8sClient.createConfiguration(configName, clusterEndpoint, 'flowqe', clusterToken, clusterVersion, true, '', LogLevel.DEBUG)
        k8sClient.createEnvironment(configName)
    }

    @AfterClass
    void tearDownTests(){
        k8sClient.client.deleteProject(environmentProjectName)
    }

    @Test
    @TmsLink("")
    @Story("Provisioning of kubernetes environment")
    @Description("Provision exsting Kubernetes cluster")
    void provisionCluster(){
        def resp = k8sClient.provisionEnvironment(projectName, environmentName, clusterName).json
        def jobStatus = k8sClient.client.getJobStatus(resp.jobId)
        def jobLogs = k8sClient.client.getJobLogs(resp.jobId)
        assert jobStatus.json.outcome == "success"
        assert jobStatus.json.status == "completed"
        assert jobLogs.contains("The service is reachable at ${clusterEndpoint}. Health check at ${clusterEndpoint}.")

    }

    @Test(dataProvider = 'invalidData')
    @TmsLink('')
    @Story('Provisioning with invalid data')
    @Description("Provision Kubernetes cluster with invalid data")
    void invalidClusterProvisioning(project, environment, cluster, message){
        try {
            k8sClient.provisionEnvironment(project, environment, cluster).json
        } catch (e){
            assert e.cause.message.contains(message)
        }
    }


    @DataProvider(name = 'invalidData')
    def getProvisionData(){
        def data = [
                ["test", environmentName, clusterName, "NoSuchEnvironment: Environment '${environmentName}' does not exist in project 'test'"],
                ["Default", environmentName, clusterName, "NoSuchEnvironment: Environment '${environmentName}' does not exist in project 'Default'"],
                [projectName, "test", clusterName, "NoSuchEnvironment: Environment 'test' does not exist in project '${projectName}'"],
                [projectName, environmentName, "test-cluster", "NoSuchCluster: Cluster 'test-cluster' does not exist in environment '${environmentName}'"],
        ]
        return data as Object[][]
    }



}