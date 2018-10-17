package com.electriccloud.procedures.deployment

import com.electriccloud.procedures.KubernetesTestBase
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.qameta.allure.Flaky
import io.qameta.allure.Story
import io.qameta.allure.TmsLink
import org.testng.annotations.*

import java.util.concurrent.TimeUnit

import static com.electriccloud.helpers.enums.LogLevels.*
import static com.electriccloud.helpers.enums.ServiceTypes.*
import static com.electriccloud.helpers.enums.ServiceTypes.ServiceType.*
import static org.awaitility.Awaitility.await

@Feature('Deployment')
class MicroserviceDeploymentTests extends KubernetesTestBase {


    @BeforeClass
    void setUpDeployment() {
        k8sClient.deleteConfiguration(configName)
        k8sClient.createConfiguration(configName, clusterEndpoint, adminAccount, clusterToken, clusterVersion)
    }

    @AfterClass
    void tearDownTests() {
        k8sClient.deleteConfiguration(configName)
    }

    @BeforeMethod
    void setUpTest(){
        k8sClient.createEnvironment(configName)
        k8sClient.createService(2, volumes, false)
    }

    @AfterMethod
    void tearDownTest() {
        k8sClient.cleanUpCluster(configName)
        await().atMost(50, TimeUnit.SECONDS).until { k8sApi.getPods().json.items.size() == 0 }
        k8sClient.client.deleteProject(projectName)
    }


    @Test(testName = "Deploy Project-Level Microservice")
    @TmsLink("")
    @Story("Deploy Microservcice")
    @Description("Deploy Project-Level Microservice")
    void deployProjectLevelMicroservice() {
        def jobId = k8sClient.deployService(projectName, serviceName).json.jobId
        def deploymentLog = k8sClient.client.getJobLogs(jobId)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        def resp = req.get("http://${services[1].status.loadBalancer.ingress[0].ip}:81")
        assert services.size() == 2
        assert pods.size() == 2
        assert services[1].metadata.name == serviceName
        assert services[1].metadata.namespace == "default"
        assert services[1].spec.type == LOAD_BALANCER.value
        assert services[1].spec.ports.first().port == 81
        assert deployments.size() == 1
        assert deployments[0].metadata.name == serviceName
        assert deployments[0].metadata.labels."ec-track" == "stable"
        assert deployments[0].spec.replicas == 2
        assert pods.last().metadata.generateName.startsWith('nginx-service-')
        assert pods.first().metadata.namespace == "default"
        assert pods.first().spec.containers.first().name == containerName
        assert pods.first().metadata.labels.get('ec-svc') == serviceName
        assert pods.first().metadata.labels.get('ec-track') == "stable"
        assert pods.first().spec.containers.first().image == "tomaskral/nonroot-nginx:latest"
        assert pods.first().spec.containers.first().ports.first().containerPort == 8080
        assert pods.first().spec.containers.first().volumeMounts.first().mountPath == "/usr/share/nginx/html"
        assert pods.first().spec.containers.first().env.first().value == "8080"
        assert pods.first().spec.containers.first().env.first().name == "NGINX_PORT"
        assert pods.first().status.phase == "Running"
        assert resp.statusCode() == 200
        assert resp.body().asString() == "Hello World!\n"
        assert !deploymentLog.contains(clusterToken)

    }

