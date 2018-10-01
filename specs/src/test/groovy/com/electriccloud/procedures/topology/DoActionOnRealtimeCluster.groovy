package com.electriccloud.procedures.topology

import com.electriccloud.helpers.config.ConfigHelper
import com.electriccloud.helpers.enums.LogLevels
import com.electriccloud.procedures.KubernetesTestBase
import io.qameta.allure.*
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import java.util.concurrent.TimeUnit

import static org.awaitility.Awaitility.await



@Feature("Topology")
class DoActionOnRealtimeCluster extends KubernetesTestBase {


    @BeforeClass
    void createAndDeployProjectLevelMicroservice() {
        createAndDeployService(false)
        setTopology()
    }

    @AfterClass(alwaysRun = true)
    void tearDown() {
        k8sClient.cleanUpCluster(configName)
        await().atMost(50, TimeUnit.SECONDS).until { k8sApi.getPods().json.items.size() == 0 }
        k8sClient.client.deleteProject(projectName)
    }


    @Test
    @TmsLink("")
    @Story("Do actions on Topology positive")
    @Description("View Logs for 'ecp-container' objectType in Topology")
    void viewLogsForEcpContainerObjectTypeInTopology() {

        topologyOutcome = ectoolApi.run "ectool", "doActionOnRealtimeCluster",
                projectName, clusterName, ecpContainerId, "ecp-container", "viewLogs", "--environmentName", environmentName

        assert topologyOutcome ==~ /.*[\d]{4}\/[\d]{2}\/[\d]{2} [\d]{2}:[\d]{2}:[\d]{2} \[notice\] [\d]+#[\d]+: nginx\/[\d]+.[\d]+.[\d]+.*/
    }


    @Test
    @TmsLink("")
    @Story("Do actions on Topology positive")
    @Description("Skip actionParameter if it is not implemented for objectType in Topology")
    void getAResponseWithCorrectFieldsForEcpClusterObjectTypeInTopology() {

        topologyOutcome = ectoolApi.run "ectool", "doActionOnRealtimeCluster",
                projectName, clusterName, ecpContainerId, "ecp-container", "nonExistingAction", "--environmentName", environmentName

        pluginProjectName = ConfigHelper.xml(
                ectoolApi.run("ectool", "getPlugin", pluginName)
        ).'*'.projectName.text()

        assert topologyOutcome == "ectool error [NotImplemented]: Cluster '$clusterName' is configured to use '$pluginProjectName' plugin which does not support action 'nonExistingAction' on object type 'ecp-container'."
    }



    @Test
    @TmsLink("")
    @Story("Do actions on Topology negative")
    @Description("Unable to Get Realtime Cluster Details for non-existing Configuration")
    void unableToGetRealtimeClusterDetailsForNonExistingConfiguration() {

        k8sClient.deleteConfiguration(configName)

        pluginProjectName = ConfigHelper.xml(
                ectoolApi.run("ectool", "getPlugin", pluginName)
        ).'*'.projectName.text()

        topologyOutcome = ectoolApi.run "ectool", "doActionOnRealtimeCluster",
                projectName, clusterName, ecpContainerId, "ecp-container", "viewLogs", "--environmentName", environmentName

        assert topologyOutcome == "ectool error [NoSuchConfiguration]: No plugin configuration '$configName' " +
                "found at 'ec_plugin_cfgs' for '$pluginProjectName'"

        k8sClient.createConfiguration(configName, clusterEndpoint, 'qe', clusterToken, '1.8')
    }


    @Test
    @TmsLink("")
    @Story("Do actions on Topology positive")
    @Description("Perform Action on Realtime Cluster using DSL")
    void performActionOnRealtimeClusterUsingDSL() {

        topologyOutcome = ectoolApi.dsl """doActionOnRealtimeCluster(projectName: '$projectName', clusterName: '$clusterName', objectId: '$ecpContainerId', objectType: 'ecp-container', action: 'viewLogs', environmentName: '$environmentName')"""

        assert topologyOutcome ==~ /.*[\d]{4}\/[\d]{2}\/[\d]{2} [\d]{2}:[\d]{2}:[\d]{2} \[notice\] [\d]+#[\d]+: nginx\/[\d]+.[\d]+.[\d]+.*/
    }


}
