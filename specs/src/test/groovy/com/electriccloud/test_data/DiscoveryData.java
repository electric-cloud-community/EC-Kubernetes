package com.electriccloud.test_data;

import org.testng.annotations.DataProvider;
import static com.electriccloud.procedures.KubernetesTestBase.*;


public class DiscoveryData  {



    @DataProvider(name = "invalidDiscoveryData")
    public Object[][] discoveryData(){
        return new Object[][] {
                {
                    "", projectName, environmentName, clusterName, "default", "One or more arguments are missing. Please provide the following arguments: projectName"
                },
                {
                    projectName, "", environmentName, clusterName, "default", "One or more arguments are missing. Please provide the following arguments: projectName"
                },
                {
                    projectName, projectName, "", clusterName, "default", "\'environmentName\' must be between 1 and 255 characters"
                },
                {
                    projectName, projectName, environmentName, "", "default", "Please provide the following arguments: clusterName"
                },
                {
                    projectName, projectName, environmentName, clusterName, "", "\'serviceName\' is required and must be between 1 and 253 alphanumeric"
                },
                {
                    "MyTestProject", projectName, environmentName, clusterName, "default", "Project \'MyTestProject\' does not exist"
                }

        };


    }


}