    @Test(testName = "Update Project-Level Microservice"/*, invocationCount = 2*/)
    @TmsLink("")
    @Story('Update Microservice')
    @Description("Update Project-level Microservice with the same data")
    void updateProjectLevelMicroservice(){
        k8sClient.deployService(projectName, serviceName)
        k8sClient.updateService(2, volumes, false, LOAD_BALANCER)
        def jobId = k8sClient.deployService(projectName, serviceName).json.jobId
        def deploymentLog = k8sClient.client.getJobLogs(jobId)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        def resp = req.get("http://${services[1].status.loadBalancer.ingress[0].ip}:81")
        assert services.size() == 2
        assert pods.size() == 2
        assert services[1].metadata.name == serviceName
        assert services[1].metadata.namespace == "default"
        assert services[1].spec.type == LOAD_BALANCER.value
        assert services[1].spec.ports.first().port == 81
        assert deployments.size() == 1
        assert deployments[0].metadata.name == serviceName
        assert deployments[0].metadata.labels."ec-track" == "stable"
        assert deployments[0].spec.replicas == 2
        assert pods.last().metadata.generateName.startsWith('nginx-service-')
        assert pods.first().metadata.namespace == "default"
        assert pods.first().spec.containers.first().name == containerName
        assert pods.first().metadata.labels.get('ec-svc') == serviceName
        assert pods.first().metadata.labels.get('ec-track') == "stable"
        assert pods.first().spec.containers.first().image == "tomaskral/nonroot-nginx:latest"
        assert pods.first().spec.containers.first().ports.first().containerPort == 8080
        assert pods.first().spec.containers.first().volumeMounts.first().mountPath == "/usr/share/nginx/html"
        assert pods.first().spec.containers.first().env.first().value == "8080"
        assert pods.first().spec.containers.first().env.first().name == "NGINX_PORT"
        assert pods.first().status.phase == "Running"
        assert resp.statusCode() == 200
        assert resp.body().asString() == "Hello World!\n"
        assert !deploymentLog.contains(clusterToken)
    }


    @Test(testName = "Scale Project-Level Microservice")
    @TmsLink("")
    @Story("Update Microservice")
    @Description("Update Project-level Microservice")
    void scaleProjectLevelMicroservice(){
        k8sClient.deployService(projectName, serviceName)
        k8sClient.updateService(3, volumes, false, LOAD_BALANCER)
        def jobId = k8sClient.deployService(projectName, serviceName).json.jobId
        def deploymentLog = k8sClient.client.getJobLogs(jobId)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        def resp = req.get("http://${services[1].status.loadBalancer.ingress[0].ip}:81")
        assert services.size() == 2
        assert pods.size() == 3
        assert services[1].metadata.name == serviceName
        assert services[1].metadata.namespace == "default"
        assert services[1].spec.type == LOAD_BALANCER.value
        assert services[1].spec.ports.first().port == 81
        assert deployments.size() == 1
        assert deployments[0].metadata.name == serviceName
        assert deployments[0].metadata.labels."ec-track" == "stable"
        assert deployments[0].spec.replicas == 3
        assert pods.last().metadata.generateName.startsWith('nginx-service-')
        assert pods.first().metadata.namespace == "default"
        assert pods.first().spec.containers.first().name == containerName
        assert pods.first().metadata.labels.get('ec-svc') == serviceName
        assert pods.first().metadata.labels.get('ec-track') == "stable"
        assert pods.first().spec.containers.first().image == "tomaskral/nonroot-nginx:latest"
        assert pods.first().spec.containers.first().ports.first().containerPort == 8080
        assert pods.first().spec.containers.first().volumeMounts.first().mountPath == "/usr/share/nginx/html"
        assert pods.first().spec.containers.first().env.first().value == "8080"
        assert pods.first().spec.containers.first().env.first().name == "NGINX_PORT"
        assert pods.first().status.phase == "Running"
        assert resp.statusCode() == 200
        assert resp.body().asString() == "Hello World!\n"
        assert !deploymentLog.contains(clusterToken)
    }


