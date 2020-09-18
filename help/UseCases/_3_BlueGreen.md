### Blue/Green Deployments

Blue/green deployments allow us to deploy 2 versions of an application, the current version and the next to an environment and switch seamlessly between them with zero downtime for the application.

A blue/green deployment can be achieved in Kubernetes by creating a new deployment, say *my-deployment-green* for running the new version while all the old pods created earlier for the older deployment, say *my-deployment-blue* are still serving all the live requests. Once the new deployment has completed successfully, and has optionally been tested through a test service, then the service in Kubernetes is switched to send requests to the newly created pods running the new version.

When CloudBees CD deploys a service to Kubernetes, the EC-Kubernetes plugin manages both the service in Kubernetes (the abstraction for a logical set of pods and a policy by which to access them) as well as the deployment controller (a deployment controller provides declarative updates for pods and replicate sets). The service created or updated by the plugin is configured to point to the pods created based on the pod specifications declared in the deployment controller.
In order to orchestrate a blue/green deployment, the following two service mapping attributes can be leveraged:

1.  **Service Name Override:** Name for the service in Kubernetes. If no override value is specified, the service name in CloudBees CD will be used to name the service in Kubernetes.
2.  **Deployment Name Override:** Name for the deployment in Kubernetes. If no value is specified, then the name of the Kubernetes service being created or updated will be used to name the deployment in Kubernetes.

\
Having these attributes in the service mapping allows you to use the same service deployment and undeployment processes that you would use for performing a more typical service deployment or undeployment. The following steps describe how a blue/green deployment can be orchestrated using the two service mapping attributes mentioned above.

1.  Set *\$\[/myJob/serviceNameOverride\]* as the property reference value for **Service Name Override** and *\$\[/myJob/deploymentNameOverride\]* for **Deployment Name Override**.
    
    ![screenshot](images/BlueGreenDeploymentServiceAttributes.png)
   

2.  Now add *serviceNameOverride* and *deploymentNameOverride* as parameters to your service or application deploy and the undeploy processes.
    The values specified for these parameters will automatically be resolved when the service mapping is used by EC-Kubernetes while performing the deployment. We will use different combination of values for these two parameters to do a blue/green deployment next.\
    
    ![screenshot](images/BlueGreenDeploymentDeployProcessParameters.png)
    
    Similarly, we will use different combination of values for these two parameters when undeploying the service.
    
    ![screenshot](images/BlueGreenDeploymentUndeployProcessParameters.png)
    
    *Note that the processes also have a parameter defined for passing in the container image version to use when deploying the service.*
    

3.  Now that the basic building blocks are in place, lets deploy the very first version of the service with no value set for *serviceNameOverride* and *deploymentNameOverride* set to say \'my-deployment-v1\', where v1 is the image version number. This becomes the \'blue\' deployment or the version that is currently deployed. Now, for any new version that is to be deployed for the service, the deployment pipeline should include the following tasks in order to orchestrate a blue/green deployment:
    
    ![screenshot](images/BlueGreenDeploymentPipeline.png)

    * Create an environment snapshot for the application or the microservice to capture deployment details for the service such as the current version deployed.
    * Now define a task for the \'green\' deployment using the new version to create a deployment for. Use the service name override so that a new deployment is created in kubernetes as well as a new service end-point. The new service end-point can be used to access the \'green\' deployment in order to perform any final tests or verification if required.
    * Now, create another task for the deploy process, this time using the deployment name override but not the service name override. This will result in the original service\'s selector to be updated to target the pods created through the new deployment.
    * Finally, define a \'cleanup blue deployment\' task to undeploy the original \'blue\' deployment as well as the new service end point by specifying both the service name override value and the deployment name override value.
