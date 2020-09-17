### Canary Deployments

Canary deployment is a way of sending out a new release into production that plays the role of a \"canary\" to get an idea of how a new release will perform before rolling it out to all the users.

A canary deployment consists of rolling out a new release or a new functionality to a subset of users or servers. This can be achieved in a Kubernetes cluster by deploying a canary of a new release side by side with the previous release so that the new release can receive live production traffic before fully rolling it out. (Reference: [Canary Deployments](https://kubernetes.io/docs/concepts/cluster-administration/manage-deployment/#canary-deployments)).

When CloudBees CD deploys a service to Kubernetes, the EC-Kubernetes plugin manages both the service in Kubernetes (the abstraction for a logical set of pods and a policy by which to access them) as well as the deployment controller (a deployment controller provides declarative updates for pods and replicate sets). The service created or updated by the plugin is configured to point to the pods created based on the pod specifications declared in the deployment controller. In order to perform a canary deployment, the following two service mapping attributes can be set:

1.  **Perform Canary Deployment:** If true, then a canary deployment will be performed. Any previous deployment will remain unchanged in the namespace allowing this canary deployment to receive live traffic side by side with the previous deployment.
2.  **Number of Canary Replicas:** The number of replicas to create if performing a canary deployment. Defaults to 1 replica for the canary deployment.


Having these attributes in the service mapping allows you to use the same service deployment and undeployment processes that you would use for performing a more typical service deployment or undeployment. The following steps describe how this can be achieved.

1.  Set *\$\[/myJob/canaryDeployment\]* as the property reference value for **Perform Canary Deployment** and *\$\[/myJob/canaryReplicas\]* for **Number of Canary Replicas**.
    
    ![screenshot](images/CanaryDeploymentServiceAttributes.png)
    

2.  Now add *canaryDeployment* and *canaryReplicas* as parameters to your service or application deploy and the undeploy processes.\
    The values specified for these parameters will automatically be resolved when the service mapping is used by EC-Kubernetes while performing the deployment. If *canaryDeployment* is set to true, then a canary deployment will be performed.\
    ![screenshot](images/CanaryDeploymentDeployProcessParameters.png)\
    Similarly, during undeploy process through *Undeploy Service* procedure, if *canaryDeployment* is set to true, then the canary deployment will be removed without impacting the previous deployment or the service. 
    ![screenshot](../../plugins/@PLUGIN_KEY@/images/CanaryDeploymentUndeployProcessParameters.png)
    

3.  Finally tie the deploy and undeploy processes into an end-to-end service release pipeline including managing the canary deployments and rolling out new releases.
    ![screenshot](images/CanaryDeploymentPipeline.png)

    * Define a \'deploy canary\' task to perform a canary deployment using the specified version on the same environment where the service is targeted.
    
    * After the canary deployment is pushed out to the environment, side by side with the previous release, use a manual task to approve the roll out of the new version if it is confirmed that the \'canary deployment is safe\' and good to be rolled out.
    * Once the canary deployment is confirmed to be safe and the pipeline is allowed to continue, define a \'deploy new service version\' task to deploy the new version of the service.
    * Finally, regardless of the canary deployment\'s result, define an \'undeploy the canary\' task to undeploy the canary deployment.
