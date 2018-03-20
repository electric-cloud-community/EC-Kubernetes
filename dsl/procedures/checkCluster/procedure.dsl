import java.io.File

procedure 'Check Cluster', 
	description: 'Checks that a Kubernetes cluster exists and is reachable. All other entities such as  Pods, services, and replication controllers all run on top of a cluster.', {

	step 'setup',
      subproject: '',
      subprocedure: 'Setup',
      command: null,
      errorHandling: 'failProcedure',
      exclusiveMode: 'call',
      postProcessor: 'postp',
      releaseMode: 'none',
      timeLimitUnits: 'minutes', {

    	  actualParameter 'additionalArtifactVersion', ''
    }

	step 'checkCluster',
	  command: new File(pluginDir, 'dsl/procedures/checkCluster/steps/checkCluster.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  shell: 'ec-groovy',
	  timeLimitUnits: 'minutes'
	  
}
  
