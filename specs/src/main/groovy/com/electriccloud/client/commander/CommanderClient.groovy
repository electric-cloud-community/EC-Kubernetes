package com.electriccloud.client.commander


import com.electriccloud.client.APIClient
import com.electriccloud.helpers.json.JsonHelper

import static com.electriccloud.helpers.config.ConfigHelper.message
import static com.electriccloud.helpers.config.ConfigHelper.dslPath
import static com.electriccloud.helpers.config.ConfigHelper.yamlPath
import groovy.json.JsonBuilder
import io.qameta.allure.Step



class CommanderClient {

    APIClient client
    def timeout = 120
    def plugin
    def json
    JsonHelper jsonHelper

    CommanderClient(){
        this.client = new APIClient()
        this.json = new JsonBuilder()
        this.jsonHelper = new JsonHelper()
    }

    CommanderClient(serverUri, username, password){
        this.client = new APIClient(serverUri, username, password)
        this.json = new JsonBuilder()
        this.jsonHelper = new JsonHelper()
    }


    @Step
    def createConfiguration(dslFile) {
        message("creating config")
        def response = client.dslFile dslPath(plugin, dslFile)
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Configuration is successfully created.")
        return response
    }


    @Step("Delete configuration: {confName}")
    def deleteConfiguration(confName) {
        message("removing configuration")
        def response = client.dslFile(dslPath(plugin, 'deleteConfig'), client.encode(jsonHelper.confJson(confName).toString()))
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Configuration: ${confName} is successfully deleted.")
        return response
    }


    @Step
    def createService(serviceDslFile) {
        message("service creation")
        def response = client.dslFile dslPath(plugin, serviceDslFile)
        client.log.info("Service for project: ${response.json.project.projectName} is created successfully.")
        return response
    }


    @Step("Deploy project-level service: {serviceName}")
    def deployService(projectName, serviceName) {
        message("service deployment")
        def mapping = getServiceMappings(projectName, serviceName)[0]
        def json = jsonHelper.deployJson(projectName, mapping.environmentName, mapping.environmentProjectName, serviceName)
        def response = client.dslFile dslPath(plugin, 'deploy'), client.encode(json.toString())
        client.waitForJobToComplete(response.json.jobId, timeout, 5, "Deployment is successfully completed.")
        return response
    }


    @Step("Undeploy project-level service: {serviceName}")
    def undeployService(projectName, serviceName) {
        message("service undeploy")
        def mapping = getServiceMappings(projectName, serviceName)[0]
        def json = jsonHelper.deployJson(projectName, mapping.environmentName, mapping.environmentProjectName, serviceName)
        def response = client.dslFile dslPath(plugin, 'undeploy'), client.encode(json.toString())
        client.waitForJobToComplete(response.json.jobId, timeout, 5, "Undeploy is successfully completed.")
        return response
    }

    @Step("Deploy application-level service: {serviceName}")
    def deployApplication(projectName, applicationName) {
        message("application service deployment")
        def mapping = getAppMappings(projectName, applicationName)[0]
        def json = jsonHelper.deployAppJson(projectName, mapping.applicationName, mapping.tierMapName)
        def response = client.dslFile dslPath(plugin, 'appDeploy'), client.encode(json.toString())
        client.waitForJobToComplete(response.json.jobId, timeout, 5, "Deployment is successfully completed.")
        return response
    }

    @Step("Undeploy application-level service: {serviceName}")
    def undeployApplication(projectName, applicationName) {
        message("application service undeploy")
        def mapping = getAppMappings(projectName, applicationName)[0]
        def json = jsonHelper.deployAppJson(projectName, mapping.applicationName, mapping.tierMapName)
        def response = client.dslFile dslPath(plugin, 'appUndeploy'), client.encode(json.toString())
        client.waitForJobToComplete(response.json.jobId, timeout, 5, "Undeploy is successfully completed.")
        response
    }


    @Step
    def importService(yamlfile, projectName, envProject, envName, clusterName, importApp = false, applicationName = null) {
        message("service import")
        File yaml = new File("./${yamlPath(plugin, yamlfile)}")
        def yamlFileText = yaml.text.readLines().join('\\n')
        client.log.info("Importing YAML: \n ${yaml.text}")
        def json = jsonHelper.importJson(yamlFileText, projectName, envProject, envName, clusterName,  importApp, applicationName)
        def response = client.dslFile dslPath(plugin, 'import'), client.encode(json.toString())
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Import is successfully completed.")
        response
    }


    @Step
    def provisionEnvironment(projectName, environmentName, clusterName, timeout = 120) {
        message("environment provisioning")
        def json = jsonHelper.provisionJson(projectName, environmentName, clusterName)
        def response = client.dslFile(dslPath(plugin, 'provision'), client.encode(json.toString()))
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Cluster provisioning is successfully completed.")
        response
    }


    def getServiceMappings(project, service) {
        message("got service mappings")
        def map = client.dslFile(dslPath(plugin, 'envMaps'), client.encode(jsonHelper.mapingJson(project, service).toString())).json.environmentMap
        // returns the collection of mappings
        map
    }

    def getAppMappings(project, application) {
        message("got application mappings")
        def map = client.tierMappings(project, application).json.tierMap
        // returns the collection of mappings
        map
    }


}
