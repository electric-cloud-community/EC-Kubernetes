## Rolling Updates
<p>Rolling updates or rolling deployment is a way to deploy a new version with zero downtime by incrementally updating instances running an old version with the new one. In Kubernetes, this is done using rolling updates which allows a <i>Deployment's</i> update to take place by incrementally updating pods with new ones.</p>
<p>EC-Kubernetes plugin deploys services using rolling updates by default. When CloudBees Flow deploys a service to Kubernetes, the EC-Kubernetes plugin uses the following service attributes for the <i>Deployment</i>'s rolling update attributes in Kubernetes.</p>
<p>
    <ol>
        <li><b>Rolling Deployment - Min Microservice Instances: </b>Minimum number of pods that must be running during a rolling update. Defaults to 1 if not set.</li>
        <li><b>Rolling Deployment - Max Microservice Instances: </b>Maximum number of pods that can be running during a rolling update. The incremental number of pods that can be created during the rolling update is the difference between this attribute and the <b>Number of microservice instances</b>.</li>
    </ol>
</p>
<p>
    <img src="../../plugins/@PLUGIN_KEY@/images/RollingDeploymentAttributes.png" alt="screenshot" />
</p>
