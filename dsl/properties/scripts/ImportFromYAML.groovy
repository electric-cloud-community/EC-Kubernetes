@Grab('org.yaml:snakeyaml:1.19')
import org.yaml.snakeyaml.Yaml

public class ImportFromYAML extends ServiceFactory {
	static final String CREATED_DESCRIPTION = "Created by ImportFromYAML"
	static final File SERVICE_FILE = "service_test.yaml" as File
	static final File DEPLOY_FILE = "deploy_test.yaml" as File

	def simpleImport(namespace){
		Yaml parser = new Yaml()
		def readedService = parser.load(SERVICE_FILE.text)
		def readedDeploy = parser.load(DEPLOY_FILE.text)
		def efServices = []
		def efService = buildServiceDefinition(readedService, readedDeploy, namespace)
		efServices.push(efService)
		efServices
	}
}