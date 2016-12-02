import java.io.File

procedure 'Deploy Service',
	description: 'Creates or updates a Deployment to bring up a Replica Set and Pods.', {

	step 'setup',
    	  command: new File(pluginDir, 'dsl/properties/scripts/retrieveGrapeDependencies.pl').text,
    	  errorHandling: 'failProcedure',
    	  exclusiveMode: 'none',
    	  postProcessor: 'postp',
    	  releaseMode: 'none',
    	  shell: 'ec-perl',
    	  timeLimitUnits: 'minutes'

	step 'createOrUpdateDeployment',
	  command: new File(pluginDir, 'dsl/procedures/deployService/steps/createOrUpdateDeployment.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  shell: 'ec-groovy',
	  timeLimitUnits: 'minutes'
	  
}
  
