package com.electriccloud.procedures.topology

import com.electriccloud.helpers.config.ConfigHelper
import com.electriccloud.procedures.KubernetesTestBase
import io.qameta.allure.*
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import static com.electriccloud.helpers.enums.LogLevels.*

@Feature("Topology")
class GetRealtimeClusterDetails extends KubernetesTestBase {



    @BeforeClass(alwaysRun = true)
    void createAndDeployProjectLevelMicroservice() {
        createAndDeployService(false)
        setTopology()
    }

    @BeforeMethod(alwaysRun = true)
    void backendAuthorization(){
        ectoolApi.ectoolLogin()
    }

    @AfterClass(alwaysRun = true)
    void tearDown() {
        k8sClient.cleanUpCluster(configName)
        k8sClient.deleteConfiguration(configName)
        k8sClient.client.deleteProject(projectName)
    }


    @Test(groups = "Positive")
    @TmsLink("")
    @Description("Get a Response with correct fields for 'ecp-container' objectType in Topology")
    void getAResponseWithCorrectFieldsForEcpContainerObjectTypeInTopology() {

        topologyOutcome = ectoolApi.run "ectool", "getRealtimeClusterDetails",
                projectName, clusterName, ecpContainerId, "ecp-container", "--environmentName", environmentName

        def resp = ConfigHelper.xml(topologyOutcome).clusterNode
        def action = resp.actions.action[0]
        def attr = { name ->
            resp.attributes.'**'.find { attribute ->
                attribute.name.text() == name }
        }
        assert resp.id == ecpContainerName
        assert resp.name == containerName
        assert resp.electricFlowClusterName == clusterName
        assert resp.electricFlowEnvironmentName == environmentName
        assert resp.electricFlowProjectName == projectName
        assert resp.type == "ecp-container"
        assert action.name == "View Logs"
        assert action.actionType == "viewLogs"
        assert action.responseType == "textarea"
        assert attr("Status").type.text() == "string"
        assert attr("Status").value.text() == "running"
        assert attr("Image").type == "string"
        assert attr("Image").value == "tomaskral/nonroot-nginx:latest"
        assert attr("Node").type == "string"
        assert attr("Node").value =~ /gke-kube-cluster-default-pool-[\w]+-[\w]+/
        assert attr("Environment Variables").type == "map"
        assert attr("Environment Variables").value.items[0].name == 'NGINX_PORT'
        assert attr("Environment Variables").value.items[0].value == '8080'
        assert attr("Ports").type == "map"
        assert attr("Ports").value.items[0].name == 'http'
        assert attr("Ports").value.items[0].value == '8080/TCP'
        assert attr("Volume Mounts").type == "map"
        assert attr("Volume Mounts").value.items[0].name == 'html-content'
        assert attr("Volume Mounts").value.items[0].value == '/usr/share/nginx/html '
        assert attr("Volume Mounts").value.items[1].name =~ /default-token-[\w]/
        assert attr("Volume Mounts").value.items[1].value == '/var/run/secrets/kubernetes.io/serviceaccount (read only)'

        /*assert !_node(  id: ecpContainerName,
                name: containerName,
                displayName: 'Kubernetes Container',
                type: 'ecp-container',
                topologyType: 'clusterNode'
        ).toString().empty

        assert !_action(name:  'View Logs', actionType: 'viewLogs', responseType: 'textarea').toString().empty

        [
                [ name: 'Status',     type: 'string', value: 'running'],
                [ name: 'Image',      type: 'string', value: 'tomaskral/nonroot-nginx:latest'],
                [ name: 'Start time', type: 'date',   value: ~/[\d]{4}-[\d]{2}-[\d]{2}T[\d]{2}:[\d]{2}:[\d]{2}Z/ ],
                [ name: 'Node',       type: 'string', value: ~/k8s-agent-[\w]+-[\w]+/ ],
                [ name: 'Environment Variables', type: 'map', itemName: 'NGINX_PORT', itemValue: '8080' ],
                [ name: 'Ports', type: 'map', itemName:  'http', itemValue: '8080/TCP' ],
                [ name: 'Volume Mounts', type: 'map', itemName: 'html-content', itemValue: ~/\/usr\/share\/nginx\/html /],
                [ name: 'Volume Mounts', type: 'map', itemName: ~/default-token-[\w]+/, itemValue: '/var/run/secrets/kubernetes.io/serviceaccount (read only)' ],
        ].each {
            assert !_attribute(it).toString().empty
        }*/
    }



    @Test(groups = "Positive")
    @TmsLink("")
    @Description("Get a Response with correct fields for 'ecp-cluster' objectType in Topology")
    void getAResponseWithCorrectFieldsForEcpClusterObjectTypeInTopology() {

        topologyOutcome = ectoolApi.run "ectool", "getRealtimeClusterDetails",
                projectName, clusterName, ecpClusterName, "ecp-cluster", "--environmentName", environmentName

        assert !_node(id: ecpClusterId, name: ecpClusterName, value: 'ecp-cluster', topologyType: 'clusterNode').toString().empty
        [
                [ name: 'Endpoint', type: 'link', value: clusterEndpoint ],
                [ name: 'Master Version', type: 'string', value: ~/[\d]+\.[\d]+.*/ ],
        ].each {
            assert !_attribute(it).toString().empty
        }
    }



