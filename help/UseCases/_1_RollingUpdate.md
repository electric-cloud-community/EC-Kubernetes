### Rolling Updates

Rolling updates or rolling deployment is a way to deploy a new version with zero downtime by incrementally updating instances running an old version with the new one. In Kubernetes, this is done using rolling updates which allows a *Deployment\'s* update to take place by incrementally updating pods with new ones.

EC-Kubernetes plugin deploys services using rolling updates by default. When CloudBees CD deploys a service to Kubernetes, the EC-Kubernetes plugin uses the following service attributes for the *Deployment*\'s rolling update attributes in Kubernetes.

1.  **Rolling Deployment - Min Microservice Instances:** Minimum number of pods that must be running during a rolling update. Defaults to 1 if not set.
2.  **Rolling Deployment - Max Microservice Instances:** Maximum number of pods that can be running during a rolling update. The incremental number of pods that can be created during the rolling update is the difference between this attribute and the **Number of microservice instances**.

![screenshot](images/RollingDeploymentAttributes.png)
