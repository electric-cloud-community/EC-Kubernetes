package com.electriccloud.procedures.deployment

import com.electriccloud.procedures.KubernetesTestBase
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.qameta.allure.Story
import io.qameta.allure.TmsLink
import org.testng.annotations.*

import java.util.concurrent.TimeUnit

import static com.electriccloud.helpers.enums.LogLevels.*
import static com.electriccloud.helpers.enums.LogLevels.LogLevel.*
import static com.electriccloud.helpers.enums.ServiceTypes.*
import static com.electriccloud.helpers.enums.ServiceTypes.ServiceType.*
import static org.awaitility.Awaitility.await

@Feature("Deployment")
class ServiceTypeDeploymentTests extends KubernetesTestBase {


    @BeforeClass(alwaysRun = true)
    void setUpTests(){
        k8sClient.deleteConfiguration(configName)
        k8sClient.createConfiguration(configName, clusterEndpoint, adminAccount, clusterToken, clusterVersion, true, "/apis", DEBUG)
    }

    @BeforeMethod(alwaysRun = true)
    void setUpTest(){
        k8sClient.createEnvironment(configName)
    }

    @AfterMethod(alwaysRun = true)
    void tearDownTest() {
        k8sClient.cleanUpCluster(configName)
        await().atMost(50, TimeUnit.SECONDS).until { k8sApi.getPods().json.items.size() == 0 }
        k8sClient.client.deleteProject(projectName)
    }




    @Test(groups = "Positive", testName = "Deploy Microservice with LoadBalancer")
    @Story("Deploy service using LoadBalancer service type")
    @Description(" Deploy Project-level Microservice with LoadBalancer service type")
    void deployMicroserviceWithLoadBalancer(){
        k8sClient.createService(2, volumes, false, LOAD_BALANCER)
        def jobId = k8sClient.deployService(projectName, serviceName).json.jobId
        def deploymentLog = k8sClient.client.getJobLogs(jobId)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        assert services.size() == 2
        assert pods.size() == 2
        assert services[1].metadata.name == serviceName
        assert services[1].metadata.namespace == "default"
        assert services[1].spec.type == LOAD_BALANCER.value
        assert services[1].spec.ports.first().port == 81
        assert services[1].spec.ports[0].nodePort != null
        assert services[1].status.loadBalancer.ingress[0].ip != null
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
        assert !deploymentLog.contains(clusterToken)
    }



    @Test(groups = "Positive", testName = "Deploy Microservice with ClusterIP")
    @Story("Deploy service using ClusterIP service type")
    @Description("Deploy Project-level Microservice with ClusterIP service type")
    void deployMicroserviceWithClusterIP(){
        k8sClient.createService(2, volumes, false, CLUSTER_IP)
        def jobId = k8sClient.deployService(projectName, serviceName).json.jobId
        def deploymentLog = k8sClient.client.getJobLogs(jobId)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        assert services.size() == 2
        assert pods.size() == 2
        assert services[1].metadata.name == serviceName
        assert services[1].metadata.namespace == "default"
        assert services[1].spec.type == CLUSTER_IP.value
        assert services[1].status.loadBalancer.ingress == null
        assert services[1].spec.ports[0].nodePort == null
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
        assert !deploymentLog.contains(clusterToken)
    }


    @Test(groups = "Positive", testName = "Deploy Microservice with NodePort")
    @Story("Deploy service using NodePort service type")
    @Description("Deploy Project-level Microservice with NodePort service type")
    void deployMicroserviceWithNodePort(){
        k8sClient.createService(2, volumes, false, NODE_PORT)
        def jobId = k8sClient.deployService(projectName, serviceName).json.jobId
        def deploymentLog = k8sClient.client.getJobLogs(jobId)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        assert services.size() == 2
        assert pods.size() == 2
        assert services[1].metadata.name == serviceName
        assert services[1].metadata.namespace == "default"
        assert services[1].spec.type == NODE_PORT.value
        assert services[1].spec.ports.first().port == 81
        assert services[1].status.loadBalancer.ingress == null
        assert services[1].spec.ports[0].nodePort != null
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
        assert !deploymentLog.contains(clusterToken)
    }




}