package com.electriccloud



import com.electriccloud.client.APIClient
import com.electriccloud.client.api.KubernetesApi
import com.electriccloud.client.ectool.EctoolApi
import com.electriccloud.client.plugin.KubernetesClient
import com.electriccloud.helpers.json.JsonHelper
import org.apache.commons.lang.RandomStringUtils

import java.text.SimpleDateFormat

trait NamingTestBase {

    def configName
    def projectName
    def environmentProjectName
    def environmentName
    def clusterName
    def serviceName
    def applicationName
    def containerName
    def acsClusterName
    def resourceGroup
    def clusterEndpoint
    def nodeEndpoint
    def clusterToken

    def pluginName
    def adminAccount
    def pluginVersion
    def pluginLegacyVersion

    //API
    EctoolApi ectoolApi
    KubernetesApi k8sApi
    //GoogleContainerEngineApi gceApi
    //DockerApi dockerApi
    //OpenshiftApi osApi
    //DockerHubApi dockerHub

    // Clients
    TopologyMatcher topologyM
    JsonHelper jsonHelper
    KubernetesClient k8sClient
    // Default Id

    def ecpNamespaceId,
        ecpClusterId,
        ecpClusterName,
        ecpServiceId,
        ecpServiceName,
        ecpContainerId,
        ecpContainerName,
        ecpNamespaceName = "default",
        ecpPodName   = "",
        ecpPodId     = ""

    // Default Names

    def environmentId   = '',
        applicationId = '',
        processId = '',
        processName = '',
        processStepName = '',
        pluginProjectName = '',
        snapshotName,
        serviceId,
        appServiceId,
        clusterId,
        pipelineName,
        releaseName,
        stageName,
        pipelineId,
        releaseId,
        stageId,
        flowRuntimeId,
        taskName,
        clusterVersion,
        topologyOutcome,
        description = 'some desc',
        endpoint


    // Parametrized names

    // Naming Helpers

    String unique(objectName) {
//        new SimpleDateFormat("${objectName}yyyyMMddHHmmssSSS".toString()).format(new Date())
        objectName + (new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()))
    }

    String characters(objectName, num) {
        num = num as Integer
        def _num
        if(num != 0) {
            _num = RandomStringUtils.random(num).next()
            return "${objectName}${_num}".toString()
        } else {
            return ''
        }
    }

    String characters(num) {
        characters('', num)
    }



}

