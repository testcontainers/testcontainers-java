# Webdriver Containers

Testcontainers can be used to automatically instantiate and manage containers that include web browsers, such as those
from SeleniumHQ's [docker-selenium](https://github.com/SeleniumHQ/docker-selenium) project.

## Benefits

* Fully compatible with Selenium 3 & 4 tests for Chrome and Firefox and Selenium 4 tests for Edge, by providing a `RemoteWebDriver` instance
* No need to have specific web browsers, or even a desktop environment, installed on test servers. The only dependency
  is a working Docker installation and your Java JUnit test suite.
* Browsers are always launched from a fixed, clean image. This means no configuration drift from user changes or
  automatic browser upgrades.
* Compatibility between browser version and the Selenium API is assured: a compatible version of the browser docker
  images will be automatically selected to match the version of `selenium-api-*.jar` on the classpath
* Additionally the use of a clean browser prevents leakage of cookies, cached data or other state between tests.
* **VNC screen recording**: Testcontainers can automatically record video of test runs (optionally capturing just
  failing tests)

Creation of browser containers is fast, so it's actually quite feasible to have a totally fresh browser instance for
every test.

## Example

The following field in your JUnit UI test class will prepare a container running Chrome:
<!--codeinclude-->
[Chrome](../../modules/selenium/src/test/java/org/testcontainers/selenium/ChromeWebDriverContainerTest.java) inside_block:junitRule
<!--/codeinclude-->

        
Now, instead of instantiating an instance of WebDriver directly, use the following to obtain an instance inside your
test methods:
<!--codeinclude-->
[RemoteWebDriver](../../modules/selenium/src/test/java/org/testcontainers/selenium/LocalServerWebDriverContainerTest.java) inside_block:getWebDriver
<!--/codeinclude-->

You can then use this driver instance like a regular WebDriver.

Note that, if you want to test a **web application running on the host machine** (the machine the JUnit tests are
running on - which is quite likely), you'll need to use [the host exposing](../features/networking.md#exposing-host-ports-to-the-container) feature of Testcontainers, e.g.:
<!--codeinclude-->
[Open Web Page](../../modules/selenium/src/test/java/org/testcontainers/selenium/LocalServerWebDriverContainerTest.java) inside_block:getPage
<!--/codeinclude-->


## Options

### Other browsers

At the moment, Chrome, Firefox and Edge are supported. To switch, simply change the first parameter to the rule constructor:
<!--codeinclude-->
[Chrome](../../modules/selenium/src/test/java/org/testcontainers/selenium/ChromeWebDriverContainerTest.java) inside_block:junitRule
[Firefox](../../modules/selenium/src/test/java/org/testcontainers/selenium/FirefoxWebDriverContainerTest.java) inside_block:junitRule
[Edge](../../modules/selenium/src/test/java/org/testcontainers/selenium/EdgeWebDriverContainerTest.java) inside_block:junitRule
<!--/codeinclude-->

### Recording videos

By default, no videos will be recorded. However, you can instruct Testcontainers to capture videos for all tests or
just for failing tests.

<!--codeinclude-->
[Record all Tests](../../modules/selenium/src/test/java/org/testcontainers/selenium/ChromeRecordingWebDriverContainerTest.java) inside_block:recordAll
[Record failing Tests](../../modules/selenium/src/test/java/org/testcontainers/selenium/ChromeRecordingWebDriverContainerTest.java) inside_block:recordFailing
<!--/codeinclude-->

Note that the second parameter of `withRecordingMode` should be a directory where recordings can be saved.

By default, the video will be recorded in [FLV](https://en.wikipedia.org/wiki/Flash_Video) format, but you can specify it explicitly or change it to [MP4](https://en.wikipedia.org/wiki/MPEG-4_Part_14) using `withRecordingMode` method with `VncRecordingFormat` option:

<!--codeinclude-->
[Video Format in MP4](../../modules/selenium/src/test/java/org/testcontainers/selenium/ChromeRecordingWebDriverContainerTest.java) inside_block:recordMp4
[Video Format in FLV](../../modules/selenium/src/test/java/org/testcontainers/selenium/ChromeRecordingWebDriverContainerTest.java) inside_block:recordFlv
<!--/codeinclude-->

If you would like to customise the file name of the recording, or provide a different directory at runtime based on the description of the test and/or its success or failure, you may provide a custom recording file factory as follows:
<!--codeinclude-->
[CustomRecordingFileFactory](../../modules/selenium/src/test/java/org/testcontainers/selenium/ChromeRecordingWebDriverContainerTest.java) inside_block:withRecordingFileFactory
<!--/codeinclude-->


Note the factory must implement `org.testcontainers.containers.RecordingFileFactory`.

## More examples

A few different examples are shown in [ChromeWebDriverContainerTest.java](https://github.com/testcontainers/testcontainers-java/blob/main/modules/selenium/src/test/java/org/testcontainers/selenium/ChromeWebDriverContainerTest.java).

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-selenium:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-selenium</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

!!! hint
    Adding this Testcontainers library JAR will not automatically add a Selenium Webdriver JAR to your project. You should ensure that your project also has suitable Selenium dependencies in place, for example:

    === "Gradle"
        ```groovy
        compile "org.seleniumhq.selenium:selenium-remote-driver:3.141.59"
        ```
    
    === "Maven"
        ```xml
        <dependency>
            <groupId>org.seleniumhq.selenium</groupId>
            <artifactId>selenium-remote-driver</artifactId>
            <version>3.141.59</version>
        </dependency>
        ```
    
    Testcontainers will try and match the version of the Dockerized browser to whichever version of Selenium is found on the classpath
