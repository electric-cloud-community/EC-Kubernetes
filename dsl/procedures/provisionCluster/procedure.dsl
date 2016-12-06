import java.io.File

procedure 'Provision Cluster', 
	description: 'Provisions a Google container cluster which is the foundation of a Container Engine application. Pods, services, and replication controllers all run on top of a cluster.', {

	step 'setup',
    	  command: new File(pluginDir, 'dsl/properties/scripts/retrieveGrapeDependencies.pl').text,
    	  errorHandling: 'failProcedure',
    	  exclusiveMode: 'none',
    	  postProcessor: 'postp',
    	  releaseMode: 'none',
    	  shell: 'ec-perl',
    	  timeLimitUnits: 'minutes'

	step 'provisionCluster', 
	  command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/provisionCluster.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  shell: 'ec-groovy',
	  timeLimitUnits: 'minutes'
	  
}
  
