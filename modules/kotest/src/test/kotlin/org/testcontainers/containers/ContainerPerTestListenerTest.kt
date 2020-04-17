import io.kotest.core.spec.style.StringSpec
import io.mockk.mockk
import io.mockk.verify
import org.testcontainers.containers.ContainerPerTestListener
import org.testcontainers.containers.GenericContainer

class ContainerPerTestListenerTest: StringSpec() {
   init {
       "should start container in beforeTest callback" {
          val mockContainer: GenericContainer<Nothing> = mockk(relaxed = true)
          val containerPerTestListener = ContainerPerTestListener(mockContainer)

          containerPerTestListener.beforeTest(mockk())

          verify(exactly = 1) { mockContainer.start() }
       }

       "should stop container in afterTest callback" {
          val mockContainer: GenericContainer<Nothing> = mockk(relaxed = true)
          val containerPerTestListener = ContainerPerTestListener(mockContainer)

          containerPerTestListener.afterTest(mockk(), mockk())

          verify(exactly = 1) { mockContainer.stop() }
       }
   }
}
