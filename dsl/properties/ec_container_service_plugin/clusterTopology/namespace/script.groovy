import com.electriccloud.kubernetes.Client

def projectName = args.projectName
def environmentName = args.environmentName
def clusterName = args.clusterName
def namespaceName = args.namespace
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

def endpoint = 'https://35.202.139.131'
def token = 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6InNlcmd1ZXktdG9rZW4tNjg4OG0iLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoic2VyZ3VleSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6IjY5MmNjOTc3LTExYTMtMTFlOC05NWZjLTQyMDEwYTgwMDMyZSIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0OnNlcmd1ZXkifQ.cDKBBMJhye7rg45ecHR-2jLv0JVJWUy5MAPtRo27dGyMZlMImcHqIVmeGdn5_6Njr3DIuEbSZvcc-Z80WCUZnV_LXtoJIRXDGONSgsASL4EniDfjd7M7B0hVTWRi-_racZII3JZIuL03zXPq2xb01_ZGANe-LqnNaLJDfCvrpMzqqMyskG6BocG1HkxncSLfQrotF3P9W5R8UeWerKScmB6ZIDnLN3OjHDtSuep1yQg-g4LjsyurM-_z1toH35RA3RpkjX-IWH_nm6PPtXGNSWJ7r1GOeVZ2EUxusmsWtzzDsR9W0zIqTXXzUfXCItcctrngk1zVEn4lAOloCuaSUg'



def cluster = getCluster(projectName: projectName, environmentName: environmentName, clusterName: clusterName)
def clusterId = cluster.clusterId.toString()
def client = new Client(endpoint, token, version)
assert clusterId
assert clusterName
def clusterView = new ClusterView(kubeClient: client, clusterName: clusterName, clusterId: clusterId)


def response
try {
    response = clusterView.buildNamespace(namespaceName)
} catch (EcException e) {
    throw e
} catch (Throwable e) {
    throw EcException.code(ErrorCodes.ScriptError).message("Exception occured while retrieving cluster namespace ${namespaceName}").cause(e).location(this.class.getCanonicalName()).build()
}
response