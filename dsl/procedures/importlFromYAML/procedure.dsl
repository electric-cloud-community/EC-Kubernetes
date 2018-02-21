import java.io.File

procedure 'ImportFromYAML',
	description: 'Creating microservice models using Kubernetes YAML file.', 
	{

	step 'import',
    	  command: new File(pluginDir, 'dsl/procedures/importFromYAML/steps/createFromYAML.groovy').text,
    	  errorHandling: 'failProcedure',
    	  exclusiveMode: 'none',
    	  postProcessor: 'postp',
    	  releaseMode: 'none',
    	  shell: 'ec-groovy',
    	  timeLimitUnits: 'minutes'
}

