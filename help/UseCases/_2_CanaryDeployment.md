## Canary Deployments
<p>Canary deployment is a way of sending out a new release into production that plays the role of a "canary" to get an idea of how a new release will perform before rolling it out to all the users.</p>
<p>A canary deployment consists of rolling out a new release or a new functionality to a subset of users or servers. This can be achieved in a Kubernetes cluster by deploying a canary of a new release side by side with the previous release so that the new release can receive live production traffic before fully rolling it out. (Reference: <a href="https://kubernetes.io/docs/concepts/cluster-administration/manage-deployment/#canary-deployments" target="_blank">Canary Deployments</a>).</p>
<p>When ElectricFlow deploys a service to Kubernetes, the EC-Kubernetes plugin manages both the service in Kubernetes (the abstraction for a logical set of pods and a policy by which to access them) as well as the deployment controller (a deployment controller provides declarative updates for pods and replicate sets). The service created or updated by the plugin is configured to point to the pods created based on the pod specifications declared in the deployment controller.
    In order to perform a canary deployment, the following two service mapping attributes can be set:<br/>
    <ol>
        <li><b>Perform Canary Deployment: </b>If true, then a canary deployment will be performed. Any previous deployment will remain unchanged in the namespace allowing this canary deployment to receive live traffic side by side with the previous deployment.</li>
        <li><b>Number of Canary Replicas: </b>The number of replicas to create if performing a canary deployment. Defaults to 1 replica for the canary deployment.</li>
    </ol>
    <br/>Having these attributes in the service mapping allows you to use the same service deployment and undeployment processes that you would use for performing a more typical service deployment or undeployment. The following steps describe how this can be achieved.
    <ol>
        <li>Set <i>$[/myJob/canaryDeployment]</i> as the property reference value for <b>Perform Canary Deployment</b> and <i>$[/myJob/canaryReplicas]</i> for <b>Number of Canary Replicas</b>.<br/>
            <img src="../../plugins/@PLUGIN_KEY@/images/CanaryDeploymentServiceAttributes.png" alt="screenshot" /><br/><br/></li>
        <li>Now add <i>canaryDeployment</i> and <i>canaryReplicas</i> as parameters to your service or application deploy and the undeploy processes.<br/>The values specified for these parameters will automatically be resolved when the service mapping is used by EC-Kubernetes while performing the deployment. If <i>canaryDeployment</i> is set to true, then a canary deployment will be performed.<br/>
            <img src="../../plugins/@PLUGIN_KEY@/images/CanaryDeploymentDeployProcessParameters.png" alt="screenshot" /><br/>
            Similarly, during undeploy process through <i>Undeploy Service</i> procedure, if <i>canaryDeployment</i> is set to true, then the canary deployment will be removed without impacting the previous deployment or the service.
            <img src="../../plugins/@PLUGIN_KEY@/images/CanaryDeploymentUndeployProcessParameters.png" alt="screenshot" /><br/><br/>
        </li>
        <li>Finally tie the deploy and undeploy processes into an end-to-end service release pipeline including managing the canary deployments and rolling out new releases.<br/>
            <img src="../../plugins/@PLUGIN_KEY@/images/CanaryDeploymentPipeline.png" alt="screenshot" /><br/>
            <p>(1) Define a 'deploy canary' task to perform a canary deployment using the specified version on the same environment where the service is targeted.<br/>
            (2) After the canary deployment is pushed out to the environment, side by side with the previous release, use a manual task to approve the roll out of the new version if it is confirmed that the 'canary deployment is safe' and good to be rolled out.<br/>
            (3) Once the canary deployment is confirmed to be safe and the pipeline is allowed to continue, define a 'deploy new service version' task to deploy the new version of the service.<br/>
            (4) Finally, regardless of the canary deployment's result, define an 'undeploy the canary' task to undeploy the canary deployment.<br/>
            </p>
        </li>
    </ol>

</p>

