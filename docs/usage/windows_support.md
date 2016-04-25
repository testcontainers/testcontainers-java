# Windows Support (Alpha)

## Prerequisites
* [Docker Toolbox for Windows](https://docs.docker.com/engine/installation/windows/) needs to be installed
* Currently Docker for Windows without Toolbox (Linux VM) is not supported

## Project Setup
Due to the early state of windows support and lack of CI, Testcontainers for Windows is only integrated in the 
windows-support branch. If you want to use this branch then follow the 
[Jitpack project setup](../index.md#jitpack-unreleased-versions) and use the following configuration:

	<dependency>
	    <groupId>com.github.testcontainers</groupId>
	    <artifactId>testcontainers-java</artifactId>
	    <version>windows-support-SNAPSHOT</version>
	</dependency>
	
## Limitations
The following features are not available or do not work correctly so make sure you do not use them or use them with 
caution. The list may not be complete.

### MySQL TestContainer
* MySQL server prevents custom configuration file (ini-script) from being loaded due to security measures ([link to feature description](database_containers.md#using-an-init-script))

## Reporting issues

Please report any issues with the Windows build of Testcontainers [here](https://github.com/testcontainers/testcontainers-java/issues)
and be sure to note that you are using this on Windows.