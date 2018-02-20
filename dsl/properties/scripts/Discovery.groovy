public class Discovery extends ServiceFactory {
    def kubeClient
    def pluginConfig
    def accessToken
    def clusterEndpoint

    static final String CREATED_DESCRIPTION = "Created by Container Discovery"

    def Discovery(params) {
        kubeClient = params.kubeClient
        pluginConfig = params.pluginConfig
        accessToken = kubeClient.retrieveAccessToken(pluginConfig)
        clusterEndpoint = pluginConfig.clusterEndpoint
    }

    def discover(namespace) {
        def kubeServices = kubeClient.getServices(clusterEndpoint, namespace, accessToken)
        def efServices = []
        kubeServices.items.each { kubeService ->
            if (!isSystemService(kubeService)) {
                def selector = kubeService.spec.selector.collect { k, v ->
                    k + '=' + v
                }.join(',')

                def deployments = kubeClient.getDeployments(
                    clusterEndpoint,
                    namespace, accessToken,
                    [labelSelector: selector]
                )

                deployments.items.each { deploy ->
                    def efService = buildServiceDefinition(kubeService, deploy, namespace)

                    // TBD
                    // if (deploy.spec.template.spec.imagePullSecrets) {
                    //     def secrets = buildSecretsDefinition(namespace, deploy.spec.template.spec.imagePullSecrets)
                    //     efService.secrets = secrets
                    // }
                    efServices.push(efService)
                }
            }
        }
        efServices
    }

}
