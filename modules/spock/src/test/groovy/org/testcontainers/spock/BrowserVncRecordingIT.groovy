package org.testcontainers.spock

import org.intellij.lang.annotations.Language
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.EmbeddedSpecRunner

import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL
import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_FAILING

class BrowserVncRecordingIT extends Specification {

    @Rule
    TemporaryFolder temp

    String recordingDir

    def setup() {
        recordingDir = temp.getRoot().getAbsolutePath()
    }

    @Unroll("For recording mode #recordingMode and failing test is #fails records video file named '#videoFileName'")
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

import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode

@Testcontainers
class BrowserWebdriverContainerIT extends Specification {

    BrowserWebDriverContainer browserContainer = new BrowserWebDriverContainer()
        .withCapabilities(new ChromeOptions())
        .withRecordingMode("$recordingMode" as VncRecordingMode, new File("$recordingDir"))

    RemoteWebDriver driver

    def setup() {
        driver = browserContainer.getWebDriver()
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS)
    }

    def "should record"() {
        when:
        driver.get("http://en.wikipedia.org/wiki/Randomness")

        then:
        driver.findElementByPartialLinkText("pattern").isDisplayed() == !$fails
    }

}


"""

        when:
        EmbeddedSpecRunner runner = new EmbeddedSpecRunner(throwFailure: false)
        runner.run(myTest)

        then:
        def videoDir = temp.getRoot().list()
        if (videoFileName.isEmpty()) {
            videoDir.length == 0
        } else {
            videoDir.find { it.contains(videoFileName) }
        }

        where:
        recordingMode  | fails | videoFileName
        RECORD_ALL     | false | 'BrowserWebdriverContainerIT-should+record'
        RECORD_FAILING | false | ''
        RECORD_FAILING | true  | 'BrowserWebdriverContainerIT-should+record'
    }

}
