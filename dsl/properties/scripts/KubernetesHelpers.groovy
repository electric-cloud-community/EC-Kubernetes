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

    String getApiVersion() {
        kubernetesObject.apiVersion
    }

    String getKind() {
        kubernetesObject.kind
    }

    String getName() {
        kubernetesObject.metadata.name
    }

    String getNamespace() {
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
    String apiGroupVersion

    ApiGroupVersionInsider(String apiGroupVersion) {
        this.apiGroupVersion = apiGroupVersion
    }

    String getApiGroupVersion() {
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

    String getKind() {
        apiResourceList.kind
    }

    String getGroupVersion() {
        apiResourceList.groupVersion
    }

    List getApiResources() {
        apiResourceList.resources
    }

    ApiGroupVersionInsider getApiGroupVersionInsider() {
        return new ApiGroupVersionInsider(getGroupVersion())
    }

    ApiResourceInsider getApiResourceInsiderByObjectKind(String kind) throws NoApiResourceForKubernetesObjectException {
        //todo: add some caching here
        def apiResource = getApiResources().find {
            it.kind == kind && it.name != ~"/"
        }
        if (!apiResource) {
            throw new NoApiResourceForKubernetesObjectException("Cannot find corresponded API resource for Kubernetes object kind '$kind'")
        }
        return new ApiResourceInsider(apiResource)
    }

    String getUriPathForObjectGet(String kind, String name, String namespace = null)
            throws NoApiResourceForKubernetesObjectException, ApiOperationIsNotSupportedForKubernetesObjectException {
        ApiResourceInsider apiResourceInsider = getApiResourceInsiderByObjectKind(kind)
        if (apiResourceInsider.canGet()) {
            return getRootUriPathForApiResource(apiResourceInsider, namespace) + "/$name"
        } else {
            throw new ApiOperationIsNotSupportedForKubernetesObjectException("Operation 'get' is not supported for Kubernetes object kind '$kind'")
        }
    }

    String getUriPathForObjectCreate(String kind, String name, String namespace = null)
            throws NoApiResourceForKubernetesObjectException, ApiOperationIsNotSupportedForKubernetesObjectException {
        ApiResourceInsider apiResourceInsider = getApiResourceInsiderByObjectKind(kind)
        if (apiResourceInsider.canCreate()) {
            return getRootUriPathForApiResource(apiResourceInsider, namespace)
        } else {
            throw new ApiOperationIsNotSupportedForKubernetesObjectException("Operation 'create' is not supported for Kubernetes object kind '$kind'")
        }
    }

    String getUriPathForObjectUpdate(String kind, String name, String namespace = null)
            throws NoApiResourceForKubernetesObjectException, ApiOperationIsNotSupportedForKubernetesObjectException {
        ApiResourceInsider apiResourceInsider = getApiResourceInsiderByObjectKind(kind)
        if (apiResourceInsider.canUpdate()) {
            return getRootUriPathForApiResource(apiResourceInsider, namespace) + "/$name"
        } else {
            throw new ApiOperationIsNotSupportedForKubernetesObjectException("Operation 'update' is not supported for Kubernetes object kind '$kind'")
        }
    }

    String getUriPathForObjectPatch(String kind, String name, String namespace = null)
            throws NoApiResourceForKubernetesObjectException, ApiOperationIsNotSupportedForKubernetesObjectException {
        ApiResourceInsider apiResourceInsider = getApiResourceInsiderByObjectKind(kind)
        if (apiResourceInsider.canPatch()) {
            return getRootUriPathForApiResource(apiResourceInsider, namespace) + "/$name"
        } else {
            throw new ApiOperationIsNotSupportedForKubernetesObjectException("Operation 'patch' is not supported for Kubernetes object kind '$kind'")
        }
    }

    String getRootUriPathForApiResource(ApiResourceInsider apiResourceInsider, String namespace = null) {
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

    String getName() {
        apiResource.name
    }

    String getKind() {
        apiResource.kind
    }

    boolean isNamespaced() {
        apiResource.namespaced
    }

    List<String> getVerbs() {
        apiResource.verbs
    }

    boolean canGet() {
        getVerbs().contains("get")
    }

    boolean canCreate() {
        getVerbs().contains("create")
    }

    boolean canUpdate() {
        getVerbs().contains("update")
    }

    boolean canPatch() {
        getVerbs().contains("patch")
    }
}

@InheritConstructors
class KubernetesApiException extends PluginException {}

@InheritConstructors
class NoApiResourceForKubernetesObjectException extends KubernetesApiException {}

@InheritConstructors
class ApiOperationIsNotSupportedForKubernetesObjectException extends KubernetesApiException {}