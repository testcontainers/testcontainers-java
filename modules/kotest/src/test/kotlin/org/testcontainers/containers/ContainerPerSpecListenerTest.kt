import io.kotest.core.spec.style.StringSpec
import io.mockk.mockk
import io.mockk.verify
import org.testcontainers.containers.ContainerPerSpecListener
import org.testcontainers.containers.GenericContainer

class ContainerPerSpecListenerTest : StringSpec() {
   init {
      "should stop container in beforeSpec callback" {
         val mockContainer: GenericContainer<Nothing> = mockk(relaxed = true)
         val containerPerSpecListener = ContainerPerSpecListener(mockContainer)

         containerPerSpecListener.afterSpec(mockk())

         verify(exactly = 1) { mockContainer.stop() }
      }

      "should start container in afterSpec callback" {
         val mockContainer: GenericContainer<Nothing> = mockk(relaxed = true)
         val containerPerSpecListener = ContainerPerSpecListener(mockContainer)

         containerPerSpecListener.beforeSpec(mockk())

         verify(exactly = 1) { mockContainer.start() }
      }
   }
}
