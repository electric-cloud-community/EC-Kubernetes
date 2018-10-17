package com.electriccloud.test_data;

import com.electriccloud.procedures.KubernetesTestBase;
import org.testng.annotations.DataProvider;
import static com.electriccloud.procedures.KubernetesTestBase.*;


public class ImportData  {


    @DataProvider(name = "invalidImportData")
    public Object[][] getImportData(){
        return new Object[][]{
                {
                    serviceName, projectName, environmentName, "", false, null,
                        "Either specify all the parameters required to identify the Kubernetes-backed ElectricFlow cluster"
                },
                {
                    serviceName, projectName, environmentName, "my-cluster", false, null,
                        "Cluster \'my-cluster\' does not exist in \'" + environmentName +"\' environment!"
                },
                {
                    serviceName, projectName, "", clusterName, false, null,
                        "Either specify all the parameters required to identify the Kubernetes-backed ElectricFlow cluster"
                },
                {
                    serviceName, projectName, "my-environment", clusterName, false, null,
                        "Environment \'my-environment\' does not exist in project \'" + projectName + "\'"
                },
                {
                    serviceName, "Default", environmentName, clusterName, false, null,
                        "Environment \'" + environmentName + "\' does not exist in project \'Default\'"
                },
                {
                    "nginx-service-invalid", projectName, environmentName, clusterName, false, null,
                        "ERROR: Failed to read the Docker Compose file contents"
                },
                {
                    applicationName, projectName, environmentName, "", true, applicationName,
                        "Either specify all the parameters required to identify the Kubernetes-backed ElectricFlow cluster"
                },
                {
                    applicationName, projectName, environmentName, "my-cluster", true, applicationName,
                        "Cluster \'my-cluster\' does not exist in \'" + environmentName + "\' environment!"
                },
                {
                    applicationName, projectName, "", clusterName, true, applicationName,
                        "Either specify all the parameters required to identify the Kubernetes-backed ElectricFlow cluster"
                },
                {
                    applicationName, projectName, "my-environment", clusterName, true, applicationName,
                        "Environment \'my-environment\' does not exist in project \'" + projectName + "\'"
                },
                {
                    applicationName, "Default", environmentName, clusterName, true, applicationName,
                        "Environment \'" + environmentName + "\' does not exist in project \'Default\'"
                },
                {
                    "nginx-service-invalid", projectName, environmentName, clusterName, true, applicationName,
                        "ERROR: Failed to read the Docker Compose file contents"
                },
                {
                    applicationName, projectName, environmentName, clusterName, true, "",
                        "Application name is required for creating application-scoped microservices"
                }
        };
    }




}
