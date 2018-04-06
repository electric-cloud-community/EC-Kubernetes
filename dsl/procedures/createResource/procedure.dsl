import java.io.File

procedure 'Create Resource',
	description: '[Deprecated] Creates or updates a resource in Kubernetes cluster based on JSON/YAML as input', {

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

	step 'createResource',
	  command: new File(pluginDir, 'dsl/procedures/createResource/steps/createResource.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  shell: 'ec-groovy',
	  resourceName: '$[grabbedResource]',
	  timeLimitUnits: 'minutes'
	  
}
