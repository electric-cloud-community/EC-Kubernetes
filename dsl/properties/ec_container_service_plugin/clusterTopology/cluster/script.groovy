
def projectName = args.projectName
def environmentName = args.environmentName
def clusterName = args.clusterName
def configParameters = args.configurationParameter
def config = [:]
configParameters.each {
    def name = it.configurationParameterName
    def value = it.value
    config[name] = value
}

def credentials = args.credentials
assert credentials.size() == 1
def userName = credentials[0].userName
def password = credentials[0].password

def endpoint = config.clusterEndpoint
def token = password
def version = config.kubernetesVersion

def cluster = getCluster(projectName: projectName, environmentName: environmentName, clusterName: clusterName)
def clusterId = cluster.clusterId.toString()

import com.electriccloud.kubernetes.*
def client = new Client(endpoint, token, version)
assert clusterId
assert clusterName
def clusterView = new ClusterView(kubeClient: client, clusterName: clusterName, clusterId: clusterId)
def response = clusterView.getRealtimeClusterTopology()
response