package com.electriccloud.procedures.configuration

import com.electriccloud.procedures.KubernetesTestBase
import com.electriccloud.test_data.ConfigurationData
import io.qameta.allure.*
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import static com.electriccloud.helpers.enums.LogLevels.*
import static com.electriccloud.helpers.enums.ServiceTypes.*
import static org.awaitility.Awaitility.*;


@Feature("Configuration")
class CreateConfigurationTests extends KubernetesTestBase {


    @BeforeClass
    void setUpTests(){
        k8sClient.deleteConfiguration(configName)
    }


    @AfterMethod
    void tearDownTest(){
        k8sClient.deleteConfiguration(configName)
        k8sClient.client.deleteProject(projectName)
    }


    @Test(dataProvider = "clusterVersions", dataProviderClass = ConfigurationData.class)
    @TmsLinks(value = [
            @TmsLink("324777"),
            @TmsLink("324778"),
            @TmsLink("324779"),
            @TmsLink("324780"),
            @TmsLink("324781"),
            @TmsLink("324782")
    ])
    @Story("Create Configuration for all cluster versions")
    @Description("Create Configuration for all cluster versions")
    void createConfigurationForDifferentVersions(version){
        def job = k8sClient.createConfiguration(configName, clusterEndpoint, adminAccount, clusterToken, version)
        def logs = k8sClient.client.getJobLogs(job.json.jobId)
        def jobStatus = k8sClient.client.getJobStatus(job.json.jobId).json
        assert job.resp.statusLine.toString().contains("200 OK")
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert logs.contains("Attaching credential to procedure Wait For Kubernetes API at step waitAPI")
    }



    @Test
    @TmsLink("324783")
    @Story("Create Configuration without test connection")
    @Description("Create configuration without cluster test connection ")
    void createConfigurationWithoutTestConnection(){
        def job = k8sClient.createConfiguration(configName, clusterEndpoint, 'flowqe', clusterToken, clusterVersion, false, clusterEndpoint)
        def logs = k8sClient.client.getJobLogs(job.json.jobId)
        def jobStatus = k8sClient.client.getJobStatus(job.json.jobId).json
        def jobSteps = k8sClient.client.getJobSteps(job.json.jobId).json.object
        assert job.resp.statusLine.toString().contains("200 OK")
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert logs.contains("Attaching credential to procedure Wait For Kubernetes API at step waitAPI")
        assert jobSteps[0].jobStep.combinedStatus.status == "skipped"
        assert jobSteps[1].jobStep.combinedStatus.status == "skipped"
    }



    @Test
    @TmsLink("324783")
    @Story("Create Configuration with test connection")
    @Description("Create Configuration with cluster test connection")
    void createConfigurationWithTestConnection(){
        def job = k8sClient.createConfiguration(configName, clusterEndpoint, 'flowqe', clusterToken, clusterVersion, true, '/api/v1/namespaces')
        def logs = k8sClient.client.getJobLogs(job.json.jobId)
        def jobStatus = k8sClient.client.getJobStatus(job.json.jobId).json
        def jobSteps = k8sClient.client.getJobSteps(job.json.jobId).json.object
        assert job.resp.statusLine.toString().contains("200 OK")
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert logs.contains("Attaching credential to procedure Wait For Kubernetes API at step waitAPI")
        assert jobSteps[0].jobStep.combinedStatus.status == "completed_success"
        assert jobSteps[1].jobStep.combinedStatus.status == "completed_success"
    }


    @Test(dataProvider = "logLevels", dataProviderClass = ConfigurationData.class)
    @TmsLinks(value = [
            @TmsLink("324785"),
            @TmsLink("324786"),
            @TmsLink("324787"),
            @TmsLink("324788")
    ])
    @Story("Log Level Configuration")
    @Description("Create Configuration for different log Levels")
    void createConfigurationForDifferentLogLevels(logLevel, message, desiredLog, missingLog){
        def job = k8sClient.createConfiguration(configName, clusterEndpoint, adminAccount, clusterToken, clusterVersion, true, '/api/v1/namespaces', logLevel)
        k8sClient.createEnvironment(configName)
        def resp = k8sClient.provisionEnvironment(projectName, environmentName, clusterName)
        def jobStatus = k8sClient.client.getJobStatus(job.json.jobId).json
        def jobSteps = k8sClient.client.getJobSteps(job.json.jobId).json.object
        def logs = k8sClient.client.getJobLogs(resp.json.jobId)
        assert job.resp.statusLine.toString().contains("200 OK")
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert jobSteps[2].jobStep.command.contains(message)
        assert logs.contains(desiredLog)
        assert !logs.contains(missingLog)
    }


    @Test
    @TmsLink("324796")
    @Story("Invalid configuration")
    @Description("Unable to create configuration that already exist")
    void unableToCreateExistingConfiguration(){
        try {
            k8sClient.createConfiguration(configName, clusterEndpoint, adminAccount, clusterToken, clusterVersion)
            k8sClient.createConfiguration(configName, clusterEndpoint, adminAccount, clusterToken, clusterVersion)
        } catch (e){
            def jobId = e.cause.message
            await().until { k8sClient.client.getJobStatus(jobId).json.status == "completed" }
            def jobStatus = k8sClient.client.getJobStatus(jobId).json
            String logs = k8sClient.client.getJobLogs(jobId)
            assert logs.contains("A configuration named '${configName}' already exists.")
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
        }
    }


    @Test(dataProvider = "invalidConfigData", dataProviderClass = ConfigurationData.class)
    @Issue("ECKUBE-180")
    @TmsLinks(value = [
            @TmsLink("324789"),
            @TmsLink("324790"),
            @TmsLink("324791"),
            @TmsLink("324792"),
            @TmsLink("324793"),
            @TmsLink("324794"),
            @TmsLink("324795")
    ])
    @Story("Invalid configuration")
    @Description("Unable to configure with invalid data")
    void unableToConfigureWithInvalidData(configName, endpoint, username, token, version, testConnection, testConnectionUri, logLevel, errorMessage){
        def jobStatus = null
        String logs = " "
        try {
            k8sClient.createConfiguration(configName, endpoint, username, token, version, testConnection, testConnectionUri, logLevel)
        } catch (e){
            def jobId = e.cause.message
            await().until { k8sClient.client.getJobStatus(jobId).json.status == "completed" }
            jobStatus = k8sClient.client.getJobStatus(jobId).json
            logs = k8sClient.client.getJobLogs(jobId)
        } finally {
            assert logs.contains(errorMessage), 'The Procedure passed with invalid credentials!!!'
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
        }
    }





}
