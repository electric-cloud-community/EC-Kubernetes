package com.electriccloud.procedures.import_ms

import com.electriccloud.procedures.KubernetesTestBase
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.qameta.allure.Story
import io.qameta.allure.TmsLink
import io.qameta.allure.TmsLinks
import org.testng.annotations.*

import static com.electriccloud.helpers.enums.LogLevels.*


@Feature('Import')
class ImportTests extends KubernetesTestBase {


    @BeforeClass
    void setUpTests() {
        k8sClient.deleteConfiguration(configName)
        k8sClient.createConfiguration(configName, clusterEndpoint, 'ecloud', clusterToken, clusterVersion, true, 'apis', LogLevel.DEBUG)
        k8sClient.createEnvironment(configName)
    }

    @AfterClass
    void tearDownTests() {
        k8sClient.client.deleteProject(projectName)
        k8sClient.deleteConfiguration(configName)
    }


    @BeforeMethod
    void setUpTest(){
        k8sClient.client.deleteApplication(projectName, applicationName)
        k8sClient.client.deleteService(projectName, serviceName)
    }



    @AfterMethod
    void tearDownTest() {
        k8sClient.client.deleteApplication(projectName, applicationName)
        k8sClient.client.deleteService(projectName, serviceName)
    }



    @Test(testName = "Import Project-level Microservice")
    @TmsLink("")
    @Story('Import microservice')
    @Description("Import Project-level Microservice")
    void importProjectLevelMicroservice() {
        k8sClient.importService(serviceName,
                projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                false, null)
        def services = k8sClient.client.getServices(projectName).json.service
        def service = k8sClient.client.getService(projectName, serviceName).json.service
        def container = k8sClient.client.getServiceContainer(projectName, serviceName, containerName).json.container
        def mappings = k8sClient.getServiceMappings(projectName, serviceName)
        assert services.size() == 1
        assert service.serviceName == serviceName
        assert service.containerCount == '1'
        assert service.environmentMapCount == '1'
        assert service.defaultCapacity == '2'
        assert service.projectName == projectName
        assert service.volume == "[{\"name\":\"html\",\"hostPath\":\"/var/html\"}]"
        assert container.containerName == containerName
        assert container.imageName == "nginx"
        assert container.imageVersion == "stable"
        assert container.volumeMount == "[{\"name\":\"html\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert mappings.size() == 1
        assert mappings[0].environmentName == environmentName
    }



    @Test(testName = "Import Project-level Microservice without mapping")
    @TmsLink("")
    @Story('Import microservice')
    @Description("Import Project-level Microservice without environment mapping")
    void importMicroserviceWithoutEnvironmentMapping() {
        k8sClient.importService(serviceName,
                projectName,
                null,
                null,
                null,
                false, null)
        def services = k8sClient.client.getServices(projectName).json.service
        def service = k8sClient.client.getService(projectName, serviceName).json.service
        def container = k8sClient.client.getServiceContainer(projectName, serviceName, containerName).json.container
        def mappings = k8sClient.getServiceMappings(projectName, serviceName)
        assert services.size() == 1
        assert service.serviceName == serviceName
        assert service.containerCount == '1'
        assert service.environmentMapCount == '0'
        assert service.defaultCapacity == '2'
        assert service.projectName == projectName
        assert service.volume == "[{\"name\":\"html\",\"hostPath\":\"/var/html\"}]"
        assert container.containerName == containerName
        assert container.imageName == "nginx"
        assert container.imageVersion == "stable"
        assert container.volumeMount == "[{\"name\":\"html\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert mappings == null
    }


    @Test(testName = "Import Application-level Microservice")
    @TmsLink("363503")
    @Story('Import microservice')
    @Description("Import Application-level Microservice")
    void applicationImport(){
        k8sClient.importService(serviceName,
                projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                true, applicationName)
        def apps = k8sClient.client.getApplications(projectName).json.application
        def app = k8sClient.client.getApplication(projectName, applicationName).json.application
        def container = k8sClient.client.getApplicationContainer(projectName, applicationName, serviceName, containerName).json.container
        def mappings = k8sClient.getAppMappings(projectName, applicationName)
        assert apps.size() == 1
        assert app.applicationName == applicationName
        assert app.containerCount == "1"
        assert app.projectName == projectName
        assert container.containerName == containerName
        assert container.imageName == "nginx"
        assert container.imageVersion == "stable"
        assert container.volumeMount == "[{\"name\":\"html\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert mappings.size() == 1
        assert mappings[0].environmentName == environmentName
    }



    @Test(testName = "Import Application-level Microservice without mapping")
    @TmsLink("")
    @Story('Import microservice')
    @Description("Import Application-level Microservice without environment mapping")
    void applicationImportWithoutEnvironmentMapping(){
        k8sClient.importService(serviceName,
                projectName,
                null,
                null,
                null,
                true, applicationName)
        def apps = k8sClient.client.getApplications(projectName).json.application
        def app = k8sClient.client.getApplication(projectName, applicationName).json.application
        def container = k8sClient.client.getApplicationContainer(projectName, applicationName, serviceName, containerName).json.container
        def mappings = k8sClient.getAppMappings(projectName, applicationName)
        assert apps.size() == 1
        assert app.applicationName == applicationName
        assert app.containerCount == "1"
        assert app.envTemplateTierMapCount == '0'
        assert app.projectName == projectName
        assert container.containerName == containerName
        assert container.imageName == "nginx"
        assert container.imageVersion == "stable"
        assert container.volumeMount == "[{\"name\":\"html\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert mappings == null
    }