    @Test(testName = "Deploy Canary Project-Level Microservice")
    @TmsLink("")
    @Story('Canary deploy of Microservice')
    @Description("Canary Deploy for Project-level Microservice")
    void preformCanaryDeploymentForProjectLevelMicroservice() {
        k8sClient.deployService(projectName, serviceName)
        k8sClient.updateService(2, volumes, true, LOAD_BALANCER)
        def jobId = k8sClient.deployService(projectName, serviceName).json.jobId
        def deploymentLog = k8sClient.client.getJobLogs(jobId)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        def resp = req.get("http://${services[1].status.loadBalancer.ingress[0].ip}:81")
        assert services.size() == 2
        assert pods.size() == 4
        assert services[1].metadata.name == serviceName
        assert services[1].metadata.namespace == "default"
        assert services[1].spec.type == LOAD_BALANCER.value
        assert services[1].spec.ports.first().port == 81
        assert deployments.size() == 2
        assert deployments[0].metadata.name == serviceName
        assert deployments[1].metadata.name == "nginx-service-canary"
        assert deployments[0].metadata.labels."ec-track" == "stable"
        assert deployments[1].metadata.labels."ec-track" == "canary"
        assert deployments[0].spec.replicas == 2
        assert deployments[1].spec.replicas == 2
        assert pods.last().metadata.generateName.startsWith('nginx-service-')
        assert pods.first().metadata.namespace == "default"
        assert pods.first().metadata.labels.get('ec-svc') == serviceName
        assert pods.first().metadata.labels.get('ec-track') == "stable"
        assert pods.last().metadata.generateName.startsWith('nginx-service-canary-')
        assert pods.last().metadata.namespace == "default"
        assert pods.last().metadata.labels.get('ec-svc') == serviceName
        assert pods.last().metadata.labels.get('ec-track') == "canary"
        pods.each {
            assert it.spec.containers.first().image == "tomaskral/nonroot-nginx:latest"
            assert it.spec.containers.first().ports.first().containerPort == 8080
            assert it.spec.containers.first().volumeMounts.first().mountPath == "/usr/share/nginx/html"
            assert it.spec.containers.first().env.first().value == "8080"
            assert it.spec.containers.first().env.first().name == "NGINX_PORT"
            assert it.status.phase == "Running"
        }
        assert resp.statusCode() == 200
        assert resp.body().asString() == "Hello World!\n"
        assert !deploymentLog.contains(clusterToken)
    }


    @Test(testName = "Undeploy Project-Level Microservice")
    @TmsLink("")
    @Story('Undeploy Microservice')
    @Description("Undeploy Project-level Microservice")
    void undeployMicroservice() {
        k8sClient.deployService(projectName, serviceName)
        def jobId = k8sClient.undeployService(projectName, serviceName).json.jobId
        def deploymentLog = k8sClient.client.getJobLogs(jobId)
        await("Deployment size to be: 0").until {
            k8sApi.getPods().json.items.size() == 0
        }
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        assert deployments.size() == 0
        assert services.size() == 1
        assert pods.size() == 0
        assert services[0].metadata.name == "kubernetes"
        assert !deploymentLog.contains(clusterToken)
    }

    @Test(testName = "Undeploy Canary Project-Level Microservice")
    @Flaky
    @TmsLink("")
    @Story('Undeploy Microservice after Canary deployment')
    @Description("Undeploy Project-level Microservice after Canary Deploy")
    void undeployMicroserviceAfterCanaryDeployment() {
        k8sClient.deployService(projectName, serviceName)
        k8sClient.updateService(2, volumes, true, LOAD_BALANCER)
        k8sClient.deployService(projectName, serviceName)
        def jobId = k8sClient.undeployService(projectName, serviceName).json.jobId
        def deploymentLog = k8sClient.client.getJobLogs(jobId)
        await("Pods size to be: 2").until {
            k8sApi.getPods().json.items.size() == 2
        }
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        def resp = req.get("http://${services[1].status.loadBalancer.ingress[0].ip}:81")
        assert services.size() == 2
        assert deployments.size() == 1
        assert pods.size() == 2
        assert services[1].metadata.name == serviceName
        assert services[1].metadata.namespace == "default"
        assert services[1].spec.type == LOAD_BALANCER.value
        assert services[1].spec.ports.first().port == 81
        assert deployments[0].metadata.name == serviceName
        assert deployments[0].metadata.labels."ec-track" == "stable"
        assert deployments[0].spec.replicas == 2
        assert pods.last().metadata.generateName.startsWith('nginx-service-')
        assert pods.first().metadata.namespace == "default"
        assert pods.first().spec.containers.first().name == containerName
        assert pods.first().metadata.labels.get('ec-svc') == serviceName
        assert pods.first().metadata.labels.get('ec-track') == "stable"
        assert pods.first().spec.containers.first().image == "tomaskral/nonroot-nginx:latest"
        assert pods.first().spec.containers.first().ports.first().containerPort == 8080
        assert pods.first().spec.containers.first().volumeMounts.first().mountPath == "/usr/share/nginx/html"
        assert pods.first().spec.containers.first().env.first().value == "8080"
        assert pods.first().spec.containers.first().env.first().name == "NGINX_PORT"
        assert pods.first().status.phase == "Running"
        assert resp.statusCode() == 200
        assert resp.body().asString() == "Hello World!\n"
        assert !deploymentLog.contains(clusterToken)
    }


}