    @Test(groups = "Positive")
    @TmsLink("")
    @Description("Get a Response with correct fields for 'ecp-namespace' objectType in Topology")
    void getAResponseWithCorrectFieldsForEcpNamespaceObjectTypeInTopology() {

        topologyOutcome = ectoolApi.run "ectool", "getRealtimeClusterDetails",
                projectName, clusterName, ecpNamespaceName, "ecp-namespace", "--environmentName", environmentName

        assert !_node(id: ecpNamespaceName, name: ecpNamespaceName, type: 'ecp-namespace', topologyType: 'clusterNode').toString().empty
        [
                [ name: 'Status', type: 'string', value: 'Active' ],
                [ name: 'Age', type: 'string', value: ~/[\d]+ day\(s\)/ ],
        ].each {
            assert !_attribute(it).toString().empty
        }
    }



    @Test(groups = "Positive")
    @TmsLink("")
    @Description("Get a Response with correct fields for 'ecp-pod' objectType in Topology")
    void getAResponseWithCorrectFieldsForEcpPodObjectTypeInTopology() {

        topologyOutcome = ectoolApi.run "ectool", "getRealtimeClusterDetails",
                projectName, clusterName, ecpPodId, "ecp-pod", "--environmentName", environmentName

        assert !_node(id: ecpPodId, name: ecpPodName, displayName: "Kubernetes Pod", type: 'ecp-pod', topologyType: 'clusterNode' ).toString().empty
        assert !_action(name: 'View Logs', actionType: 'viewLogs', responseType: 'textarea').toString().empty
        [
                [ name: 'Status',     type: 'string', value: 'Running' ],
                [ name: 'Labels',     type: 'map', itemName: 'ec-svc', itemValue: 'nginx-service', index: 0 ],
                [ name: 'Labels',     type: 'map', itemName: 'ec-track', itemValue: 'stable', index: 1 ],
                [ name: 'Labels',     type: 'map', itemName: 'pod-template-hash', itemValue: ~/[\d]+/, index: 2 ],
                [ name: 'Start time', type: 'date', value: ~/[\d]{4}-[\d]{2}-[\d]{2}T[\d]{2}:[\d]{2}:[\d]{2}Z/ ],
                [ name: 'Node',       type: 'string', value: ~/gke-kube-cluster-default-pool-[\w]+-[\w]+/ ],
        ].each {
            assert !it.toString().empty
        }
    }



    @Test(groups = "Positive")
    @TmsLink("")
    @Description("Get a Response with correct fields for 'ecp-service' objectType in Topology")
    void getAResponseWithCorrectFieldsForEcpServiceObjectTypeInTopology() {

        topologyOutcome = ectoolApi.run "ectool", "getRealtimeClusterDetails",
                projectName, clusterName, ecpServiceId, "ecp-service", "--environmentName", environmentName

        assert !_node(
                id: ecpServiceName,
                name: serviceName,
                displayName: 'Kubernetes Service',
                type: 'ecp-service',
                efId: serviceId,
                topologyType: 'clusterNode'
        ).toString().empty
    }




    @Test(groups = "Positive")
    @TmsLink("")
    @Description("Get Realtime Cluster Details using DSL")
    void getRealtimeClusterDetailsUsingDSL() {

        topologyOutcome = ectoolApi.dsl """getRealtimeClusterDetails(projectName: '$projectName', clusterName: '$clusterName', objectId: '$ecpContainerId', objectType: 'ecp-container', environmentName: '$environmentName')"""

        assert !_node(  id: ecpContainerName,
                name: containerName,
                displayName: 'Kubernetes Container',
                type: 'ecp-container',
                topologyType: 'clusterNode'
        ).toString().empty

        assert !_action(name:  'View Logs', actionType: 'viewLogs', responseType: 'textarea').toString().empty

        [
                [ name: 'Status',     type: 'string', value: 'running'],
                [ name: 'Image',      type: 'string', value: 'tomaskral/nonroot-nginx:latest'],
                [ name: 'Start time', type: 'date',   value: ~/[\d]{4}-[\d]{2}-[\d]{2}T[\d]{2}:[\d]{2}:[\d]{2}Z/ ],
                [ name: 'Node',       type: 'string', value: ~/gke-kube-cluster-default-pool-[\w]+-[\w]+/ ],
                [ name: 'Environment Variables', type: 'map', itemName: 'NGINX_PORT', itemValue: '8080', index: 0],
                [ name: 'Ports', type: 'map', itemName:  'http', itemValue: '8080/TCP' ],
                [ name: 'Volume Mounts', type: 'map', itemName: 'html-content', itemValue: ~/\/usr\/share\/nginx\/html.*/, index: 0],
                [ name: 'Volume Mounts', type: 'map', itemName: ~/default-token-[\w]+/, itemValue: '/var/run/secrets/kubernetes.io/serviceaccount (read only)', index: 1],
        ].each {
            assert !_attribute(it).toString().empty
        }
    }




    @Test(groups = "Negative")
    @TmsLink("")
    @Description("Unable to Get Realtime Cluster Details for non-existing Configuration")
    void unableToGetRealtimeClusterDetailsForNonExistingConfiguration() {

        k8sClient.deleteConfiguration(configName)

        topologyOutcome = ectoolApi.run "ectool", "getRealtimeClusterDetails",
                projectName, clusterName, ecpContainerId, "ecp-container", "--environmentName", environmentName

        assert topologyOutcome == "ectool error [NoSuchConfiguration]: No plugin configuration '$configName' " +
                "found at 'ec_plugin_cfgs' for '$pluginName-$pluginVersion'"

        k8sClient.createConfiguration(configName, clusterEndpoint, adminAccount, clusterToken, clusterVersion)

    }



}