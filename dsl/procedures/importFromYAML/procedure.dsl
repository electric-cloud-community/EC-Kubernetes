import java.io.File

procedure 'Import From YAML',
	description: 'Creating microservice models using Kubernetes YAML file.', 
	{

	step 'import',
    	  command: new File(pluginDir, 'dsl/procedures/importFromYAML/steps/import.groovy').text,
    	  errorHandling: 'failProcedure',
    	  exclusiveMode: 'none',
    	  postProcessor: 'postp',
    	  releaseMode: 'none',
    	  shell: 'ec-groovy',
    	  timeLimitUnits: 'minutes'
}

