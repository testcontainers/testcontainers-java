# Image name substitution

Testcontainers supports automatic substitution of Docker image names.

This allows replacement of an image name specified in test code with an alternative name - for example, to replace the 
name of a Docker Hub image dependency with an alternative hosted on a private image registry.

This is advisable to avoid [Docker Hub rate limiting](./pull_rate_limiting.md), and some companies will prefer this for policy reasons.

This page describes four approaches for image name substitution:

* [Manual substitution](#manual-substitution) - not relying upon an automated approach
* Using an Image Name Substitutor:
    * Recommended: [Adding a registry URL prefix to image names automatically](#adding-a-registry-url-prefix-to-image-names-automatically)
    * [Developing a custom function for transforming image names on the fly](#developing-a-custom-function-for-transforming-image-names-on-the-fly)
    * [Overriding image names individually in configuration](#overriding-image-names-individually-in-configuration)

It is assumed that you have already set up a private registry hosting [all the Docker images your build requires](./pull_rate_limiting.md#which-images-are-used-by-testcontainers).




## Manual substitution

Consider this if:

* You use only a few images and updating code is not a chore
* All developers and CI machines in your organisation have access to a common registry server
* You also use one of the automated mechanisms to substitute [the images that Testcontainers itself requires](./pull_rate_limiting.md#which-images-are-used-by-testcontainers)

This approach simply entails modifying test code manually, e.g. changing:

For example, you may have a test that uses the `mysql` container image from Docker Hub:

<!--codeinclude--> 
[Direct Docker Hub image name](../examples/junit4/generic/src/test/java/generic/ImageNameSubstitutionTest.java) inside_block:directDockerHubReference
<!--/codeinclude-->

to:

<!--codeinclude--> 
[Private registry image name](../examples/junit4/generic/src/test/java/generic/ImageNameSubstitutionTest.java) inside_block:hardcodedMirror
<!--/codeinclude-->





## Adding a registry URL prefix to image names automatically

Consider this if:

* Developers and CI machines need to use different image names. For example, developers are able to pull images from Docker Hub, but CI machines need to pull from a private registry
* Your private registry has copies of images from Docker Hub where the names are predictable, and just adding a prefix is enough. 
  For example, `registry.mycompany.com/mirror/mysql:8.0.22` can be derived from the original Docker Hub image name (`mysql:8.0.22`) with a consistent prefix string: `registry.mycompany.com/mirror/`

In this case, image name references in code are **unchanged**.
i.e. you would leave as-is:

<!--codeinclude--> 
[Unchanged direct Docker Hub image name](../examples/junit4/generic/src/test/java/generic/ImageNameSubstitutionTest.java) inside_block:directDockerHubReference
<!--/codeinclude-->

You can then configure Testcontainers to apply the prefix `registry.mycompany.com/mirror/` to every image that it tries to pull.
This can be done in one of two ways:

* Setting an environment variable, `TESTCONTAINERS_IMAGE_NAME_PREFIX=registry.mycompany.com/mirror/`
* Via config file, setting `testcontainers.image.name.prefix=registry.mycompany.com/mirror/` in either:
    * the `~/.testcontainers.properties` file in your user home directory, or
    * a file named `testcontainers.properties` on the classpath

Testcontainers will automatically apply this prefix to every image that it pulls - please verify that all [the required images](./pull_rate_limiting.md#which-images-are-used-by-testcontainers) exist in your registry.

Note that the prefix-based substitution will skip applying a prefix if it is already set.
This is intended to help avoid obvious mistakes if image names have been partially migrated to a private image registry via changes to code.




## Developing a custom function for transforming image names on the fly

Consider this if:

* You have complex rules about which private registry images should be used as substitutes, e.g.:
    * non-deterministic mapping of names meaning that a [name prefix](#adding-a-registry-url-prefix-to-image-names-automatically) cannot be used
    * rules depending upon developer identity or location
* or you wish to add audit logging of images used in the build
* or you wish to prevent accidental usage of images that are not on an approved list

In this case, image name references in code are **unchanged**.
i.e. you would leave as-is:

<!--codeinclude--> 
[Unchanged direct Docker Hub image name](../examples/junit4/generic/src/test/java/generic/ImageNameSubstitutionTest.java) inside_block:directDockerHubReference
<!--/codeinclude-->

You can implement a custom image name substitutor by:

* subclassing `org.testcontainers.utility.ImageNameSubstitutor`
* making sure that Testcontainers can find your custom implementation by creating a service loader file. **Do not miss this step!**

The following is an example image substitutor implementation:

<!--codeinclude--> 
[Example Image Substitutor](../examples/junit4/generic/src/test/java/generic/ExampleImageNameSubstitutor.java) block:ExampleImageNameSubstitutor
<!--/codeinclude-->

Testcontainers can be configured to find it at runtime using the Service Loader mechanism.
To do this, create a file on the classpath at `META-INF/services/org.testcontainers.utility.ImageNameSubstitutor` 
containing the full name of your custom image substitutor.

For example:

```text tab="src/main/resources/META-INF/services/org.testcontainers.utility.ImageNameSubstitutor"
com.mycompany.testcontainers.ExampleImageNameSubstitutor
``` 


## Overriding image names individually in configuration

!!! note
    This approach is discouraged and deprecated, but is documented for completeness.
    Overriding individual image names via configuration may be removed in 2021. 

Consider this if:

* You have many references to image names in code and changing them is impractical, and
* None of the other options are practical for you

In this case, image name references in code are left **unchanged**.
i.e. you would leave as-is:

<!--codeinclude--> 
[Unchanged direct Docker Hub image name](../examples/junit4/generic/src/test/java/generic/ImageNameSubstitutionTest.java) inside_block:directDockerHubReference
<!--/codeinclude-->

You can force Testcontainers to substitute in a different image [using a configuration file](./configuration.md), which allows some (but not all) container names to be substituted. 
