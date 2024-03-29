For communicating with the Kubernetes cluster, you need the following details:

1. Base address of API Endpoint URL
2. Bearer token which has authorization to access API.

    Use the following steps for creating a service account and obtaining the service account bearer token:

    * Download the Kubeconfig file from your Kubernetes cluster. Or if you have direct access to Kubectl shell of the cluster, that will work too.
    * You will need to install Kubectl (http://kubernetes.io/docs/user-guide/prereqs/) or have access to Kubectl shell
    * Create a service account with following kubectl command:

        kubectl create serviceaccount api-user

    * Assign cluster-admin role to serviceaccount api-user. Specify serviceaccount name as default:api-user, if it is created in default namespace.

        kubectl create clusterrolebinding root-cluster-admin-binding --clusterrole=cluster-admin --serviceaccount=default:api-user

    * Get details of the service account we just created, from the output determine name of secret in which data is stored:

        kubectl get serviceaccount api-user -o yaml

    * Assuming name of secret from above step is secret-1234, get details of secret:

        kubectl get secret secret-1234 -o yaml

    * The value of the token field in the output above is base64 encoded. We need to decode it to use in the plugin configuration. On a Unix like system, following command can be used to decode the value:

        echo "<encoded_token_value>" | base64 --decode
