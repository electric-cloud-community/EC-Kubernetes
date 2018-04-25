import java.io.File

procedure 'Discover',
	description: '''<html>Automatically create microservice models in ElectricFlow for the services and the pods discovered within a namespace on a Kubernetes cluster.
<div>
    <ol>
        <li><b>Select your method of discovery from a Kubernetes Cluster</b>  There are two options for connecting to Kubernetes for discovery
            <ul>
                <li><b>Existing ElectricFlow Environment and Cluster</b>  Use the Cluster configuration details in an existing ElectricFlow environment to connect to Kubernetes. Enter details for the existing environment and cluster in the following parameters:
                    <ul>
                        <li>Environment Project Name: The project containing the existing environment</li>
                        <li>Environment Name:  the name of an existing environment that contains the Kubernetes backend cluster to be discovered</li>
                        <li>Cluster Name: The name of the ElectricFlow cluster in the environment above that represents the Kubernetes cluster</li>
                    </ul></li>
                <li><b>Kubernetes Connection Details</b>  Enter Kubernetes endpoint and Account details to directly connect to the endpoint and discover the clusters and pods.  Enter the endpoint and account details in the following parameters:
                    <ul>
                        <li>Kubernetes Endpoint: The endpoint where the Kubernetes endpoint will be reachable</li>
                        <li>Service Account API Token</li>
                        <li><i>If selecting this connection option, you can optionally enter a new values for Environment Name and Cluster Name parameters, to create a new environment and cluster in ElectricFlow based on the discovered services and pods.</i></li>
                    </ul>
                </li>
            </ul></li>
        <li><b>Determine how the discovered microservices will be created in ElectricFlow</b>
            <ul>
                <li><b>Create the microservices individually at the top-level within the project.</b> All discovered microservices will be created at the top-level.  Enter the following parameters:
                    <ul>
                        <li>Project Name: Enter the name of the project where the microservices will be created</li>
                    </ul>
                </li>
                <li><b>Create the Microservices within an application in ElectricFlow.</b> All discovered microservices will be created as services within a new application. Enter the following parameters:
                    <ul>
                        <li>Project Name: Enter the name of the project where the new application will be created</li>
                        <li>Create Microservices within and Application:  Select the checkbox</li>
                        <li>Application Name:  The name of a new application which will be created in ElectricFlow containing the discovered services</li>
                    </ul>
                </li></ul>
        </li>
    </ol>
</div>
</html>''', {

    //Using a simple description for use with the step picker since it cannot handle HTML content
    property 'stepPickerDescription',
        value: 'Automatically create microservice models in ElectricFlow for the services and the pods discovered within a namespace on a Kubernetes cluster.'

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

