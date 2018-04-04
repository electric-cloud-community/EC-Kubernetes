import java.io.File

procedure 'Import Microservices',
    description: 'Creating microservice models using Kubernetes YAML file. ',
    {

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
