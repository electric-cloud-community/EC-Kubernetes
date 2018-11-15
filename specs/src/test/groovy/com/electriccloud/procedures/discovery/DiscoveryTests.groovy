package com.electriccloud.procedures.discovery

import com.electriccloud.procedures.KubernetesTestBase
import com.electriccloud.test_data.DiscoveryData
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.qameta.allure.Story
import io.qameta.allure.TmsLink
import org.testng.annotations.*
import static com.electriccloud.models.enums.ServiceTypes.ServiceType.*
import static org.awaitility.Awaitility.await


@Feature('Discovery')
class DiscoveryTests extends KubernetesTestBase {

    @BeforeClass(alwaysRun = true)
    void setUpTests(){
        k8sClient.deleteConfiguration(configName)
        k8sClient.createConfiguration(configName, clusterEndpoint, adminAccount, clusterToken, clusterVersion)
        k8sClient.createEnvironment(configName)
        k8sClient.createService(2, volumes, false)
        k8sClient.deployService(projectName, serviceName)
    }

    @BeforeMethod(alwaysRun = true)
    void setUpTest(){
        k8sClient.client.deleteService(projectName, serviceName)
        k8sClient.client.deleteApplication(projectName, applicationName)
    }

    @AfterMethod(alwaysRun = true)
    void tearDownTest(){
        k8sClient.client.deleteService(projectName, serviceName).deleteApplication(projectName, applicationName)
        k8sClient.client.deleteProject('MyProject')
    }


    @AfterClass(alwaysRun = true)
    void tearDownTests(){
        k8sClient.deleteConfiguration(configName)
        k8sClient.createConfiguration(configName, clusterEndpoint, adminAccount, clusterToken, clusterVersion)
        k8sClient.cleanUpCluster(configName)
        k8sClient.client.deleteProject(projectName)
    }




    @Test(groups = "Positive", testName = "Discover Project-level Microservice")
    @TmsLink("")
    @Story("Microservice discovery")
    @Description("Discover Project-level Microservice")
    void discoverProjectLevelMicroservice() {
        def jobId = k8sClient.discoverService(projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                'default',
                null,
                null,
                false, null).json.jobId
        def jobLog = k8sClient.client.getJobLogs(jobId)
        def services = k8sClient.client.getServices(projectName).json.service
        def service = k8sClient.client.getService(projectName, serviceName).json.service
        def container = k8sClient.client.getServiceContainer(projectName, serviceName, containerName).json.container
        def mapping = k8sClient.getServiceMappings(projectName, serviceName)[0].serviceClusterMappings.serviceClusterMapping[0]
        assert services.size() == 1
        assert service.serviceName == serviceName
        assert service.containerCount == '1'
        assert service.environmentMapCount == '1'
        assert service.defaultCapacity == '2'
        assert service.projectName == projectName
        assert service.volume == "[{\"name\":\"html-content\",\"hostPath\":\"/var/html\"}]"
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert mapping.actualParameters.parameterDetail[0].parameterName == "serviceType"
        assert mapping.actualParameters.parameterDetail[0].parameterValue == LOAD_BALANCER.value
        assert !jobLog.contains(clusterToken)
    }



    @Test(groups = "Positive", testName = "Discover Microservice with new environment")
    @TmsLink("")
    @Story("Microservice discovery")
    @Description("Discover Project-level Microservice with environment generation")
    void discoverProjectLevelMicroserviceWithEnvironmentGeneration() {
        def jobId = k8sClient.discoverService(projectName,
                environmentProjectName,
                'my-environment',
                clusterName,
                'default',
                clusterEndpoint,
                clusterToken,
                false, null).json.jobId
        def jobLog = k8sClient.client.getJobLogs(jobId)
        def services = k8sClient.client.getServices(projectName).json.service
        def service = k8sClient.client.getService(projectName, serviceName).json.service
        def environment = k8sClient.client.getEnvironment(projectName, 'my-environment').json.environment
        def container = k8sClient.client.getServiceContainer(projectName, serviceName, containerName).json.container
        def mappings = k8sClient.getServiceMappings(projectName, serviceName)
        def mapping = mappings[0].serviceClusterMappings.serviceClusterMapping[0]
        assert services.size() == 1
        assert service.serviceName == serviceName
        assert service.containerCount == '1'
        assert service.environmentMapCount == '1'
        assert service.defaultCapacity == '2'
        assert service.projectName == projectName
        assert service.volume == "[{\"name\":\"html-content\",\"hostPath\":\"/var/html\"}]"
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert environment.environmentName == 'my-environment'
        assert environment.environmentEnabled == '1'
        assert environment.projectName == projectName
        assert mappings.size() == 1
        assert mappings[0].environmentName == "my-environment"
        assert mapping.actualParameters.parameterDetail[0].parameterName == "serviceType"
        assert mapping.actualParameters.parameterDetail[0].parameterValue == LOAD_BALANCER.value
        assert !jobLog.contains(clusterToken)
    }



