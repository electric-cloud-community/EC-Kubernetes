import java.io.File

procedure 'Cleanup Cluster - Experimental',
	description: '[Experimental] This procedure is meant for testing purposes only and should not be used in a production environment. It will delete all services pods, deployments and replication controllers in a given Kubernetes cluster.', {

    // don't add a step picker for this procedure since it is experimental and meant for demo/testing purposes only.
    property 'standardStepPicker', value: false

	step 'setup',
      subproject: '',
      subprocedure: 'Setup',
      command: null,
      errorHandling: 'failProcedure',
      exclusiveMode: 'none',
      postProcessor: 'postp',
      releaseMode: 'none',
      timeLimitUnits: 'minutes', {

    	  actualParameter 'additionalArtifactVersion', ''
    }

	step 'cleanup',
	  command: new File(pluginDir, 'dsl/procedures/deleteServices/steps/deleteServices.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  shell: 'ec-groovy',
	  timeLimitUnits: 'minutes'
	  
}
  
