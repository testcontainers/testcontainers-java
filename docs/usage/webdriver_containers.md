# Webdriver Containers

Test Containers can be used to automatically instantiate and manage containers that include web browsers, such as those
from SeleniumHQ's [docker-selenium](https://github.com/SeleniumHQ/docker-selenium) project.

## Benefits

* Fully compatible with Selenium 2/Webdriver tests, by providing a `RemoteWebDriver` instance
* No need to have specific web browsers, or even a desktop environment, installed on test servers. The only dependency
  is a working Docker installation and your Java JUnit test suite.
* Browsers are always launched from a fixed, clean image. This means no configuration drift from user changes or
  automatic browser upgrades.
* Additionally the use of a clean browser prevents leakage of cookies, cached data or other state between tests.
* **VNC screen recording**: Test Containers can automatically record video of test runs (optionally capturing just
  failing tests)

Creation of browser containers is fast, so it's actually quite feasible to have a totally fresh browser instance for
every test.

## Example

The following field in your JUnit UI test class will prepare a container running Chrome:
	
        @Rule
        public BrowserWebDriverContainer chrome =
            new BrowserWebDriverContainer()
                    .withDesiredCapabilities(DesiredCapabilities.chrome());
        
Now, instead of instantiating an instance of WebDriver directly, use the following to obtain an instance inside your
test methods:

        RemoteWebDriver driver = chrome.getWebDriver();

You can then use this driver instance like a regular WebDriver.

Note that, if you want to test a **web application running on the host machine** (the machine the JUnit tests are
running on - which is quite likely), you'll need to replace any references to `localhost` with an IP address that the
Docker container can reach. Use the `getHostIpAddress()` method, e.g.:

        driver.get("http://" + chrome.getHostIpAddress() + ":8080/");

## Options

### Other browsers

At the moment, Chrome and Firefox are supported. To switch, simply change the first parameter to the rule constructor:

    new BrowserWebDriverContainer()
                    .withDesiredCapabilities(DesiredCapabilities.chrome());
        
or

    new BrowserWebDriverContainer()
                    .withDesiredCapabilities(DesiredCapabilities.firefox());

### Recording videos

By default, no videos will be recorded. However, you can instruct Test Containers to capture videos for all tests or
just for failing tests.

To do this, simply add extra parameters to the rule constructor:

    new BrowserWebDriverContainer()
                    .withDesiredCapabilities(DesiredCapabilities.chrome())
                    .withRecordingMode(VncRecordingMode.RECORD_ALL, new File("./target/"))

or if you only want videos for test failures:

    new BrowserWebDriverContainer()
                    .withDesiredCapabilities(DesiredCapabilities.chrome())
                    .withRecordingMode(VncRecordingMode.RECORD_FAILING, new File("./target/"))

Note that the seconds parameter to `withRecordingMode` should be a directory where recordings can be saved.

## More examples

A few different examples are shown in [ChromeWebDriverContainerTest.java](https://github.com/testcontainers/testcontainers-java/blob/master/modules/selenium/src/test/java/org/testcontainers/junit/ChromeWebDriverContainerTest.java).

# Extended Selenium Grid containers

To be able to run tests within dedicated hub / nodes containers (for further scaling), you can use the following items:

 * `GenericGridContainer` - base class for all Selenium Grid containers. Provides common API for starting child containers with video recording feature support. Only for internal usage.
 * `SeleniumHubContainer` - dedicated Selenium Grid Hub container without VNC and video recording support. Could be used with dedicated nodes.
 * `SeleniumNodeContainer` - dedicated Selenium Grid Node container with VNC and video recording support. Could be linked with dedicated hub by provided address. Supports ports exposing and volumes mounting. Could be safely used with object pool for further scaling.
 * `SeleniumStandaloneContainer` - `WebDriver` independent `BrowseWebDroverContainer`'s alternative.

Note that `WebDriver` dependency was completely removed to let end-user fully control testing flow by his own.

All listed containers use the following enums to provide better flexibility while configuration phase:
 
 * `Browser` - contains a list of supported browsers. Note that standalone chrome / firefox items are splitted due to the differences in containers' creation logic.
 * `SeleniumImage` - contains a list of official [SeleniumHQ](https://github.com/SeleniumHQ/docker-selenium) images, mapped with corresponding browsers. Note that versions are set to `latest` by default.
 
Containers' connection obtaining was revised due to the fact the containers themselves were splitted, and `WebDriver` dependency was removed. There was added a special utility class `SeleniumHttpUtils` for detecting if hub / node is ready for interaction before actual test execution is started.

As soon as the following PR is merged, there will be added extensive `TestNG` usage examples with scaling feature.