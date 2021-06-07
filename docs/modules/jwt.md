# JWT Module

JWT module can be used to automatically instantiate a JWT provider.

More precisely JWT module give you in one hand the possibility to forge a specific JWT token an on the other hand to validate
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
[Initialization](../../modules/jwt/src/test/java/org/testcontainers/containers/jwt/ForgeTokenTest.java) inside_block:initialization
<!--/codeinclude-->

### Forge the token
<!--codeinclude-->
[Forge](../../modules/jwt/src/test/java/org/testcontainers/containers/jwt/ForgeTokenTest.java) inside_block:forgeToken
<!--/codeinclude-->

The token is forge with the issuer in the container.

You can inspect the content of the forged token on: [Jwt.io](https://jwt.io)

The forged token reference your container as issuer.

### Stop the Container
<!--codeinclude-->
[Shutdown](../../modules/jwt/src/test/java/org/testcontainers/containers/jwt/ForgeTokenTest.java) inside_block:shutdown
<!--/codeinclude-->







        