    @Test(groups = "Positive", testName = "Discover Microservice with new project")
    @TmsLink("")
    @Story("Microservice discovery")
    @Description("Discover Project-level Microservice with project generation ")
    void discoverProjectLevelMicroserviceWithProjectGeneration() {
        def jobId = k8sClient.discoverService(projectName, 'MyProject',
                environmentName,
                clusterName,
                'default',
                clusterEndpoint,
                clusterToken,
                false, null).json.jobId
        def jobLog = k8sClient.client.getJobLogs(jobId)
        def service = k8sClient.client.getService(projectName, serviceName).json.service
        def environment = k8sClient.client.getEnvironment("MyProject", environmentName).json.environment
        def container = k8sClient.client.getServiceContainer(projectName, serviceName, containerName).json.container
        def mappings = k8sClient.getServiceMappings(projectName, serviceName)
        def mapping = mappings[0].serviceClusterMappings.serviceClusterMapping[0]
        assert service.serviceName == serviceName
        assert service.containerCount == '1'
        assert service.environmentMapCount == '1'
        assert service.defaultCapacity == '2'
        assert service.projectName == projectName
        assert service.volume == "[{\"name\":\"html-content\",\"hostPath\":\"/var/html\"}]"
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert environment.environmentName == environmentName
        assert environment.environmentEnabled == '1'
        assert environment.projectName == 'MyProject'
        assert mappings.size() == 1
        assert mappings[0].environmentName == environmentName
        assert mapping.actualParameters.parameterDetail[0].parameterName == "serviceType"
        assert mapping.actualParameters.parameterDetail[0].parameterValue == LOAD_BALANCER.value
        assert !jobLog.contains(clusterToken)
    }







    @Test(groups = "Positive", testName = "Discover Application-level Microservice")
    @TmsLink("")
    @Story("Application discovery")
    @Description("Discover Application-level Microservice")
    void discoverApplicationWithMicroservice() {
        def jobId = k8sClient.discoverService(projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                'default',
                null,
                null,
                true, applicationName).json.jobId
        def jobLog = k8sClient.client.getJobLogs(jobId)
        def applications = k8sClient.client.getApplications(projectName).json.application
        def application = k8sClient.client.getApplication(projectName, applicationName).json.application
        def container = k8sClient.client.getApplicationContainer(projectName, applicationName, serviceName, containerName).json.container
        def mapping = k8sClient.getAppMappings(projectName, applicationName)[0].serviceClusterMappings.serviceClusterMapping[0]
        assert applications.size() == 1
        assert application.applicationName == applicationName
        assert application.containerCount == '1'
        assert application.description == 'Created by EF Discovery'
        assert application.projectName == projectName
        assert application.tiermapCount == '1'
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert mapping.actualParameters.parameterDetail[0].parameterName == "serviceType"
        assert mapping.actualParameters.parameterDetail[0].parameterValue == LOAD_BALANCER.value
        assert !jobLog.contains(clusterToken)

    }





