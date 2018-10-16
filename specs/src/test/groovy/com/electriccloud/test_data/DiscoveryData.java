package com.electriccloud.test_data;

import com.electriccloud.procedures.KubernetesTestBase;
import org.testng.annotations.DataProvider;

public class DiscoveryData extends KubernetesTestBase {



    @DataProvider(name = "invalidDiscoveryData")
    public Object[][] discoveryData(){
        return new Object[][] {
                {
                    "", projectName, environmentName, clusterName, "default", clusterEndpoint, clusterToken,
                        "One or more arguments are missing. Please provide the following arguments: projectName"
                },
                {
                    projectName, "", environmentName, clusterName, "default", clusterEndpoint, clusterToken,
                        "One or more arguments are missing. Please provide the following arguments: projectName"
                },
                {
                    projectName, projectName, "", clusterName, "default", clusterEndpoint, clusterToken,
                        "\'environmentName\' must be between 1 and 255 characters"
                },
                {
                    projectName, projectName, environmentName, "", "default", clusterEndpoint, clusterToken,
                        "Please provide the following arguments: clusterName"
                },
                {
                    projectName, projectName, environmentName, clusterName, "", clusterEndpoint, clusterToken,
                        "\'serviceName\' is required and must be between 1 and 253 alphanumeric"
                },
                {
                    "MyTestProject", projectName, environmentName, clusterName, "default", clusterEndpoint, clusterToken,
                        "Project \'MyTestProject\' does not exist"
                }

        };


    }


}
