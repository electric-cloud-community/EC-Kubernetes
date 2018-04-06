import java.io.File

procedure 'Discover',
	description: 'Discovers services on Kubernetes and creates corresponding service models for them in ElectricFlow.', {

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

	step 'discover',
    	  command: new File(pluginDir, 'dsl/procedures/discover/steps/discover.groovy').text,
    	  errorHandling: 'failProcedure',
    	  exclusiveMode: 'none',
    	  postProcessor: 'postp',
    	  releaseMode: 'none',
    	  resourceName: '$[grabbedResource]',
    	  shell: 'ec-groovy',
    	  timeLimitUnits: 'minutes'
}