    @Test(testName = "Import existing Project-level Microservice")
    @TmsLink("363471")
    @Story('Import with invalid data')
    @Description("Unable to import Project-level Microservice that already exist")
    void importExistingProjectLevelMicroservice(){
        k8sClient.importService(serviceName,
                projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                false, null)
        def jobId = k8sClient.importService(serviceName,
                projectName,
                projectName,
                environmentName,
                clusterName,
                false, null).json.jobId
        def services = k8sClient.client.getServices(projectName).json.service
        def service = k8sClient.client.getService(projectName, 'nginx-service').json.service
        def container = k8sClient.client.getServiceContainer(projectName, serviceName, containerName).json.container
        String jobLogs = k8sClient.client.getJobLogs(jobId)
        def jobStatus = k8sClient.client.getJobStatus(jobId).json
        assert services.size() == 1
        assert service.serviceName == serviceName
        assert service.containerCount == '1'
        assert service.environmentMapCount == '1'
        assert service.defaultCapacity == '2'
        assert service.projectName == projectName
        assert service.volume == "[{\"name\":\"html\",\"hostPath\":\"/var/html\"}]"
        assert container.containerName == containerName
        assert container.imageName == "nginx"
        assert container.imageVersion == "stable"
        assert container.volumeMount == "[{\"name\":\"html\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert jobStatus.status == "completed"
        assert jobStatus.outcome == "warning"
        assert jobLogs.contains("Service ${serviceName} already exists, skipping")
    }



    @Test(testName = "Import existing Application-level Microservice")
    @TmsLink("363472")
    @Story('Import with invalid data')
    @Description("Unable to import Application-level Microservice that already exist")
    void importExistingApplicationLevelMicroservice(){
        k8sClient.importService(serviceName,
                projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                true, applicationName)
        def jobId = k8sClient.importService(serviceName,
                projectName,
                projectName,
                environmentName,
                clusterName,
                true, applicationName).json.jobId
        def services = k8sClient.client.getApplicationServices(projectName, applicationName).json.service
        def service = k8sClient.client.getApplicationService(projectName, applicationName, serviceName).json.service
        def container = k8sClient.client.getApplicationContainer(projectName, applicationName, serviceName, containerName).json.container
        String jobLogs = k8sClient.client.getJobLogs(jobId)
        def jobStatus = k8sClient.client.getJobStatus(jobId).json
        assert services.size() == 1
        assert service.serviceName == serviceName
        assert service.containerCount == '1'
        assert service.defaultCapacity == '2'
        assert service.projectName == projectName
        assert service.volume == "[{\"name\":\"html\",\"hostPath\":\"/var/html\"}]"
        assert container.containerName == containerName
        assert container.imageName == "nginx"
        assert container.imageVersion == "stable"
        assert container.volumeMount == "[{\"name\":\"html\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert jobStatus.status == "completed"
        assert jobStatus.outcome == "warning"
        assert jobLogs.contains("Service ${serviceName} already exists, skipping")
    }



    @Test(dataProvider = 'importData', testName = "Import with invalid data")
    @TmsLinks([ @TmsLink("363509"),
            @TmsLink("363508"),
            @TmsLink("363507"),
            @TmsLink("363474"),
            @TmsLink("363473"),
            @TmsLink("279011"),
            @TmsLink("279010"),
            @TmsLink("279008"), @TmsLink("279013"), @TmsLink("279003")])
    @Story('Import with invalid parameter')
    @Description("Unable to import Microservice with invalid data")
    void invalidServiceImport(yamlFile, project, envName, clusterName, isApp, appName, errorMessage){
        try {
            k8sClient.importService(yamlFile, project, project,  envName, clusterName, isApp, appName)
        } catch (e){
            def jobId = e.cause.message
            String errorLog = k8sClient.client.getJobLogs(jobId)
            def jobStatus = k8sClient.client.getJobStatus(jobId).json
            assert errorLog.contains(errorMessage)
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
        }
    }



    @DataProvider(name = "importData")
    Object[][] getImportData(){
        def data = [
                [serviceName, projectName, environmentName, "", false, null, "Either specify all the parameters required to identify the Kubernetes-backed ElectricFlow cluster"],
                [serviceName, projectName, environmentName, "my-cluster", false, null, "Cluster \'my-cluster\' does not exist in \'${environmentName}\' environment!"],
                [serviceName, projectName, "", clusterName, false, null,  "Either specify all the parameters required to identify the Kubernetes-backed ElectricFlow cluster"],
                [serviceName, projectName, "my-environment", clusterName, false, null, "Environment \'my-environment\' does not exist in project \'${projectName}\'"],
                [serviceName, "Default", environmentName, clusterName,  false, null,  "Environment \'${environmentName}\' does not exist in project \'Default\'"],
                ["nginx-service-invalid", projectName, environmentName, clusterName, false, null,  "ERROR: Failed to read the Docker Compose file contents"],
                [applicationName,projectName, environmentName, "",  true, applicationName,  "Either specify all the parameters required to identify the Kubernetes-backed ElectricFlow cluster"],
                [applicationName,projectName, environmentName, "my-cluster",  true, applicationName,  "Cluster \'my-cluster\' does not exist in \'${environmentName}\' environment!"],
                [applicationName, projectName, "", clusterName,  true, applicationName,  "Either specify all the parameters required to identify the Kubernetes-backed ElectricFlow cluster"],
                [applicationName, projectName, "my-environment", clusterName, true, applicationName,  "Environment \'my-environment\' does not exist in project \'${projectName}\'"],
                [applicationName, "Default", environmentName, clusterName, true, applicationName,  "Environment \'${environmentName}\' does not exist in project \'Default\'"],
                ["nginx-service-invalid", projectName, environmentName, clusterName, true, applicationName,  "ERROR: Failed to read the Docker Compose file contents"],
                [applicationName, projectName, environmentName, clusterName, true, "", "Application name is required for creating application-scoped microservices"]
        ]
        return data as Object[][]
    }


}
