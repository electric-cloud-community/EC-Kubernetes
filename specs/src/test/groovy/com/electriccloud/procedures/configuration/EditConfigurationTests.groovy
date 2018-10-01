package com.electriccloud.procedures.configuration

import com.electriccloud.procedures.KubernetesTestBase
import io.qameta.allure.*
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import static com.electriccloud.helpers.enums.LogLevels.*
import static com.electriccloud.helpers.enums.ServiceTypes.*


@Feature('Configuration')
class EditConfigurationTests extends KubernetesTestBase {


    @BeforeClass
    void setUpTests(){
        k8sClient.createConfiguration(configName, clusterEndpoint, 'flowqe', clusterToken, clusterVersion)
    }

    @AfterClass
    void tearDownTests(){
        k8sClient.deleteConfiguration(configName)
    }


    @Test
    @TmsLink("324797")
    @Story("Edit Configuration")
    @Description("Edit Configuration description, endpoint, token, debug level, version")
    void editConfiguration(){
        def resp = k8sClient.editConfiguration(clusterEndpoint, 'ecadmin', clusterToken, clusterVersion, true, "/api/v1/namespaces", LogLevel.DEBUG)
        String logs = k8sClient.client.getJobLogs(resp.json.jobId)
        def jobStatus = k8sClient.client.getJobStatus(resp.json.jobId).json
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert logs.contains("Kubernetes cluster is reachable at ${clusterEndpoint}.")
    }


    @Test
    @TmsLink("324804")
    @Story("Edit Configuration")
    @Description("Edit Configuration without cluster test connection")
    void editConfigurationWithoutTestConnection(){
        def resp = k8sClient.editConfiguration(clusterEndpoint, 'ecadmin', clusterToken, clusterVersion, false, "", LogLevel.DEBUG)
        def jobStatus = k8sClient.client.getJobStatus(resp.json.jobId).json
        def jobSteps = k8sClient.client.getJobSteps(resp.json.jobId).json.object
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert jobSteps[0].jobStep.combinedStatus.status == "skipped"
        assert jobSteps[1].jobStep.combinedStatus.status == "skipped"
    }


    @Test
    @TmsLink("324798")
    @Story("Edit Configuration")
    @Description("Edit Configuration with test cluster connection")
    void editConfigurationWithTestConnection(){
        def resp = k8sClient.editConfiguration(clusterEndpoint, 'ecadmin', clusterToken, clusterVersion, true, '/api/v1/namespaces', LogLevel.DEBUG)
        String logs = k8sClient.client.getJobLogs(resp.json.jobId)
        def jobStatus = k8sClient.client.getJobStatus(resp.json.jobId).json
        def jobSteps = k8sClient.client.getJobSteps(resp.json.jobId).json.object
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert logs.contains("Kubernetes cluster is reachable at ${clusterEndpoint}.")
        assert jobSteps[0].jobStep.combinedStatus.status == "completed_success"
        assert jobSteps[1].jobStep.combinedStatus.status == "completed_success"
    }


    @Test(dataProvider = "invalidData")
    @Issue("ECKUBE-180")
    @TmsLinks(value = [@TmsLink("324799"), @TmsLink("324800"), @TmsLink("324801"), @TmsLink("324802"), @TmsLink("324803")])
    @Story("Invalid configuration")
    @Description("Unable to Edit Configuration invalid data")
    void unnableEditConfigurationWithInvalidData(endpoint, username, token, version, testConnection, testConnectionUri, logLevel, errorMessage){
        def jobStatus = null
        String logs = " "
        try {
            k8sClient.editConfiguration(endpoint, username, token, version, testConnection, testConnectionUri, logLevel)
        } catch (e){
            def jobId = e.cause.message
            jobStatus = k8sClient.client.getJobStatus(jobId).json
            logs = k8sClient.client.getJobLogs(jobId)
        } finally {
            assert logs.contains(errorMessage), 'The Procedure passed with invalid credentials!!!'
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
        }
    }


    @DataProvider(name = "invalidData")
    def getInvalidData(){
        return [
                [clusterEndpoint, "flowqe", clusterToken, clusterVersion, true, "/api/v1/test", LogLevel.DEBUG, "ERROR: Kubernetes cluster at ${clusterEndpoint} was not reachable. Health check (#2) at ${clusterEndpoint}/api/v1/test failed with HTTP/1.1 404 Not Found"],
                ["", "flowqe", clusterToken, clusterVersion, true, '/api/v1/namespaces', LogLevel.DEBUG, 'java.lang.IllegalStateException: Target host is null'],
                ["https://35.188.101.83", "flowqe", clusterToken, clusterVersion, true, '/api/v1/namespaces', LogLevel.DEBUG, 'java.net.ConnectException: Connection timed out (Connection timed out)'],
                [clusterEndpoint, "", "", clusterVersion, true, '/api/v1/namespaces', LogLevel.DEBUG, "ERROR: Kubernetes cluster at ${clusterEndpoint} was not reachable. Health check (#2) at ${clusterEndpoint}/api/v1/namespaces failed with HTTP/1.1 403 Forbidden"],
                [clusterEndpoint, "flowqe", "test", clusterVersion, true, '/api/v1/namespaces', LogLevel.DEBUG, "Kubernetes cluster at ${clusterEndpoint} was not reachable."]
        ] as Object[][]
    }





}