    @Test(groups = "Positive", testName = "Discover Application with new environment")
    @TmsLink("")
    @Story("Application discovery")
    @Description("Discover Application-level Microservice with environment generation")
    void discoverApplicationLevelMicroserviceWithEnvironmentGeneration() {
        def jobId = k8sClient.discoverService(projectName, projectName,
                'my-environment',
                clusterName,
                'default',
                clusterEndpoint,
                clusterToken,
                true, applicationName).json.jobId
        def jobLog = k8sClient.client.getJobLogs(jobId)
        def applications = k8sClient.client.getApplications(projectName).json.application
        def application = k8sClient.client.getApplication(projectName, applicationName).json.application
        def environment = k8sClient.client.getEnvironment(projectName, 'my-environment').json.environment
        def container = k8sClient.client.getApplicationContainer(projectName, applicationName, serviceName, containerName).json.container
        def mappings = k8sClient.getAppMappings(projectName, applicationName)
        def mapping = mappings[0].serviceClusterMappings.serviceClusterMapping[0]
        assert applications.size() == 1
        assert application.applicationName == applicationName
        assert application.containerCount == '1'
        assert application.description == 'Created by EF Discovery'
        assert application.projectName == projectName
        assert application.tiermapCount == '1'
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert environment.environmentName == 'my-environment'
        assert environment.environmentEnabled == '1'
        assert environment.projectName == projectName
        assert mappings.size() == 1
        assert mappings[0].environmentName == "my-environment"
        assert mapping.actualParameters.parameterDetail[0].parameterName == "serviceType"
        assert mapping.actualParameters.parameterDetail[0].parameterValue == LOAD_BALANCER.value
        assert !jobLog.contains(clusterToken)
    }



    @Test(groups = "Positive", testName = "Discover Application with new project")
    @TmsLink("363540")
    @Story("Microservice discovery")
    @Description(" Discover Application-level Microservice with project generation")
    void discoverApplicationLevelMicroserviceWithProjectGeneration(){
        def jobId = k8sClient.discoverService(projectName, 'MyProject',
                environmentName,
                clusterName,
                'default',
                clusterEndpoint,
                clusterToken,
                true, applicationName).json.jobId
        def jobLog = k8sClient.client.getJobLogs(jobId)
        def application = k8sClient.client.getApplication(projectName, applicationName).json.application
        def environment = k8sClient.client.getEnvironment('MyProject', environmentName).json.environment
        def container = k8sClient.client.getApplicationContainer(projectName, applicationName, serviceName, containerName).json.container
        def mappings = k8sClient.getAppMappings(projectName, applicationName)
        def mapping = mappings[0].serviceClusterMappings.serviceClusterMapping[0]
        assert application.applicationName == applicationName
        assert application.containerCount == '1'
        assert application.description == 'Created by EF Discovery'
        assert application.projectName == projectName
        assert application.tiermapCount == '1'
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert environment.environmentName == environmentName
        assert environment.environmentEnabled == '1'
        assert environment.projectName == 'MyProject'
        assert mappings.size() == 1
        assert mappings[0].environmentName == environmentName
        assert mapping.actualParameters.parameterDetail[0].parameterName == "serviceType"
        assert mapping.actualParameters.parameterDetail[0].parameterValue == LOAD_BALANCER.value
        assert !jobLog.contains(clusterToken)
    }





    @Test(groups = "Nagative", testName = "Discover existing Project-level Microservice")
    @TmsLink("")
    @Story("Invalid Microservice discovery")
    @Description("Unable to discover Project-level Microservice that already exist")
    void unableToDiscoverExistingProjectLevelMicroservice() {
        try {
            k8sClient.discoverService(projectName,
                    environmentProjectName,
                    environmentName,
                    clusterName,
                    'default',
                    clusterEndpoint,
                    clusterToken,
                    false, null)
            k8sClient.discoverService(projectName,
                    environmentProjectName,
                    environmentName,
                    clusterName,
                    'default',
                    clusterEndpoint,
                    clusterToken,
                    false, null)
        } catch (e){
            def jobId = e.cause.message
            await('Job to be completed').until { k8sClient.client.getJobStatus(jobId).json.status == "completed" }
            String jobLog = k8sClient.client.getJobLogs(jobId)
            def jobStatus = k8sClient.client.getJobStatus(jobId).json
            assert jobLog.contains("Service ${serviceName} already exists")
            assert jobLog.contains("Container ${containerName} already exists")
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
            assert !jobLog.contains(clusterToken)
        }
    }


