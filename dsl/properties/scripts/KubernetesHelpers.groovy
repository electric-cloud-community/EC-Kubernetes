import groovy.json.JsonBuilder
import groovy.transform.InheritConstructors

class KubernetesObjectInsider {
    def kubernetesObject

    KubernetesObjectInsider(def kubernetesObject) {
        this.kubernetesObject = kubernetesObject
    }

    def getKubernetesObject() {
        kubernetesObject
    }

    def getApiVersion() {
        kubernetesObject.apiVersion
    }

    def getKind() {
        kubernetesObject.kind
    }

    def getName() {
        kubernetesObject.metadata.name
    }

    def getNamespace() {
        kubernetesObject.metadata.namespace
    }

    ApiGroupVersionInsider getApiGroupVersionInsider() {
        return new ApiGroupVersionInsider(getApiVersion())
    }

    def getKubernetesObjectJson() {
        return (new JsonBuilder(kubernetesObject)).toPrettyString()
    }
}

class ApiGroupVersionInsider {
    def apiGroupVersion

    ApiGroupVersionInsider(def apiGroupVersion) {
        this.apiGroupVersion = apiGroupVersion
    }

    def getApiGroupVersion() {
        return apiGroupVersion
    }

    boolean isCoreGroupApi() {
        return getApiGroupVersion() == "v1"
    }

    String getUriPathRoot() {
        return isCoreGroupApi() ? "/api" : "/apis"
    }

    String getUriPathRootWithApiVersion() {
        return getUriPathRoot() + "/" + getApiGroupVersion()
    }
}

class ApiResourceListInsider {
    def apiResourceList

    ApiResourceListInsider(def apiResourceList) {
        this.apiResourceList = apiResourceList
    }

    def getKind() {
        apiResourceList.kind
    }

    def getGroupVersion() {
        apiResourceList.groupVersion
    }

    def getApiResources() {
        apiResourceList.resources
    }

    ApiGroupVersionInsider getApiGroupVersionInsider() {
        return new ApiGroupVersionInsider(getGroupVersion())
    }

    ApiResourceInsider getApiResourceInsiderByObjectKind(def kind) throws NoApiResourceForKubernetesObjectException {
        //todo: add some caching here
        def apiResource = getApiResources().find {
            it.kind == kind && it.name != ~"/"
        }
        if (!apiResource) {
            throw new NoApiResourceForKubernetesObjectException("Cannot find corresponded API resource for Kubernetes object kind '$kind'")
        }
        return new ApiResourceInsider(apiResource)
    }

    def getUriPathForObjectGet(def kind, def name, def namespace = null)
            throws NoApiResourceForKubernetesObjectException, ApiOperationIsNotSupportedForKubernetesObjectException {
        ApiResourceInsider apiResourceInsider = getApiResourceInsiderByObjectKind(kind)
        if (apiResourceInsider.canGet()) {
            return getRootUriPathForApiResource(apiResourceInsider, namespace) + "/$name"
        } else {
            throw new ApiOperationIsNotSupportedForKubernetesObjectException("Operation 'get' is not supported for Kubernetes object kind '$kind'")
        }
    }

    String getUriPathForObjectCreate(def kind, def name, def namespace = null)
            throws NoApiResourceForKubernetesObjectException, ApiOperationIsNotSupportedForKubernetesObjectException {
        ApiResourceInsider apiResourceInsider = getApiResourceInsiderByObjectKind(kind)
        if (apiResourceInsider.canCreate()) {
            return getRootUriPathForApiResource(apiResourceInsider, namespace)
        } else {
            throw new ApiOperationIsNotSupportedForKubernetesObjectException("Operation 'create' is not supported for Kubernetes object kind '$kind'")
        }
    }

    String getUriPathForObjectUpdate(def kind, def name, def namespace = null)
            throws NoApiResourceForKubernetesObjectException, ApiOperationIsNotSupportedForKubernetesObjectException {
        ApiResourceInsider apiResourceInsider = getApiResourceInsiderByObjectKind(kind)
        if (apiResourceInsider.canUpdate()) {
            return getRootUriPathForApiResource(apiResourceInsider, namespace) + "/$name"
        } else {
            throw new ApiOperationIsNotSupportedForKubernetesObjectException("Operation 'update' is not supported for Kubernetes object kind '$kind'")
        }
    }

    String getRootUriPathForApiResource(ApiResourceInsider apiResourceInsider, def namespace = null) {
        def resourceName = apiResourceInsider.getName()
        if (apiResourceInsider.isNamespaced()) {
            namespace = namespace ?: "default"
            return getApiGroupVersionInsider().getUriPathRootWithApiVersion() + "/namespaces/$namespace/$resourceName"
        } else {
            return getApiGroupVersionInsider().getUriPathRootWithApiVersion() + "/$resourceName"
        }
    }
}

class ApiResourceInsider {
    def apiResource

    ApiResourceInsider(def apiResource) {
        this.apiResource = apiResource
    }

    def getApiResource() {
        apiResource
    }

    def getName() {
        apiResource.name
    }

    def getKind() {
        apiResource.kind
    }

    def isNamespaced() {
        apiResource.namespaced
    }

    List<String> getVerbs() {
        apiResource.verbs
    }

    def canGet() {
        getVerbs().contains("get")
    }

    def canCreate() {
        getVerbs().contains("create")
    }

    def canUpdate() {
        getVerbs().contains("update")
    }
}

@InheritConstructors
class KubernetesApiException extends PluginException {}

@InheritConstructors
class NoApiResourceForKubernetesObjectException extends KubernetesApiException {}

@InheritConstructors
class ApiOperationIsNotSupportedForKubernetesObjectException extends KubernetesApiException {}