package org.testcontainers.spock

import org.intellij.lang.annotations.Language
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.util.EmbeddedSpecRunner

class BrowserVncRecordingIT extends Specification {

    @Rule
    TemporaryFolder temp

    String recordingDir

    def setup() {
        recordingDir = temp.getRoot().getAbsolutePath()
    }

    def "retains all recordings for RECORD_ALL if successful"() {
        given:

        //noinspection GrPackage
        @Language("groovy")
        String myTest = """
package org.testcontainers.spock

import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.testcontainers.containers.BrowserWebDriverContainer
import spock.lang.Specification

import java.util.concurrent.TimeUnit

import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL

@Testcontainers
class BrowserWebdriverContainerIT extends Specification {

    BrowserWebDriverContainer browserContainer = new BrowserWebDriverContainer()
        .withCapabilities(new ChromeOptions())
        .withRecordingMode(RECORD_ALL, new File("$recordingDir"))

    RemoteWebDriver driver

    def setup() {
        driver = browserContainer.getWebDriver()
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS)
    }

    def "should record"() {
        when:
        driver.get("http://en.wikipedia.org/wiki/Randomness")

        then:
        driver.findElementByPartialLinkText("pattern").isDisplayed()
    }

}


"""

        when:
        EmbeddedSpecRunner runner = new EmbeddedSpecRunner(throwFailure: true)
        runner.run(myTest)

        then:
        temp.getRoot().list().find { it.contains("BrowserWebdriverContainerIT-should-record") }
    }

    def "records nothing if RECORD_FAILING and not failing"() {
        given:

        //noinspection GrPackage
        @Language("groovy")
        String myTest = """
package org.testcontainers.spock

import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.testcontainers.containers.BrowserWebDriverContainer
import spock.lang.Specification

import java.util.concurrent.TimeUnit

import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_FAILING

@Testcontainers
class BrowserWebdriverContainerIT extends Specification {

    BrowserWebDriverContainer browserContainer = new BrowserWebDriverContainer()
        .withCapabilities(new ChromeOptions())
        .withRecordingMode(RECORD_FAILING, new File("$recordingDir"))

    RemoteWebDriver driver

    def setup() {
        driver = browserContainer.getWebDriver()
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS)
    }

    def "should record"() {
        when:
        driver.get("http://en.wikipedia.org/wiki/Randomness")

        then:
        driver.findElementByPartialLinkText("pattern").isDisplayed()
    }

}


"""

        when:
        EmbeddedSpecRunner runner = new EmbeddedSpecRunner(throwFailure: false)
        runner.run(myTest)

        then:
        temp.getRoot().list().length == 0
    }

    def "records file if RECORD_FAILING and failing"() {
        given:

        //noinspection GrPackage
        @Language("groovy")
        String myTest = """
package org.testcontainers.spock

import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.testcontainers.containers.BrowserWebDriverContainer
import spock.lang.Specification

import java.util.concurrent.TimeUnit

import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_FAILING

@Testcontainers
class BrowserWebdriverContainerIT extends Specification {

    BrowserWebDriverContainer browserContainer = new BrowserWebDriverContainer()
        .withCapabilities(new ChromeOptions())
        .withRecordingMode(RECORD_FAILING, new File("$recordingDir"))

    RemoteWebDriver driver

    def setup() {
        driver = browserContainer.getWebDriver()
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS)
    }

    def "should record"() {
        when:
        driver.get("http://en.wikipedia.org/wiki/Randomness")

        then:
        !driver.findElementByPartialLinkText("pattern").isDisplayed()
    }

}


"""

        when:
        EmbeddedSpecRunner runner = new EmbeddedSpecRunner(throwFailure: false)
        runner.run(myTest)

        then:
        temp.getRoot().list().find { it.contains("BrowserWebdriverContainerIT-should-record") }
    }

}
