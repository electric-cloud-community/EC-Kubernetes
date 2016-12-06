import java.io.File

procedure 'CreateConfiguration',
        description: 'Creates a configuration for Google Container Engine', {

    step 'createConfiguration',
            command: new File(pluginDir, 'dsl/procedures/createConfiguration/steps/createConfiguration.pl').text,
            errorHandling: 'failProcedure',
            exclusiveMode: 'none',
            postProcessor: 'postp',
            releaseMode: 'none',
            shell: 'ec-perl',
            timeLimitUnits: 'minutes'

    step 'createAndAttachCredential',
        command: new File(pluginDir, 'dsl/procedures/createConfiguration/steps/createAndAttachCredential.pl').text,
        errorHandling: 'failProcedure',
        exclusiveMode: 'none',
        releaseMode: 'none',
        shell: 'ec-perl',
        timeLimitUnits: 'minutes'

}
