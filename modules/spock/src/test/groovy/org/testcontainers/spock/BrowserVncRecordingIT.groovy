package org.testcontainers.spock

import org.intellij.lang.annotations.Language
import spock.lang.Specification
import spock.util.EmbeddedSpecRunner

class BrowserVncRecordingIT extends Specification {

    def cleanup() {
        new File("dummy.flv").delete()
    }

    def "retains all recordings"() {
        given:

        @Language("groovy")
        String myTest = """
package org.testcontainers.spock

import org.junit.runner.Description
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.RemoteWebDriver
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.RecordingFileFactory
import spock.lang.Specification

import java.util.concurrent.TimeUnit

import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL

@Testcontainers
class BrowserWebdriverContainerIT extends Specification {

    BrowserWebDriverContainer browserContainer = new BrowserWebDriverContainer()
        .withDesiredCapabilities(DesiredCapabilities.chrome())
        .withRecordingMode(RECORD_ALL, new File("./build/"))
        .withRecordingFileFactory(new RecordingFileFactory() {
            @Override
            File recordingFileForTest(File vncRecordingDirectory, Description description, boolean succeeded) {
                return new File("dummy.flv")
            }
    });

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
        def result = runner.run(myTest)

        then:
        new File("dummy.flv").exists()
    }

    def "records nothing if RECORD_FAILING and not failing"() {
        given:

        @Language("groovy")
        String myTest = """
package org.testcontainers.spock

import org.junit.runner.Description
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.RemoteWebDriver
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.RecordingFileFactory
import spock.lang.Specification

import java.util.concurrent.TimeUnit

import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_FAILING

@Testcontainers
class BrowserWebdriverContainerIT extends Specification {

    BrowserWebDriverContainer browserContainer = new BrowserWebDriverContainer()
        .withDesiredCapabilities(DesiredCapabilities.chrome())
        .withRecordingMode(RECORD_FAILING, new File("./build/"))
        .withRecordingFileFactory(new RecordingFileFactory() {
            @Override
            File recordingFileForTest(File vncRecordingDirectory, Description description, boolean succeeded) {
                return new File("dummy.flv")
            }
    });

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
        def result = runner.run(myTest)

        then:
        !new File("dummy.flv").exists()
    }

    def "records file if RECORD_FAILING and failing"() {
        given:

        @Language("groovy")
        String myTest = """
package org.testcontainers.spock

import org.junit.runner.Description
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.RemoteWebDriver
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.RecordingFileFactory
import spock.lang.Specification

import java.util.concurrent.TimeUnit

import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_FAILING

@Testcontainers
class BrowserWebdriverContainerIT extends Specification {

    BrowserWebDriverContainer browserContainer = new BrowserWebDriverContainer()
        .withDesiredCapabilities(DesiredCapabilities.chrome())
        .withRecordingMode(RECORD_FAILING, new File("./build/"))
        .withRecordingFileFactory(new RecordingFileFactory() {
            @Override
            File recordingFileForTest(File vncRecordingDirectory, Description description, boolean succeeded) {
                return new File("dummy.flv")
            }
    });

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
        def result = runner.run(myTest)

        then:
        new File("dummy.flv").exists()
    }

}
