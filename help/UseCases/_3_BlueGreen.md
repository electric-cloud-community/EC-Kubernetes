## Blue/Green Deployments
<p>Blue/green deployments allow us to deploy 2 versions of an application, the current version and the next to an environment and switch seamlessly between them with zero downtime for the application.</p>
<p>A blue/green deployment can be achieved in Kubernetes by creating a new deployment, say <i>my-deployment-green</i> for running the new version while all the old pods created earlier for the older deployment, say <i>my-deployment-blue</i> are still serving all the live requests. Once the new deployment has completed successfully, and has optionally been tested through a test service, then the service in Kubernetes is switched to send requests to the newly created pods running the new version.</p>
<p>When ElectricFlow deploys a service to Kubernetes, the EC-Kubernetes plugin manages both the service in Kubernetes (the abstraction for a logical set of pods and a policy by which to access them) as well as the deployment controller (a deployment controller provides declarative updates for pods and replicate sets). The service created or updated by the plugin is configured to point to the pods created based on the pod specifications declared in the deployment controller.
<br/>In order to orchestrate a blue/green deployment, the following two service mapping attributes can be leveraged:<br/>
<ol>
    <li><b>Service Name Override: </b>Name for the service in Kubernetes. If no override value is specified, the service name in ElectricFlow will be used to name the service in Kubernetes.</li>
    <li><b>Deployment Name Override: </b>Name for the deployment in Kubernetes. If no value is specified, then the name of the Kubernetes service being created or updated will be used to name the deployment in Kubernetes.</li>
</ol>
<br/>Having these attributes in the service mapping allows you to use the same service deployment and undeployment processes that you would use for performing a more typical service deployment or undeployment. The following steps describe how a blue/green deployment can be orchestrated using the two service mapping attributes mentioned above.

<ol>
    <li>Set <i>$[/myJob/serviceNameOverride]</i> as the property reference value for <b>Service Name Override</b> and <i>$[/myJob/deploymentNameOverride]</i> for <b>Deployment Name Override</b>.<br/>
        <img src="../../plugins/@PLUGIN_KEY@/images/BlueGreenDeploymentServiceAttributes.png" alt="screenshot" /><br/><br/></li>
    <li>Now add <i>serviceNameOverride</i> and <i>deploymentNameOverride</i> as parameters to your service or application deploy and the undeploy processes.<br/>The values specified for these parameters will automatically be resolved when the service mapping is used by EC-Kubernetes while performing the deployment. We will use different combination of values for these two parameters to do a blue/green deployment next.<br/>
        <img src="../../plugins/@PLUGIN_KEY@/images/BlueGreenDeploymentDeployProcessParameters.png" alt="screenshot" /><br/><br/>
        Similarly, we will use different combination of values for these two parameters when undeploying the service.<br/>
        <img src="../../plugins/@PLUGIN_KEY@/images/BlueGreenDeploymentUndeployProcessParameters.png" alt="screenshot" /><br/><br/>
        <i>Note that the processes also have a parameter defined for passing in the container image version to use when deploying the service.</i><br/><br/>
    </li>
    <li>Now that the basic building blocks are in place, lets deploy the very first version of the service with no value set for <i>serviceNameOverride</i> and <i>deploymentNameOverride</i> set to say 'my-deployment-v1', where v1 is the image version number. This becomes the 'blue' deployment or the version that is currently deployed. Now, for any new version that is to be deployed for the service, the deployment pipeline should include the following tasks in order to orchestrate a blue/green deployment:<br/>
        <img src="../../plugins/@PLUGIN_KEY@/images/BlueGreenDeploymentPipeline.png" alt="screenshot" /><br/>
        <p>(1) Create an environment snapshot for the application or the microservice to capture deployment details for the service such as the current version deployed.<br/>
            (2) Now define a task for the 'green' deployment using the new version to create a deployment for. Use the service name override so that a new deployment is created in kubernetes as well as a new service end-point. The new service end-point can be used to access the 'green' deployment in order to perform any final tests or verification if required.<br/>
            (3) Now, create another task for the deploy process, this time using the deployment name override but not the service name override. This will result in the original service's selector to be updated to target the pods created through the new deployment.<br/>
            (4) Finally, define a 'cleanup blue deployment' task to undeploy the original 'blue' deployment as well as the new service end point by specifying both the service name override value and the deployment name override value.<br/>
        </p>
    </li>
</ol>
</p>
