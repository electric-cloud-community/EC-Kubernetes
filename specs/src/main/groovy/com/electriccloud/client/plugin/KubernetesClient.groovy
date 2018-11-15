package com.electriccloud.client.plugin


import com.electriccloud.models.enums.LogLevels.LogLevel

import com.electriccloud.client.commander.CommanderClient
import static com.electriccloud.models.config.ConfigHelper.message
import static com.electriccloud.models.config.ConfigHelper.dslPath
import static com.electriccloud.models.enums.LogLevels.LogLevel.*
import static com.electriccloud.models.enums.ServiceTypes.*

import io.qameta.allure.Step

import static com.electriccloud.models.enums.ServiceTypes.ServiceType.*

class KubernetesClient extends CommanderClient {


    KubernetesClient() {
        this.timeout = 240
        this.plugin = 'kubernetes'
    }


    @Step("Create configuration: {configurationName}, {clusterEndpoint}")
    def createConfiguration(configurationName, clusterEndpoint, username, secretToken, clusterVersion, testConnection = true, testConnectionUri = "/apis", logLevel = DEBUG) {
        message("creating kubernetes config")
        def response = client.dslFileMap(dslPath(plugin, 'config'), [params: [
                configName: configurationName,
                endpoint: clusterEndpoint,
                logLevel: logLevel.getValue(),
                userName: username,
                token: secretToken,
                version: clusterVersion,
                testConnection: testConnection,
                uriToCheckCluster: testConnectionUri
        ]])
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Configuration: ${configurationName} with endpoint ${clusterEndpoint} is successfully created.")
        return response
    }

    @Step("Delete configuration: {confName}")
    def deleteConfiguration(configName) {
        message("removing configuration")
        def response = client.dslFileMap(dslPath(plugin, 'deleteConfig'), [params: [configName: configName]])
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Configuration: ${configName} is successfully deleted.")
        return response
    }

    @Step("Edit configuration")
    def editConfiguration(clusterEndpoint,
                          username, secretToken, clusterVersion,
                          testConnection = true,
                          testConnectionUri = "",
                          logLevel = DEBUG) {
        message("edit kubernetes config")
        def response = client.dslFileMap(dslPath(plugin, 'editConfig'), [params: [
                endpoint: clusterEndpoint,
                logLevel: logLevel.getValue(),
                userName: username,
                token: secretToken,
                version: clusterVersion,
                testConnection: testConnection,
                uriToCheckCluster: testConnectionUri
        ]])
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Configuration is successfully changed.", false)
        return response
    }


    @Step
    def createEnvironment(configName) {
        message("environment creation")
        def response = client.dslFileMap dslPath(plugin, 'environment'),  [params: [configName: configName]]
        client.log.info("Environment for project: ${response.json.project.projectName} is created successfully.")
        return response
    }

    @Step
    def createService(replicaNum, volumes = [source: null, target: null ], canaryDeploy, serviceType = LOAD_BALANCER, namespace = "default", deploymentTimeout = timeout) {
        message("service creation")
        def response = client.dslFileMap dslPath(plugin, 'service'), [params: [
                replicas: replicaNum,
                sourceVolume: volumes.source,
                targetVolume: volumes.target,
                isCanary: canaryDeploy,
                serviceType: serviceType,
                namespace: namespace,
                deploymentTimeout: deploymentTimeout
        ]]
        client.log.info("Service for project: ${response.json.project.projectName} is created successfully.")
        return response
    }

    @Step
    def createApplication(replicaNum, volumes = [source: null, target: null ], canaryDeploy, serviceType = LOAD_BALANCER, namespace = "default", deploymentTimeout = timeout) {
        message("application creation")
        def response = client.dslFileMap dslPath(plugin, 'application'), [params: [
                replicas: replicaNum,
                sourceVolume: volumes.source,
                targetVolume: volumes.target,
                isCanary: canaryDeploy,
                serviceType: serviceType,
                namespace: namespace,
                deploymentTimeout: deploymentTimeout
        ]]
        client.log.info("Service for project: ${response.json.project.projectName} is created successfully.")
        return response
    }


    @Step
    def cleanUpCluster(configName, namespace = 'default') {
        message("cluster clean-up")
        def response = client.dslFileMap dslPath(plugin, 'cleanUp'), [params: [configName: configName, projectNamespace: namespace]]
        client.waitForJobToComplete(response.json.jobId, timeout, 5, "Cluster is successfully cleaned-up.")
        return response
    }


    @Step("Discover {cluster} on {endpoint}")
    def discoverService(project, envProject, envName, cluster, namespace = 'default', endpoint, token, importApp = false, appName = null) {
        message("service discovery")
        def response = client.dslFileMap dslPath(plugin, 'discover'), [params: [
                projectName: project,
                envProjectName: envProject,
                environmentName: envName,
                namespace: namespace,
                clusterName: cluster,
                clusterEndpoint: endpoint,
                clusterApiToken: token,
                applicationScoped: importApp.toString(),
                applicationName: appName
        ]]
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Service is discovered successfully.")
        return response
    }


}
