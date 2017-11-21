import java.io.File

procedure 'Invoke Kubernetes API',
	description: 'Invokes Kubernetes REST API based on specified input parameters. Can also be used to create or modify a resource in Kubernetes cluster based on JSON/YAML as input', {

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

	step 'invokeAPI',
	  command: new File(pluginDir, 'dsl/procedures/invokeKubernetesAPI/steps/invokeAPI.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  shell: 'ec-groovy',
	  timeLimitUnits: 'minutes'
	  
}