$[/myProject/scripts/preamble]

EFClient efClient = new EFClient()
def actualParams = efClient.getActualParameters()

if (efClient.toBoolean(actualParams.get('testConnection'))) {

	def cred = efClient.getCredentials('credential')

	String accessToken = 'Bearer ' + cred.password
	String uriToCheckCluster = actualParams.get('uriToCheckCluster').trim()
	String clusterEndpoint = actualParams.get('clusterEndpoint').trim()
	String kubernetesHealthUrl = "$clusterEndpoint"
	if (uriToCheckCluster){
		if(uriToCheckCluster[0] == '/'){
			kubernetesHealthUrl += uriToCheckCluster
		}
		else{
			kubernetesHealthUrl += '/' + uriToCheckCluster
		}
	}

	KubernetesClient client = new KubernetesClient()
	client.setVersion(actualParams)

	def resp = client.checkClusterHealth(kubernetesHealthUrl, accessToken)
	if (resp.status == 200){ 
		efClient.logger INFO, "Kubernetes cluster is reachable at ${clusterEndpoint}. Health check at ${kubernetesHealthUrl}."
	}
	if (resp.status >= 400){
		efClient.handleProcedureError("Kubernetes cluster at ${clusterEndpoint} was not reachable. Health check at ${kubernetesHealthUrl} failed with ${resp.statusLine}")
	}
}