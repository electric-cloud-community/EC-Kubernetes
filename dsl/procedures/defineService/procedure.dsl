import java.io.File

procedure 'Define Service',
	description: 'Helper procedure for generating a service spec', {

    property 'standardStepPicker', value: false

	step 'generateSpec',
    	  command: new File(pluginDir, 'dsl/procedures/defineService/steps/generateSpec.groovy').text,
    	  errorHandling: 'failProcedure',
    	  exclusiveMode: 'none',
    	  postProcessor: 'postp',
    	  releaseMode: 'none',
    	  shell: 'ec-groovy',
    	  timeLimitUnits: 'minutes'
}
  
