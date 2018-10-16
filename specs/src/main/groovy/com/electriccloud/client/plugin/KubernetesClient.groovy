package com.electriccloud.client.plugin


import com.electriccloud.helpers.enums.LogLevels.LogLevel

import com.electriccloud.client.commander.CommanderClient
import static com.electriccloud.helpers.config.ConfigHelper.message
import static com.electriccloud.helpers.config.ConfigHelper.dslPath
import static com.electriccloud.helpers.enums.ServiceTypes.*

import io.qameta.allure.Step

class KubernetesClient extends CommanderClient {


    KubernetesClient() {
        this.timeout = 240
        this.plugin = 'kubernetes'
    }


    @Step("Create configuration: {configurationName}, {clusterEndpoint}")
    def createConfiguration(configurationName,
                            clusterEndpoint, username, secretToken, clusterVersion,
                            testConnection = true,
                            testConnectionUri = "/apis",
                            logLevel = LogLevel.DEBUG) {
        message("creating kubernetes config")
        def json = jsonHelper.configJson(configurationName, clusterEndpoint, username, secretToken, clusterVersion, testConnection, testConnectionUri, logLevel.getValue())
        def response = client.dslFile(dslPath(plugin, 'config'), client.encode(json.toString()))
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Configuration: ${configurationName} with endpoint ${clusterEndpoint} is successfully created.")
        return response
    }

    @Step("Edit configuration")
    def editConfiguration(clusterEndpoint,
                          username, secretToken, clusterVersion,
                          testConnection = true,
                          testConnectionUri = "",
                          logLevel = LogLevel.DEBUG) {
        message("edit kubernetes config")
        def json = jsonHelper.editConfigJson(clusterEndpoint, username, secretToken, clusterVersion, testConnection, testConnectionUri, logLevel.getValue())
        def response = client.dslFile(dslPath(plugin, 'editConfig'), client.encode(json.toString()))
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Configuration is successfully changed.")
        return response
    }


    @Step
    def createEnvironment(configName) {
        message("environment creation")
        def response = client.dslFile dslPath(plugin, 'environment'), client.encode(jsonHelper.confJson(configName).toString())
        client.log.info("Environment for project: ${response.json.project.projectName} is created successfully.")
        return response
    }

    @Step
    def createService(replicaNum,
                      volumes = [source: null, target: null ],
                      canaryDeploy,
                      serviceType = ServiceType.LOAD_BALANCER,
                      namespace = "default",
                      deploymentTimeout = timeout) {
        message("service creation")
        def json = jsonHelper.serviceJson(replicaNum, volumes, canaryDeploy.toString(), serviceType.getValue(), namespace, deploymentTimeout.toString())
        def response = client.dslFile dslPath(plugin, 'service'), client.encode(json.toString())
        client.log.info("Service for project: ${response.json.project.projectName} is created successfully.")
        return response
    }

    @Step
    def createApplication(replicaNum,
                          volumes = [source: null, target: null ],
                          canaryDeploy,
                          serviceType = ServiceType.LOAD_BALANCER,
                          namespace = "default",
                          deploymentTimeout = timeout) {
        message("application creation")
        def json = jsonHelper.serviceJson(replicaNum, volumes, canaryDeploy.toString(), serviceType.getValue(), namespace, deploymentTimeout.toString())
        def response = client.dslFile dslPath(plugin, 'application'), client.encode(json.toString())
        client.log.info("Service for project: ${response.json.project.projectName} is created successfully.")
        return response
    }

    @Step
    def updateService(replicaNum,
                      volumes = [source: null, target: null ],
                      canaryDeploy,
                      serviceType = ServiceType.LOAD_BALANCER,
                      namespace = "default",
                      deploymentTimeout = timeout) {
        message("service update")
        def json = jsonHelper.serviceJson(replicaNum, volumes, canaryDeploy.toString(), serviceType.getValue(), namespace, deploymentTimeout.toString())
        def response = client.dslFile dslPath(plugin, 'service'), client.encode(json.toString())
        client.log.info("Service for project: ${response.json.project.projectName} is updated successfully.")
        return response
    }

    @Step
    def updateApplication(replicaNum,
                          volumes = [source: null, target: null ],
                          canaryDeploy,
                          serviceType = ServiceType.LOAD_BALANCER,
                          namespace = "default",
                          deploymentTimeout = timeout) {
        message("service update")
        def json = jsonHelper.serviceJson(replicaNum, volumes, canaryDeploy.toString(), serviceType.getValue(), namespace, deploymentTimeout.toString())
        def response = client.dslFile dslPath(plugin, 'application'), client.encode(json.toString())
        client.log.info("Service for project: ${response.json.project.projectName} is updated successfully.")
        return response
    }

    @Step
    def cleanUpCluster(config, namespace = 'default') {
        message("cluster clean-up")
        def response = client.dslFile dslPath(plugin, 'cleanUp'), client.encode(jsonHelper.cleanUpJson(config, namespace).toString())
        client.waitForJobToComplete(response.json.jobId, timeout, 5, "Cluster is successfully cleaned-up.")
        return response
    }


    @Step("Discover {cluster} on {endpoint}")
    def discoverService(project,
                        envProject,
                        envName,
                        cluster,
                        namespace = 'default',
                        endpoint, token,
                        importApp = false,
                        appName = null) {
        message("service discovery")
        def json = jsonHelper.discoveryJson(project, envProject, envName, namespace, cluster,  endpoint, token, importApp.toString(), appName)
        def response = client.dslFile dslPath(plugin, 'discover'), client.encode(json.toString())
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Service is discovered successfully.")
        return response
    }


}
