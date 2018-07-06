$[/myProject/scripts/preamble]

EFClient efClient = new EFClient()
def actualParams = efClient.getActualParameters()

if (efClient.toBoolean(actualParams.get('testConnection'))) {

	def cred = efClient.getCredentials('credential')

	String accessToken = 'Bearer ' + cred.password
	String uriToCheckCluster = actualParams.get('uriToCheckCluster').trim()
	String clusterEndpoint = actualParams.get('clusterEndpoint').trim()

	KubernetesClient client = new KubernetesClient()
	client.setVersion(actualParams)

	String healthUrlDefault = "${clusterEndpoint}/apis"
	def responseHealthCheckDefault = client.checkClusterHealth(clusterEndpoint, accessToken)
	if (responseHealthCheckDefault.status >= 400){
		efClient.handleProcedureError("Kubernetes cluster at ${clusterEndpoint} was not reachable. Health check (#1) at ${healthUrlDefault} failed with ${responseHealthCheckDefault.statusLine}")
	} else {
		efClient.logger INFO, "Kubernetes cluster is reachable at ${clusterEndpoint}. Health check (#1) at ${healthUrlDefault}."
	}

	if (uriToCheckCluster && uriToCheckCluster[0] != '/'){
		uriToCheckCluster = '/' + uriToCheckCluster
	}
	String healthUrlProcedure = "${clusterEndpoint}${uriToCheckCluster}"
	def responseHealthCheckProcedure = client.doHttpGet(clusterEndpoint,
			uriToCheckCluster,
			accessToken, /*failOnErrorCode*/ false)
	if (responseHealthCheckProcedure.status >= 400){
		efClient.handleProcedureError("Kubernetes cluster at ${clusterEndpoint} was not reachable. Health check (#2) at ${healthUrlProcedure} failed with ${responseHealthCheckProcedure.statusLine}")
	} else {
		efClient.logger INFO, "Kubernetes cluster is reachable at ${clusterEndpoint}. Health check (#2) at ${healthUrlProcedure}."
	}
}