    @Test(groups = "Negative")
    @TmsLink("324444")
    @Story("Invalid Application discovery")
    @Description("Unable to Discover Application-level Microservice that already exist")
    void unableToDiscoverExistingApplication() {
        try {
            k8sClient.discoverService(projectName,
                    environmentProjectName,
                    environmentName,
                    clusterName,
                    'default',
                    clusterEndpoint,
                    clusterToken,
                    true, applicationName)
            k8sClient.discoverService(projectName,
                    environmentProjectName,
                    environmentName,
                    clusterName,
                    'default',
                    clusterEndpoint,
                    clusterToken,
                    true, applicationName)
        } catch (e){
            def jobId = e.cause.message
            await('Job to be completed').until { k8sClient.client.getJobStatus(jobId).json.status == "completed" }
            String jobLog = k8sClient.client.getJobLogs(jobId)
            def jobStatus = k8sClient.client.getJobStatus(jobId).json
            assert jobLog.contains("Application ${applicationName} already exists in project ${projectName}")
            assert jobLog.contains("Process \'Deploy\' already exists in application \'${applicationName}\'")
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
            assert !jobLog.contains(clusterToken)
        }
    }



    @Test(groups = "Negative", testName = "Unable to Discover with invalid namespace")
    @TmsLink("")
    @Story("Invalid Microservice discovery")
    @Description("Unable to discover Project-level Microservice with invalid Namespace ")
    void unableToDiscoverMicroserviceWithInvalidNamespace(){
        def jobId = k8sClient.discoverService(projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                'my-namespace',
                clusterEndpoint,
                clusterToken,
                false, null).json.jobId
        await('Job to be completed').until { k8sClient.client.getJobStatus(jobId).json.status == "completed" }
        def jobStatus = k8sClient.client.getJobStatus(jobId).json
        def jobLog = k8sClient.client.getJobLogs(jobId)
        assert jobStatus.outcome == "warning"
        assert jobStatus.status == "completed"
        assert jobLog.contains("No services found on the cluster ${clusterEndpoint}")
        assert jobLog.contains("Discovered services: 0")
        assert !jobLog.contains(clusterToken)
    }



    @Test(groups = "Negative", priority = 1, testName = "Discover Microservice with invalid data")
    @TmsLink("")
    @Story("Invalid Microservice discovery")
    @Description("Unable to Discover Microservice without plugin configuration")
    void unableToDiscoverMicroserviceWithoutConfiguration() {
        try {
            k8sClient.deleteConfiguration(configName)
            k8sClient.discoverService(projectName,
                    environmentProjectName,
                    environmentName,
                    clusterName,
                    'default',
                    clusterEndpoint,
                    clusterToken,
                    false, null)
        } catch (e) {
            def jobId = e.cause.message
            await('Job to be completed').until {
                k8sClient.client.getJobStatus(jobId).json.status == "completed"
            }
            String jobLog = k8sClient.client.getJobLogs(jobId)
            def jobStatus = k8sClient.client.getJobStatus(jobId).json
            assert jobLog.contains("Configuration ${configName} does not exist!")
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
            assert !jobLog.contains(clusterToken)
        }
    }





    @Test(groups = "Negative", dataProvider = 'invalidDiscoveryData', dataProviderClass = DiscoveryData.class)
    @Story('Preform invalid discovery')
    void invalidDiscoveryProcedure(project, envProject, envName, clusterName, namespace, errorMessage){
        try {
            k8sClient.discoverService(project, envProject,
                    envName,
                    clusterName,
                    namespace,
                    clusterEndpoint,
                    clusterToken,
                    false, null)
        } catch (e){
            def jobId = e.cause.message
            await('Job to be completed').until { k8sClient.client.getJobStatus(jobId).json.status == "completed" }
            String jobLog = k8sClient.client.getJobLogs(jobId)
            def jobStatus = k8sClient.client.getJobStatus(jobId).json
            assert jobLog.contains(errorMessage)
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
            assert !jobLog.contains(clusterToken)

        }
    }






}
