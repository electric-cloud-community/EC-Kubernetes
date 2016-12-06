import java.io.File

procedure 'DeleteConfiguration',
        description: 'Deletes an existing plugin configuration', {

    step 'deleteConfiguration',
            command: new File(pluginDir, 'dsl/procedures/deleteConfiguration/steps/deleteConfiguration.pl').text,
            errorHandling: 'failProcedure',
            exclusiveMode: 'none',
            releaseMode: 'none',
            shell: 'ec-perl',
            timeLimitUnits: 'minutes'

}
