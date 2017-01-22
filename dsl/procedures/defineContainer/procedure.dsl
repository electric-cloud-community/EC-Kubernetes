import java.io.File

procedure 'Define Container',
	description: 'Helper procedure for generating a container spec', {

    property 'standardStepPicker', value: false

	step 'generateSpec',
    	  command: new File(pluginDir, 'dsl/procedures/defineContainer/steps/generateSpec.groovy').text,
    	  errorHandling: 'failProcedure',
    	  exclusiveMode: 'none',
    	  postProcessor: 'postp',
    	  releaseMode: 'none',
    	  shell: 'ec-groovy',
    	  timeLimitUnits: 'minutes'
}
  
