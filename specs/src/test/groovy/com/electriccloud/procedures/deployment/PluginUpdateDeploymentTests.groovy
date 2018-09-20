package com.electriccloud.procedures.deployment

import com.electriccloud.procedures.KubernetesTestBase
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import static com.electriccloud.helpers.enums.LogLevels.*
import static com.electriccloud.helpers.enums.ServiceTypes.*
import static org.awaitility.Awaitility.await


@Feature("Deployment")
class PluginUpdateDeploymentTests extends KubernetesTestBase {

    @BeforeClass
    void setUpTests(){
        ectoolApi.deletePlugin pluginProjectName, pluginVersion
        def legacyPlugin = ectoolApi.installPlugin("${pluginName}-legacy").plugin
        ectoolApi.promotePlugin(legacyPlugin.projectName)
        k8sClient.createConfiguration(configName, clusterEndpoint, 'flowqe', clusterToken, clusterVersion)
        k8sClient.createEnvironment(configName)
    }

    @AfterClass
    void tearDownTests(){
        ectoolApi.deletePlugin pluginProjectName, pluginLegacyVersion
        def latestPlugin = ectoolApi.installPlugin(pluginProjectName).plugin
        ectoolApi.promotePlugin(latestPlugin.pluginName)
        k8sClient.deleteConfiguration(configName)
        k8sClient.client.deleteProject(projectName)
    }


    @AfterMethod
    void tearDownTest(){
        k8sClient.cleanUpCluster(configName)
    }




    @Test(groups = 'pluginUpdate')
    @Story('Deploy service after Plugin version update')
    void pluginUpdateDeployment(){
        k8sClient.createService(2, false, ServiceType.LOAD_BALANCER)
        k8sClient.deployService(projectName, serviceName)
        def plugin = ectoolApi.installPlugin(pluginProjectName).plugin
        ectoolApi.promotePlugin(plugin.projectName)
        k8sClient.deleteConfiguration(configName)
        k8sClient.createConfiguration(configName, clusterEndpoint, 'flowqe', clusterToken, clusterVersion)
        k8sClient.updateService(3, volumes, false, ServiceType.LOAD_BALANCER)
        k8sClient.deployService(projectName, serviceName)
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
        assert resp.body().asString().contains("Welcome to nginx!")
    }







}
