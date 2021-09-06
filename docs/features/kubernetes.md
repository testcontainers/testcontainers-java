# Kubernetes

Kubernetes support is currently being developed by this fork.

## Proof of concept / Disclaimer
The feature is under ongoing development and therefore very unstable.
Caused by the previously deep integration with Docker, a lot of core functionalities had and have to be refactored or redesigned.

Consider this version to be a proof of concept, which might never progress to a stable state.

## Maven Dependencies
The Marven artifacts are currently available at [https://maven.cluster.lise.de/repository/testcontainers/](https://maven.cluster.lise.de/repository/testcontainers/).
Simply add the additional repository and replace the current version of your Testcontainers dependencies with `kubernetes-alpha0.5`.

Using Maven
```xml
<repository>
    <id>testcontainers-k8s</id>
    <name>testcontainers-k8s</name>
    <url>https://maven.cluster.lise.de/repository/testcontainers</url>
</repository>
<!--...-->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>kubernetes-alpha0.5</scope>
</dependency>
```

Using Gradle
```groovy
repositories {
    maven {
        url "https://maven.cluster.lise.de/repository/testcontainers/"
    }
}

// ...

dependencies {
    testImplementation "org.testcontainers:testcontainers:kubernetes-alpha0.5"
}

```

## Getting started

### Selecting the Kubernetes provider
By default, Testcontainers is going to prefer an existing/detected Docker environment.
If a docker client can not be created, Testcontainers will proceed to search for an available Kubernetes cluster.

The detection is done using the default Kubernetes discovery mechanisms (e.g. In-Cluster configuration, `~/.kube/config`, environment variables). 
See https://github.com/fabric8io/kubernetes-client#configuring-the-client for more details.

You can force testcontainers to use the Kubernetes provider by setting the following environment variable:
```bash
TESTCONTAINERS_PROVIDER_IDENTIFIER = "kubernetes"
```

### Selecting a namespace to be used
If not specified otherwise, the current namespace (as configured within the active Kubernetes context) is used to start every container.
In order to use a different namespace, the environment variable 

`TESTCONTAINERS_PROVIDER_KUBERNETES_NAMESPACE` 

can be used. 
You can also use [environment variable interpolation](#environment-variable-interpolation) using the `${ENV_NAME}`-syntax and a custom `${random}` placeholder, allowing dynamic namespace creation and concurrent executions.
**Note that** creating namespaces and manage resources inside them almost always requires `cluster-admin` permissions.

Example:
```bash
TESTCONTAINERS_PROVIDER_KUBERNETES_NAMESPACE = 'testcontainers-${random}'
```

### Accessing NodePorts
In order to connect to exposed ports, the Kubernetes provider is currently relying on the NodePort mechanism.
Testcontainers automatically tries to obtain a nodes IP address by inspecting the Kuberentes `node` resource.
If this fails or returns an invalid result (e.g. the Node has multiple addresses), the address can be specified manually:

```bash
TESTCONTAINERS_PROVIDER_KUBERNETES_NODEPORT_ADDRESS # e.g "node1.mycluster.tld", "10.0.1.5", ...
```

### Temporary Image Registries
In order to support dynamically built images (e.g. using `ImageFromDockerfile`), the Kubernetes provider will automatically spawn a temporary image registry.
The repository is exposed using an `Ingress` resource, requiring an ingress controller to be configured within you cluster.

The hostname is constructed using the template provided by the
```bash
TESTCONTAINERS_PROVIDER_KUBERNETES_TEMP_REGISTRY_INGRESS_HOST
```
variable, which also supports [environment variable interpolation](#environment-variable-interpolation).

As most container runtimes require registries to be secured by a trusted TLS certificate, you might need to use
tools like `cert-manager` to create the required certificates automatically or create them manually. 
See [Configuration](#configuration) for more options. 


Example configuration using nginx + cert-manger:
```bash
TESTCONTAINERS_PROVIDER_KUBERNETES_TEMP_REGISTRY_INGRESS_HOST = '${random}-registry.my-cluster.tld'
TESTCONTAINERS_PROVIDER_KUBERNETES_TEMP_REGISTRY_INGRESS_ANNOTATIONS = 'kubernetes.io/tls-acme:true,nginx.ingress.kubernetes.io/proxy-body-size:999m'
```

**Be aware** that some ingress controllers are limiting the proxy buffer size, causing the push of larger layers to fail.
This can be solved by increasing the buffer size using the appropriate annotation (e.g `nginx.ingress.kubernetes.io/proxy-body-size`).


## Configuration
The prefix `TESTCONTAINERS_` is omitted for brevity.

| Name | Description | Default | 
| ---- | ----------- | ------- |
| `PROVIDER_KUBERNETES_NAMESPACE` | Selects the namespace to be used. It will be created (and destroyed afterwards), if it doesn't already exist. Supports variable interpolation. | As defined by the current Kubernetes context |
| `PROVIDER_KUBERNETES_NAMESPAE_LABELS` | A list of labels to be added to a namespace created by Testcontainers. | `null` |
| `PROVIDER_KUBERNETES_NAMESPACE_ANNOTATIONS` | A list of annotations to be added to a namespace created by Testcontainers. | `null` |
| `PROVIDER_KUBERNETES_NODEPORT_ADDRESS` | The IP address or hostname to be used in order to connect to exposed NodePorts. | The automatically discovered IP address of a healthy node. |
| `PROVIDER_KUBERNETES_TEMP_REGISTRY_INGRESS_HOST` | The host to be used for the Ingress exposing the temporary registry. Support variable interpolation. | |
| `PROVIDER_KUBERNETES_TEMP_REGISTRY_INGRESS_ANNOTATIONS` | Annotations to be applied to the created Ingress that exposes the temporary registry. | |
| `PROVIDER_KUBERNETES_TEMP_REGISTRY_INGRESS_CERT` | The name of the secret holding the TLS certificate for the registry ingress. | `registry-cert-${random}` |

### Environment variable interpolation
Some configuration variables support the interpolation of environment variables and other placeholders.
This is done using the `${ENV_NAME}` syntax with `ENV_NAME` being the name of the environment variable or placeholder to be interpolated.

Other placeholders are:

| Name | Description |
| ---- | ----------- |
| `RANDOM` / `random` | A short random sequence of lowercase alphanumeric characters.

### Autoconfiguration
During the startup, the provider will also look for a ConfigMap named `config` within the `testcontainers` namespace (if it exists).
If such ConfigMap is found, the configured values are also taken into account.   

## Current status

### Limitations
The current version has the following limitations, which will probably be resolved any time soon.  
- the Kubernetes cluster has to support NodePort services accessible by your machine
- network functionalities are limited to an internal network (containers can speak to each other using their network alias)
and exposed ports
- `ImageFromDockerfile` requires further configuration
- the identity used to connect to the Kubernetes cluster needs to be able to create and delete namespaces and manage almost all namespaced resources
- docker-compose is not implemented
- pulling from private registries is not implemented
- pre-pulling is not implemented


