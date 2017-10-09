package nl.biopet.tools.fastqsync

import nl.biopet.test.BiopetTest
import org.testng.annotations.Test

class FastqSyncTest extends BiopetTest {
  @Test
  def testNoArgs(): Unit = {
    intercept[IllegalArgumentException] {
      FastqSync.main(Array())
    }
  }
}
