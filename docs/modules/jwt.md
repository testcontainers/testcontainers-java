# Jwt Containers

Jwt module can be used to automatically instantiate a jwt provider.

More precisely Jwt module give you in one hand the possibility to generate a specific JWT token an on the other hand to validate
this token.

## Benefits

* Running a JWT provider for stubbing OAuth security

## Example

This example show you how to use this module

### Declare the Container
<!--codeinclude-->
[Declaration](../../modules/jwt/src/test/java/org/testcontainers/containers/jwt/ForgeTokenTest.java) inside_block:declaration
<!--/codeinclude-->

### Start the Container
<!--codeinclude-->
[Initialization](../../modules/jwt/src/test/java/org/testcontainers/containers/jwt/ForgeTokenTest.java) inside_block:junitBefore
<!--/codeinclude-->

### Forge the token
<!--codeinclude-->
[Forge](../../modules/jwt/src/test/java/org/testcontainers/containers/jwt/ForgeTokenTest.java) inside_block:forge
<!--/codeinclude-->

The token is forge with the issuer in the container.
You can inspect the content of the forged token on: [Jwt.io](https://jwt.io)
The forged token reference your container as issuer.

### Stop the Container
<!--codeinclude-->
[Clean](../../modules/jwt/src/test/java/org/testcontainers/containers/jwt/ForgeTokenTest.java) inside_block:junitAfter
<!--/codeinclude-->







        

