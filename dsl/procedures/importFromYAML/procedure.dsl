import java.io.File

procedure 'Import Microservices',
    description: '''<html>
    Create microservices in ElectricFlow by importing a Kubernetes YAML file containing services and deployment configurations.
    <div>
        <ol>
            <li><b>Copy and enter the content of your Kubernetes YAML file</b></li>
            <li><b>Determine how the new microservices will be created in ElectricFlow</b>
            <ul>
                <li><b>Create the microservices individually at the top-level within the project.</b> All microservices will be created at the top-level. Enter the following parameters:
                <ul>
                    <li>Project Name: Enter the name of the project where the microservices will be created</li>
                </ul></li>
                <li><b>Create the Microservices within an application in ElectricFlow.</b> All microservices will be created as services within a new application. Enter the following parameters:
                <ul>
                    <li>Project Name: Enter the name of the project where the new application will be created</li>
                    <li>Create Microservices within and Application: Select the checkbox</li>
                    <li>Application Name:  The name of a new application which will be created in ElectricFlow containing the new services.</li>
                </ul>
                </li>
            </ul></li>
            <li><b>Optionally map the services to an existing Environment Cluster</b> Select an existing Environment that contains a cluster with Kubernetes configuration details where the new microservices can be deployed. Enter the following parameters:
            <ul>
                <li>Environment Project Name: The project containing the ElectricFlow environment where the services will be deployed.</li>
                <li>Environment Name: The name of the existing environment that contains a cluster where the newly created microservice(s) will be deployed.</li>
                <li>Cluster Name: The name of an existing EC-Kubernetes (or EC-AzureContainerService) backed cluster in the environment above where the newly created microservice(s) will be deployed.</li>
            </ul>
            </li>
        </ol>
    </div>
    </html>''',
    {

    //Using a simple description for use with the step picker since it cannot handle HTML content
    property 'stepPickerDescription',
        value: 'Create microservices in ElectricFlow by importing a Kubernetes YAML file containing services and deployment configurations.'

    step 'setup',
          subproject: '',
          subprocedure: 'Setup',
          command: null,
          errorHandling: 'failProcedure',
          exclusiveMode: 'none',
          postProcessor: 'postp',
          releaseMode: 'none',
          timeLimitUnits: 'minutes', {
          actualParameter 'additionalArtifactVersion', ''
   }

    step 'import',
          command: new File(pluginDir, 'dsl/procedures/importFromYAML/steps/import.groovy').text,
          errorHandling: 'failProcedure',
          exclusiveMode: 'none',
          postProcessor: 'postp',
          releaseMode: 'none',
          resourceName: '$[grabbedResource]',
          shell: 'ec-groovy',
          timeLimitUnits: 'minutes'
}
