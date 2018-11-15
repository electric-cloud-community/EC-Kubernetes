package com.electriccloud.procedures.configuration

import com.electriccloud.procedures.KubernetesTestBase
import com.electriccloud.test_data.ConfigurationData
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import static com.electriccloud.models.enums.LogLevels.LogLevel.*
import static com.electriccloud.models.enums.ServiceTypes.*

@Feature('Provsioning')
class ProvisionTests extends KubernetesTestBase {

    @BeforeClass(alwaysRun = true)
    void setUpTests(){
        k8sClient.deleteConfiguration(configName)
        k8sClient.createConfiguration(configName, clusterEndpoint, adminAccount, clusterToken, clusterVersion, true, '/apis', DEBUG)
        k8sClient.createEnvironment(configName)
    }

    @AfterClass(alwaysRun = true)
    void tearDownTests(){
        k8sClient.client.deleteProject(environmentProjectName)
    }

    @Test(groups = "Positive")
    @Story("Provisioning of kubernetes environment")
    @Description("Provision exsting Kubernetes cluster")
    void provisionCluster(){
        def resp = k8sClient.provisionEnvironment(projectName, environmentName, clusterName).json
        def jobStatus = k8sClient.client.getJobStatus(resp.jobId)
        def jobLogs = k8sClient.client.getJobLogs(resp.jobId)
        assert jobStatus.json.outcome == "success"
        assert jobStatus.json.status == "completed"
        assert jobLogs.contains("The service is reachable at ${clusterEndpoint}. Health check at ${clusterEndpoint}/apis.")

    }


    @Test(groups = "Negative", dataProvider = 'invalidProvisionData', dataProviderClass = ConfigurationData.class)
    @Story('Provisioning with invalid data')
    @Description("Provision Kubernetes cluster with invalid data")
    void invalidClusterProvisioning(project, environment, cluster, message){
        try {
            k8sClient.provisionEnvironment(project, environment, cluster).json
        } catch (e){
            assert e.cause.message.contains(message)
        }
    }






}