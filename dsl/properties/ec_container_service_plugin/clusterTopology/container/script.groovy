def projectName = args.projectName
def environmentName = args.environmentName
def clusterName = args.clusterName
def config = args.configurationParameters
def objectType = args.objectType
def objectIdentifier = args.objectId

def credentials = args.credential
assert credentials.size() == 1
def userName = credentials[0].userName
def password = credentials[0].password

def endpoint = config.clusterEndpoint
def token = password
def version = config.kubernetesVersion
assert endpoint
assert version

def cluster = getCluster(projectName: projectName, environmentName: environmentName, clusterName: clusterName)
def clusterId = cluster.clusterId.toString()


import com.electriccloud.errors.EcException
import com.electriccloud.errors.ErrorCodes
import com.electriccloud.kubernetes.*

def client = new Client(endpoint, token, version)
assert clusterId
assert clusterName
def clusterView = new ClusterView(kubeClient: client, clusterName: clusterName, clusterId: clusterId)
def response
try {
    response = clusterView.getContainerDetails(objectIdentifier)
} catch (EcException e) {
    throw e
} catch (Throwable e) {
    throw EcException
        .code(ErrorCodes.ScriptError)
        .message("Exception occured while retrieving pod details")
        .cause(e)
        .location(this.class.getCanonicalName())
        .build()
}
response

