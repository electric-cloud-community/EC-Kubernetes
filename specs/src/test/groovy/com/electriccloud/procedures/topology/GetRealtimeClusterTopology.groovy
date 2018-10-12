package com.electriccloud.procedures.topology

import com.electriccloud.procedures.KubernetesTestBase
import io.qameta.allure.*
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import static com.electriccloud.helpers.enums.LogLevels.*


@Feature("Topology")
class GetRealtimeClusterTopology extends KubernetesTestBase {



    @BeforeClass
    void createAndDeployProjectLevelMicroservice() {
        createAndDeployService(false)
        setTopology()
    }

    @BeforeMethod
    void backendAuthorization(){
        ectoolApi.ectoolLogin()
    }

    @AfterClass(alwaysRun = true)
    void tearDown() {
        k8sClient.cleanUpCluster(configName)
        k8sClient.deleteConfiguration(configName)
        k8sClient.client.deleteProject(projectName)
    }



    @Test
    @TmsLink("")
    @Story("Get Realtime Cluster Topology positive")
    @Description("Get a Response with correct fields for all Node Types in Topology")
    void getAResponseWithCorrectFieldsForAllNodeTypesInTopology() {

        topologyOutcome = ectoolApi.run "ectool", "getRealtimeClusterTopology", projectName, clusterName,
                "--environmentName", environmentName

        [
                [ id: clusterId,       name: clusterName,      type: "cluster"        ],
                [ id: clusterEndpoint, name: clusterEndpoint,  type: "ecp-cluster",    efRef: true ],
                [ id: ecpNamespaceId,  name: ecpNamespaceName, type: "ecp-namespace",  efRef: true ],
                [ id: ecpServiceId,    name: serviceName,      type: "ecp-service",   status: "Running", efId: serviceId, efRef: true ],
                [ id: ecpPodId,        name: ecpPodName,       type: "ecp-pod",       status: "Running", efRef: true ],
                [ id: ecpContainerId,  name: containerName,    type: "ecp-container", status: "running", efRef: true ],
        ].each { item ->
            assert !_node(item).toString().empty
        }

        [
                [ source: clusterId,        target: clusterEndpoint ],
                [ source: clusterEndpoint,  target: ecpNamespaceId ],
                [ source: ecpNamespaceId,   target: ecpServiceId ],
                [ source: ecpServiceId,     target: ecpPodId ],
                [ source: ecpPodId,         target: ecpContainerId ],
        ].each { item ->
            assert !_link(item).toString().empty
        }
    }



    @Test(enabled = true)
    @TmsLink("")
    @Story("Get Realtime Cluster Topology positive")
    @Description("Get a Response with correct fields for all Node Types in Topology after Deploy Imported Microservice")
    void getAResponseWithCorrectFieldsForAllNodeTypesInTopologyAfterDeployImportedMicroservice() {

        topologyOutcome = ectoolApi.run "ectool", "getRealtimeClusterTopology", projectName, clusterName,
                "--environmentName", environmentName

        [
                [ id: clusterId,       name: clusterName,      type: "cluster"        ],
                [ id: clusterEndpoint, name: clusterEndpoint,  type: "ecp-cluster",    efRef: true ],
                [ id: ecpNamespaceId,  name: ecpNamespaceName, type: "ecp-namespace",  efRef: true ],
                [ id: ecpServiceId,    name: serviceName,      type: "ecp-service",   status: "Running", efId: serviceId, efRef: true ],
                [ id: ecpPodId,        name: ecpPodName,       type: "ecp-pod",       status: "Running", efRef: true ],
                [ id: ecpContainerId,  name: containerName,    type: "ecp-container", status: "running", efRef: true ],
        ].each { item ->
            assert !_node(item).toString().empty
        }

        [
                [ source: clusterId,        target: clusterEndpoint ],
                [ source: clusterEndpoint,  target: ecpNamespaceId ],
                [ source: ecpNamespaceId,   target: ecpServiceId ],
                [ source: ecpServiceId,     target: ecpPodId ],
                [ source: ecpPodId,         target: ecpContainerId ],
        ].each { item ->
            assert !_link(item).toString().empty
        }
        // checkResponseForGetRealtimeClusterTopology(topologyOutcome)
    }



    @Test(enabled = true)
    @TmsLink("")
    @Story("Get Realtime Cluster Topology negative")
    @Description("Unable to Get Realtime Cluster Topology for non-existing Configuration")
    void getTopologyWithoutPluginConfiguration() {

        k8sClient.deleteConfiguration(configName)

        topologyOutcome = ectoolApi.run "ectool", "getRealtimeClusterTopology", projectName, clusterName,
                "--environmentName", environmentName

        assert topologyOutcome == "ectool error [NoSuchConfiguration]: No plugin configuration '$configName' " +
                "found at 'ec_plugin_cfgs' for '$pluginName-$pluginVersion'"

        k8sClient.createConfiguration(configName, clusterEndpoint, 'flowqe', clusterToken, clusterVersion, true, "/api/v1/namespaces", LogLevel.DEBUG)
    }


    @Test
    @TmsLink("")
    @Story("Get Realtime Cluster Topology positive")
    @Description("Get Realtime Cluster Topology using DSL")
    void getRealtimeClusterTopologyUsingDSL() {

        topologyOutcome = ectoolApi.dsl "getRealtimeClusterTopology(projectName:'$projectName',clusterName:'$clusterName',environmentName:'$environmentName')"

        [
                [ id: clusterId,       name: clusterName,      type: "cluster"        ],
                [ id: clusterEndpoint, name: clusterEndpoint,  type: "ecp-cluster",    efRef: true ],
                [ id: ecpNamespaceId,  name: ecpNamespaceName, type: "ecp-namespace",  efRef: true ],
                [ id: ecpServiceId,    name: serviceName,      type: "ecp-service",   status: "Running", efId: serviceId, efRef: true ],
                [ id: ecpPodId,        name: ecpPodName,       type: "ecp-pod",       status: "Running", efRef: true ],
                [ id: ecpContainerId,  name: containerName,    type: "ecp-container", status: "running", efRef: true ],
        ].each { item ->
            assert !_node(item).toString().empty
        }

        [
                [ source: clusterId,        target: clusterEndpoint ],
                [ source: clusterEndpoint,  target: ecpNamespaceId ],
                [ source: ecpNamespaceId,   target: ecpServiceId ],
                [ source: ecpServiceId,     target: ecpPodId ],
                [ source: ecpPodId,         target: ecpContainerId ],
        ].each { item ->
            assert !_link(item).toString().empty
        }
    }




    // Implement in the future!

    @Test(enabled = false)
    @TmsLink("")
    @Story("Get Realtime Cluster Topology negative")
    @Description("Unable to Get Realtime Cluster Topology for stopped Cluster ")
    void unableToGetRealtimeClusterTopologyForStoppedCluster() {

        topologyOutcome = ectoolApi.run "ectool", "getRealtimeClusterTopology", projectName, clusterName,
                "--environmentName", environmentName

        k8sApi.deleteService(serviceName)

        topologyOutcome = ectoolApi.run "ectool", "getRealtimeClusterTopology", projectName, "test",
                "--environmentName", environmentName

        assert topologyOutcome == "Exception occured while retrieving cluster topology: connect timed out"
    }






}