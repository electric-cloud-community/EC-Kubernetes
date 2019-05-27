Procedure looks for services and deployments in Kubernetes
YAML file and transfers data into CloudBees Flow. Services found
in YAML file will be mapped to Services objects in CloudBees Flow,
then associated deployments will be found and containers
definitions retrieved from there.

If the configuration contains unsupported fields, these fields will be skipped and a warning message will be emitted to logs.
If the object with the provided name already exists in the CloudBees Flow, this object will be skipped and a warning message will be emitted to logs.
