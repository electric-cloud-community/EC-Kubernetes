package com.electriccloud.test_data;

import com.electriccloud.helpers.enums.LogLevels;
import org.testng.annotations.DataProvider;
import static com.electriccloud.helpers.enums.LogLevels.LogLevel.*;
import static com.electriccloud.procedures.KubernetesTestBase.*;

public class ConfigurationData {


    @DataProvider(name = "logLevels")
    public Object[][] logLevelsData(){
        return new Object[][]{
                {DEBUG, "logger DEBUG", "[DEBUG]", "[ERROR]"},
                {INFO, "logger INFO", "[INFO]", "[DEBUG]"},
                {WARNING, "logger WARNING", "[INFO]", "[DEBUG]"},
                {ERROR, "logger ERROR", "[INFO]", "[DEBUG]"}
        };
    }

    @DataProvider(name = "clusterVersions")
    public Object[][] clusterVersionsData(){
        return new Object[][]{
                { "1.5" },
                { "1.6" },
                { "1.7" },
                { "1.8" },
                { "1.9" }
        };
    }



    @DataProvider(name = "invalidConfigData")
    public Object[][] invalidConfigurationData(){
        return new Object[][]{
                {
                    " ", clusterEndpoint, adminAccount, clusterToken, clusterVersion, "/api/v1/namespaces", "Error creating configuration credential: 'credentialName' is required and must be between 1 and 255 characters"
                },
                {
                    configName, " ", adminAccount, clusterToken, clusterVersion, "/api/v1/namespaces", "java.lang.IllegalStateException: Target host is null"
                },
                {
                    configName, clusterEndpoint, "", "", clusterVersion,  "/api/v1/namespaces", "Kubernetes cluster at " + clusterEndpoint + " was not reachable."
                },
                {
                    clusterToken, clusterEndpoint, adminAccount, clusterToken, clusterVersion,  "/api/v1/namespaces", "Error creating configuration credential: 'credentialName' is required and must be between 1 and 255 characters"
                },
                {
                    configName, "https://35.188.101.83", adminAccount, clusterToken, clusterVersion, "/api/v1/namespaces", "java.net.ConnectException: Connection timed out (Connection timed out)"
                },
                {
                    configName, clusterEndpoint, adminAccount, "test", clusterVersion, "/api/v1/namespaces", "Kubernetes cluster at " + clusterEndpoint + " was not reachable."
                },
                {
                    configName, clusterEndpoint, adminAccount, clusterToken, clusterVersion, "/api/v1/test",  "Kubernetes cluster at " + clusterEndpoint + " was not reachable."
                }
        };
    }




    @DataProvider(name = "invalidEditConfigData")
    public Object[][] getInvalidData(){
        return new Object[][]{
                {
                    clusterEndpoint,  adminAccount, clusterToken, clusterVersion, true, "/api/v1/test", LogLevels.LogLevel.DEBUG, "ERROR: Kubernetes cluster at " + clusterEndpoint + " was not reachable. Health check (#2) at " + clusterEndpoint + "/api/v1/test failed with HTTP/1.1 404 Not Found"
                },
                {
                    "", adminAccount, clusterToken, clusterVersion, true, "/api/v1/namespaces", LogLevels.LogLevel.DEBUG, "java.lang.IllegalStateException: Target host is null"
                },
                {
                    "https://35.188.101.83", adminAccount, clusterToken, clusterVersion, true, "/api/v1/namespaces", LogLevels.LogLevel.DEBUG, "java.net.ConnectException: Connection timed out (Connection timed out)"
                },
                {
                    clusterEndpoint, "", "", clusterVersion, true, "/api/v1/namespaces", LogLevels.LogLevel.DEBUG, "ERROR: Kubernetes cluster at " + clusterEndpoint + " was not reachable. Health check (#2) at " + clusterEndpoint + "/api/v1/namespaces failed with HTTP/1.1 403 Forbidden"
                },
                {
                    clusterEndpoint, adminAccount, "test", clusterVersion, true, "/api/v1/namespaces", LogLevels.LogLevel.DEBUG, "Kubernetes cluster at " + clusterEndpoint + " was not reachable."
                }
        };
    }


    @DataProvider(name = "invalidProvisionData")
    public Object[][] ivalidProvisioningData(){
        return new Object[][]{
                {
                        "test", environmentName, clusterName, "NoSuchEnvironment: Environment '" + environmentName +"' does not exist in project 'test'"
                },
                {
                        "Default", environmentName, clusterName, "NoSuchEnvironment: Environment '" + environmentName + "' does not exist in project 'Default'"
                },
                {
                        projectName, "test", clusterName, "NoSuchEnvironment: Environment 'test' does not exist in project '" + projectName + "'"
                },
                {
                        projectName, environmentName, "test-cluster", "NoSuchCluster: Cluster 'test-cluster' does not exist in environment '" + environmentName + "'"
                }
        };
    }





}
