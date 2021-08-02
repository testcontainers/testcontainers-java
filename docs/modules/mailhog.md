# Mailhog Module

[MailHog](https://github.com/mailhog/MailHog/) is a mail server/testing tool that simply collects mails (instead of delivering them) and offers a web interface to view all mails that have been sent. It also offers an API to retrieve these mails. This makes it possible to use it in integration tests for applications that send mails.

## Usage example

This is how you create a "mocked mail server" in a test:

<!--codeinclude-->
[Creating a MailHog container](../../modules/mailhog/src/test/java/org/testcontainers/containers/MailHogContainerTest.java) inside_block:createContainer
<!--/codeinclude-->

You then configure you mail server with `mailHog.getContainerIpAddress()` and `mailHog.getSmtpPort()`. After sending mails, you can retrieve them from the API:

<!--codeinclude-->
[Getting all mails from MailHog](../../modules/mailhog/src/test/java/org/testcontainers/containers/MailHogContainerTest.java) inside_block:exampleGetAllMails
<!--/codeinclude-->

Or check if there is a mail from a specific sender:

<!--codeinclude-->
[Getting the newest mail of a specific sender](../../modules/mailhog/src/test/java/org/testcontainers/containers/MailHogContainerTest.java) inside_block:exampleGetMail
<!--/codeinclude-->

Using `MailhogContainer.getMailsWithParameters()` you can also directly query the MailHog API. See [this document](https://github.com/mailhog/MailHog/blob/master/docs/APIv2/swagger-2.0.yaml) for details on the possible parameters.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:mailhog:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mailhog</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
