package com.electriccloud.helpers.json


import groovy.json.JsonBuilder


class JsonHelper {

    def json = new JsonBuilder()

    /**
     * Service Mapping
     */

    def mapingJson = { project, service ->
        json.names {
            projectName "${project}"
            serviceName "${service}"
        }
        json
    }

    /**
     * Deploy Json
     */


    def deployJson = { projectName, environmentName, environmentProjectName, serviceName ->
        json.names {
            project "${projectName}"
            environment "${environmentName}"
            envProject "${environmentProjectName}"
            service "${serviceName}"
        }
        json
    }


    def deployAppJson = { projectName, applicationName, tierMappingName ->
        json.names {
            project "${projectName}"
            appName "${applicationName}"
            tierMapName "${tierMappingName}"
        }
        json
    }


    def confJson = { configurationName ->
        json.names {
            configName configurationName
        }
        json
    }

    def envJson = { projectName, configurationName ->
        json.names {
            projName "${projectName}"
            configName "${configurationName}"
        }
        json
    }

    /**
     * Import
     */

    def importJson = { yamlText, project, envProject, envName, cluster, importApp = false, appName ->
        def appScoped
        if (importApp){
            appScoped = "1"
        } else {
            appScoped = null
        }
        json.names {
            templateYaml "${yamlText}"
            projectName project
            applicationScoped appScoped
            applicationName appName
            envProjectName envProject
            environmentName envName
            clusterName cluster
        }
        json
    }

    /**
     * Provisoning
     */

    def provisionJson = { project, environment, clusterName ->
        json.names {
            projectName project
            environmentName environment
            cluster clusterName
        }
        json
    }




    /**
     * Configuration json
     */
    def configJson = { configurationName, clusterEndpoint, username, secretToken, clusterVersion, testConnect, connectCheckUri, logLevels->
        json.names {
            configName configurationName
            endpoint clusterEndpoint
            logLevel logLevels
            userName username
            token secretToken
            version clusterVersion
            testConnection testConnect
            uriToCheckCluster connectCheckUri
        }
        json
    }

    def editConfigJson = { clusterEndpoint, username, secretToken, clusterVersion, testConnect, connectCheckUri, logLevels ->
        json.names {
            endpoint clusterEndpoint
            logLevel logLevels
            userName username
            token secretToken
            version clusterVersion
            testConnection testConnect
            uriToCheckCluster connectCheckUri
        }
        json
    }


    def cleanUpJson = { configuration, namespace ->
        json.names {
            configName configuration
            projectNamespace namespace
        }
        json
    }


    def serviceJson = { replicaNum, volumes = [source: null, target: null ], canaryDeploy, servicesType, deploymentNamespace, deploymentTimeoutInSec ->
        json.names {
            replicas replicaNum
            sourceVolume volumes.source
            targetVolume volumes.target
            isCanary canaryDeploy
            serviceType servicesType
            namespace deploymentNamespace
            deploymentTimeout deploymentTimeoutInSec
        }
        json
    }

    def discoveryJson(project, envProject, envName, nameSpace, cluster, endpoint, token, appScoped, appName){
        json.names {
            projectName project
            envProjectName envProject
            environmentName envName
            namespace nameSpace
            clusterName cluster
            clusterEndpoint endpoint
            clusterApiToken token
            applicationScoped appScoped
            applicationName appName
        }
        json
    }




}