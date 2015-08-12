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
        public BrowserWebDriverContainerRule chrome = 
            new BrowserWebDriverContainerRule(DesiredCapabilities.chrome());
        
Now, instead of instantiating an instance of WebDriver directly, use the following to obtain an instance inside your
test methods:

        RemoteWebDriver driver = chrome.newDriver();

You can then use this driver instance like a regular WebDriver.

Note that, if you want to test a **web application running on the host machine** (the machine the JUnit tests are
running on - which is quite likely), you'll need to replace any references to `localhost` with an IP address that the
Docker container can reach. Use the `getHostIpAddress()` method, e.g.:

        driver.get("http://" + chrome.getHostIpAddress() + ":8080/");

## Options

### Other browsers

At the moment, Chrome and Firefox are supported. To switch, simply change the first parameter to the rule constructor:

    new BrowserWebDriverContainerRule(DesiredCapabilities.chrome())
        
or

    new BrowserWebDriverContainerRule(DesiredCapabilities.firefox())

### Recording videos

By default, no videos will be recorded. However, you can instruct Test Containers to capture videos for all tests or
just for failing tests.

To do this, simply add extra parameters to the rule constructor:

    new BrowserWebDriverContainerRule(
            DesiredCapabilities.chrome(),
            RECORD_ALL,
            new File("./target/"))
    
or if you don't want to keep videos of tests that passed:

    new BrowserWebDriverContainerRule(
            DesiredCapabilities.chrome(),
            RECORD_FAILING,
            new File("./target/"))
    
Note that the third parameter should be a directory where recordings can be saved.

## More examples

A few different examples are shown in [SimpleWebDriverContainerTest.java](https://github.com/testcontainers/testcontainers-java/blob/master/modules/selenium/src/test/java/org/testcontainers/junit/SimpleWebDriverContainerTest.java).