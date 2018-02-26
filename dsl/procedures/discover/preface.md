Procedure looks for services and deployments on Kubernetes
cluster and transfers data into ElectricFlow. Services found
in cluster will be mapped to Services objects in ElectricFlow,
then associated deployments will be found and containers
definitions retrieved from there.

If the object with the provided name already exists in the ElectricFlow, this object will be skipped and a warning message will be emitted to logs.
