package com.electriccloud.procedures.deployment

import com.electriccloud.procedures.KubernetesTestBase
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.qameta.allure.Story
import io.qameta.allure.TmsLink
import org.testng.annotations.*


import static com.electriccloud.helpers.enums.LogLevels.*
import static com.electriccloud.helpers.enums.ServiceTypes.*
import static org.awaitility.Awaitility.await

@Feature('Deployment')
class ApplicationDeploymentTests extends KubernetesTestBase {

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
        k8sClient.createApplication(2, volumes, false, ServiceType.LOAD_BALANCER)
    }

    @AfterMethod
    void tearDownTest() {
        k8sClient.cleanUpCluster(configName)
        k8sClient.client.deleteProject(projectName)
    }



    @Test(testName = "Deploy Application-Level Microservice")
    @TmsLink("")
    @Story("Deploy Microservcice")
    @Description("Deploy Application-level Microservice")
    void deployApplicationLevelMicroservice(){
        def jobId = k8sClient.deployApplication(projectName, applicationName).json.jobId
        def deploymentLog = k8sClient.client.getJobLogs(jobId)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        def resp = req.get("http://${services[1].status.loadBalancer.ingress[0].ip}:81")
        assert services.size() == 2
        assert pods.size() == 2
        assert services[1].metadata.name == serviceName
        assert services[1].metadata.namespace == "default"
        assert services[1].spec.type == ServiceType.LOAD_BALANCER.value
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


    @Test(testName = "Update Application-Level Microservice")
    @TmsLink("")
    @Story('Update Microservice')
    @Description("Update Application-level Microservice")
    void updateApplicationLevelMicroservice(){
        k8sClient.deployApplication(projectName, applicationName)
        k8sClient.updateApplication(2, volumes, false, ServiceType.LOAD_BALANCER)
        def jobId = k8sClient.deployApplication(projectName, applicationName).json.jobId
        def deploymentLog = k8sClient.client.getJobLogs(jobId)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        def resp = req.get("http://${services[1].status.loadBalancer.ingress[0].ip}:81")
        assert services.size() == 2
        assert pods.size() == 2
        assert services[1].metadata.name == serviceName
        assert services[1].metadata.namespace == "default"
        assert services[1].spec.type == ServiceType.LOAD_BALANCER.value
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



    @Test(testName = "Scale Application-Level Microservice ")
    @TmsLink("")
    @Story('Update Microservice')
    @Description("Scale Application-level Microservice")
    void scaleApplicationLevelMicroservice(){
        k8sClient.deployApplication(projectName, applicationName)
        k8sClient.updateApplication(3, volumes, false, ServiceType.LOAD_BALANCER)
        def jobId = k8sClient.deployApplication(projectName, applicationName).json.jobId
        def deploymentLog = k8sClient.client.getJobLogs(jobId)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        def resp = req.get("http://${services[1].status.loadBalancer.ingress[0].ip}:81")
        assert services.size() == 2
        assert pods.size() == 3
        assert services[1].metadata.name == serviceName
        assert services[1].metadata.namespace == "default"
        assert services[1].spec.type == ServiceType.LOAD_BALANCER.value
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



    @Test(testName = "Deploy Canary Application-Level Microservice ")
    @TmsLink("")
    @Story('Canary deploy of Microservice')
    @Description("Canary Deploy for Application-level Microservice")
    void preformCanaryDeploymentForApplicationLevelMicroservice() {
        k8sClient.deployApplication(projectName, applicationName)
        k8sClient.updateApplication(2, volumes, true, ServiceType.LOAD_BALANCER)
        def jobId = k8sClient.deployApplication(projectName, applicationName).json.jobId
        def deploymentLog = k8sClient.client.getJobLogs(jobId)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        def resp = req.get("http://${services[1].status.loadBalancer.ingress[0].ip}:81")
        assert services.size() == 2
        assert deployments.size() == 2
        assert pods.size() == 4
        assert services[1].metadata.name == serviceName
        assert services[1].metadata.namespace == "default"
        assert services[1].spec.type == ServiceType.LOAD_BALANCER.value
        assert services[1].spec.ports.first().port == 81
        assert deployments[0].metadata.name == serviceName
        assert deployments[1].metadata.name == "nginx-service-canary"
        assert deployments[0].metadata.labels."ec-track" == "stable"
        assert deployments[1].metadata.labels."ec-track" == "canary"
        assert deployments.each { assert it.spec.replicas == 2 }
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


    @Test(testName = "Undeploy Application-Level Microservice")
    @TmsLink("")
    @Story('Undeploy Microservice')
    @Description("Undeploy Application-level Microservice")
    void undeployApplication() {
        k8sClient.deployApplication(projectName, applicationName)
        def jobId = k8sClient.undeployApplication(projectName, applicationName).json.jobId
        def deploymentLog = k8sClient.client.getJobLogs(jobId)
        await("Pods size to be: 0").until {
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


    @Test
    @Story('Undeploy Application after Canary deployment')
    @Description("Undeploy Application-level Microservice after Canary Deploy")
    void undeployApplicationAfterCanaryDeployment() {
        k8sClient.deployApplication(projectName, applicationName)
        k8sClient.updateApplication(2, volumes, true, ServiceType.LOAD_BALANCER)
        k8sClient.deployApplication(projectName, applicationName)
        def jobId = k8sClient.undeployApplication(projectName, applicationName).json.jobId
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
        assert services[1].spec.type == ServiceType.LOAD_BALANCER.value
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
