package nl.biopet.tools.fastqsync

import java.io.File

import htsjdk.samtools.fastq.{AsyncFastqWriter, FastqReader, FastqRecord}
import nl.biopet.test.BiopetTest
import org.mockito.Mockito.{when, inOrder => inOrd}
import org.scalatest.mock.MockitoSugar
import org.testng.annotations.{DataProvider, Test}

import scala.collection.JavaConverters._

class FastqSyncTest extends BiopetTest with MockitoSugar {
  @Test
  def testNoArgs(): Unit = {
    intercept[IllegalArgumentException] {
      FastqSync.main(Array())
    }
  }

  // Helper functions to create iterator over FastqRecords given its IDs as Ints
  private def recordsOver(ids: String*): java.util.Iterator[FastqRecord] =
    ids
      .map(x => new FastqRecord(x, "A", "", "H"))
      .toIterator
      .asJava

  @DataProvider(name = "mockProvider")
  def mockProvider() =
    Array(
      Array(mock[FastqReader],
        mock[FastqReader],
        mock[FastqReader],
        mock[AsyncFastqWriter],
        mock[AsyncFastqWriter])
    )

  @Test(dataProvider = "mockProvider")
  def testDefault(refMock: FastqReader,
                  aMock: FastqReader,
                  bMock: FastqReader,
                  aOutMock: AsyncFastqWriter,
                  bOutMock: AsyncFastqWriter): Unit = {
    FastqSync.idSufixes = ("/1", "/2")

    when(refMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(aMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(bMock.iterator) thenReturn recordsOver("1", "2", "3")
    val obs = inOrd(aOutMock, bOutMock)
    val exp = recordsOver("1", "2", "3").asScala.toSeq

    val (numDiscard1, numDiscard2, numKept) = FastqSync.syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    obs.verify(aOutMock).write(exp.head)
    obs.verify(bOutMock).write(exp.head)

    obs.verify(aOutMock).write(exp(1))
    obs.verify(bOutMock).write(exp(1))

    obs.verify(aOutMock).write(exp(2))
    obs.verify(bOutMock).write(exp(2))

    numDiscard1 shouldBe 0
    numDiscard2 shouldBe 0
    numKept shouldBe 3
  }

  @Test(dataProvider = "mockProvider")
  def testRefTooShort(refMock: FastqReader,
                      aMock: FastqReader,
                      bMock: FastqReader,
                      aOutMock: AsyncFastqWriter,
                      bOutMock: AsyncFastqWriter): Unit = {

    when(refMock.iterator) thenReturn recordsOver("1", "2")
    when(aMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(bMock.iterator) thenReturn recordsOver("1", "2", "3")

    val thrown = intercept[NoSuchElementException] {
      FastqSync.syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)
    }
    thrown.getMessage should ===("Reference record stream shorter than expected")
  }

  @Test(dataProvider = "mockProvider")
  def testSeqAEmpty(refMock: FastqReader,
                    aMock: FastqReader,
                    bMock: FastqReader,
                    aOutMock: AsyncFastqWriter,
                    bOutMock: AsyncFastqWriter): Unit = {
    FastqSync.idSufixes = ("/1", "/2")

    when(refMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(aMock.iterator) thenReturn recordsOver()
    when(bMock.iterator) thenReturn recordsOver("1", "2", "3")

    val (numDiscard1, numDiscard2, numKept) = FastqSync.syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    numDiscard1 shouldBe 0
    numDiscard2 shouldBe 3
    numKept shouldBe 0
  }

  @Test(dataProvider = "mockProvider")
  def testSeqBEmpty(refMock: FastqReader,
                    aMock: FastqReader,
                    bMock: FastqReader,
                    aOutMock: AsyncFastqWriter,
                    bOutMock: AsyncFastqWriter): Unit = {
    FastqSync.idSufixes = ("/1", "/2")

    when(refMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(aMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(bMock.iterator) thenReturn recordsOver()

    val (numDiscard1, numDiscard2, numKept) = FastqSync.syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    numDiscard1 shouldBe 3
    numDiscard2 shouldBe 0
    numKept shouldBe 0
  }

  @Test(dataProvider = "mockProvider")
  def testSeqAShorter(refMock: FastqReader,
                      aMock: FastqReader,
                      bMock: FastqReader,
                      aOutMock: AsyncFastqWriter,
                      bOutMock: AsyncFastqWriter): Unit = {
    FastqSync.idSufixes = ("/1", "/2")

    when(refMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(aMock.iterator) thenReturn recordsOver("2", "3")
    when(bMock.iterator) thenReturn recordsOver("1", "2", "3")
    val obs = inOrd(aOutMock, bOutMock)
    val exp = recordsOver("1", "2", "3").asScala.toSeq

    val (numDiscard1, numDiscard2, numKept) = FastqSync.syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    // exp(0) is discarded by syncFastq
    obs.verify(aOutMock).write(exp(1))
    obs.verify(bOutMock).write(exp(1))

    obs.verify(aOutMock).write(exp(2))
    obs.verify(bOutMock).write(exp(2))

    numDiscard1 shouldBe 0
    numDiscard2 shouldBe 1
    numKept shouldBe 2
  }

  @Test(dataProvider = "mockProvider")
  def testSeqBShorter(refMock: FastqReader,
                      aMock: FastqReader,
                      bMock: FastqReader,
                      aOutMock: AsyncFastqWriter,
                      bOutMock: AsyncFastqWriter): Unit = {
    FastqSync.idSufixes = ("/1", "/2")

    when(refMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(aMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(bMock.iterator) thenReturn recordsOver("1", "3")
    val obs = inOrd(aOutMock, bOutMock)
    val exp = recordsOver("1", "2", "3").asScala.toSeq

    val (numDiscard1, numDiscard2, numKept) = FastqSync.syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    // exp(1) is discarded by syncFastq
    obs.verify(aOutMock).write(exp.head)
    obs.verify(bOutMock).write(exp.head)

    obs.verify(aOutMock).write(exp(2))
    obs.verify(bOutMock).write(exp(2))

    numDiscard1 shouldBe 1
    numDiscard2 shouldBe 0
    numKept shouldBe 2
  }

  @Test(dataProvider = "mockProvider")
  def testSeqABShorter(refMock: FastqReader,
                       aMock: FastqReader,
                       bMock: FastqReader,
                       aOutMock: AsyncFastqWriter,
                       bOutMock: AsyncFastqWriter): Unit = {
    FastqSync.idSufixes = ("/1", "/2")

    when(refMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(aMock.iterator) thenReturn recordsOver("2", "3")
    when(bMock.iterator) thenReturn recordsOver("1", "2")
    val obs = inOrd(aOutMock, bOutMock)
    val exp = recordsOver("1", "2", "3").asScala.toSeq

    val (numDiscard1, numDiscard2, numKept) = FastqSync.syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    // exp(0) and exp(2) are discarded by syncFastq
    obs.verify(aOutMock).write(exp(1))
    obs.verify(bOutMock).write(exp(1))

    numDiscard1 shouldBe 1
    numDiscard2 shouldBe 1
    numKept shouldBe 1
  }

  @Test(dataProvider = "mockProvider")
  def testSeqSolexa(refMock: FastqReader,
                    aMock: FastqReader,
                    bMock: FastqReader,
                    aOutMock: AsyncFastqWriter,
                    bOutMock: AsyncFastqWriter): Unit = {

    when(refMock.iterator) thenReturn recordsOver("SOLEXA12_24:6:117:1388:2001/2",
      "SOLEXA12_24:6:96:470:1965/2",
      "SOLEXA12_24:6:35:1209:2037/2")
    when(aMock.iterator) thenReturn recordsOver("SOLEXA12_24:6:96:470:1965/1",
      "SOLEXA12_24:6:35:1209:2037/1")
    when(bMock.iterator) thenReturn recordsOver("SOLEXA12_24:6:117:1388:2001/2",
      "SOLEXA12_24:6:96:470:1965/2")
    val obs = inOrd(aOutMock, bOutMock)

    val (numDiscard1, numDiscard2, numKept) = FastqSync.syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    obs.verify(aOutMock).write(new FastqRecord("SOLEXA12_24:6:96:470:1965/1", "A", "", "H"))
    obs.verify(bOutMock).write(new FastqRecord("SOLEXA12_24:6:96:470:1965/2", "A", "", "H"))

    numDiscard1 shouldBe 1
    numDiscard2 shouldBe 1
    numKept shouldBe 1
  }

  @Test(dataProvider = "mockProvider")
  def testSeqABShorterPairMarkSlash(refMock: FastqReader,
                                    aMock: FastqReader,
                                    bMock: FastqReader,
                                    aOutMock: AsyncFastqWriter,
                                    bOutMock: AsyncFastqWriter): Unit = {
    FastqSync.idSufixes = ("/1", "/2")

    when(refMock.iterator) thenReturn recordsOver("1/1", "2/1", "3/1")
    when(aMock.iterator) thenReturn recordsOver("2/1", "3/1")
    when(bMock.iterator) thenReturn recordsOver("1/2", "2/2")
    val obs = inOrd(aOutMock, bOutMock)

    val (numDiscard1, numDiscard2, numKept) = FastqSync.syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    obs.verify(aOutMock).write(new FastqRecord("2/1", "A", "", "H"))
    obs.verify(bOutMock).write(new FastqRecord("2/2", "A", "", "H"))

    numDiscard1 shouldBe 1
    numDiscard2 shouldBe 1
    numKept shouldBe 1
  }

  @Test(dataProvider = "mockProvider")
  def testSeqABShorterPairMarkUnderscore(refMock: FastqReader,
                                         aMock: FastqReader,
                                         bMock: FastqReader,
                                         aOutMock: AsyncFastqWriter,
                                         bOutMock: AsyncFastqWriter): Unit = {
    FastqSync.idSufixes = ("_1", "_2")

    when(refMock.iterator) thenReturn recordsOver("1_1", "2_1", "3_1")
    when(aMock.iterator) thenReturn recordsOver("2_1", "3_1")
    when(bMock.iterator) thenReturn recordsOver("1_2", "2_2")
    val obs = inOrd(aOutMock, bOutMock)

    val (numDiscard1, numDiscard2, numKept) = FastqSync.syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    obs.verify(aOutMock).write(new FastqRecord("2_1", "A", "", "H"))
    obs.verify(bOutMock).write(new FastqRecord("2_2", "A", "", "H"))

    numDiscard1 shouldBe 1
    numDiscard2 shouldBe 1
    numKept shouldBe 1
  }

  @Test(dataProvider = "mockProvider")
  def testSeqABShorterWithDescription(refMock: FastqReader,
                                      aMock: FastqReader,
                                      bMock: FastqReader,
                                      aOutMock: AsyncFastqWriter,
                                      bOutMock: AsyncFastqWriter): Unit = {
    FastqSync.idSufixes = ("/1", "/2")

    when(refMock.iterator) thenReturn recordsOver("1 desc1b", "2 desc2b", "3 desc3b")
    when(aMock.iterator) thenReturn recordsOver("2 desc2a", "3 desc3a")
    when(bMock.iterator) thenReturn recordsOver("1 desc1b", "2 desc2b")
    val obs = inOrd(aOutMock, bOutMock)

    val (numDiscard1, numDiscard2, numKept) = FastqSync.syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    obs.verify(aOutMock).write(new FastqRecord("2 desc2a", "A", "", "H"))
    obs.verify(bOutMock).write(new FastqRecord("2 desc2b", "A", "", "H"))

    numDiscard1 shouldBe 1
    numDiscard2 shouldBe 1
    numKept shouldBe 1
  }

  @Test(dataProvider = "mockProvider")
  def testComplex(refMock: FastqReader,
                  aMock: FastqReader,
                  bMock: FastqReader,
                  aOutMock: AsyncFastqWriter,
                  bOutMock: AsyncFastqWriter): Unit = {
    FastqSync.idSufixes = ("/1", "/2")

    when(refMock.iterator) thenReturn recordsOver("1/2 yep",
      "2/2 yep",
      "3/2 yep",
      "4/2 yep",
      "5/2 yep")
    when(aMock.iterator) thenReturn recordsOver("1/1 yep", "2/1 yep", "4/1 yep")
    when(bMock.iterator) thenReturn recordsOver("1/2 yep", "3/2 yep", "4/2 yep")
    val obs = inOrd(aOutMock, bOutMock)

    val (numDiscard1, numDiscard2, numKept) = FastqSync.syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    obs.verify(aOutMock).write(new FastqRecord("1/1 yep", "A", "", "H"))
    obs.verify(bOutMock).write(new FastqRecord("1/2 yep", "A", "", "H"))

    obs.verify(aOutMock).write(new FastqRecord("4/1 yep", "A", "", "H"))
    obs.verify(bOutMock).write(new FastqRecord("4/2 yep", "A", "", "H"))

    numDiscard1 shouldBe 1
    numDiscard2 shouldBe 1
    numKept shouldBe 2
  }

  @Test
  def testMain(): Unit = {
    val r1Output = File.createTempFile("temp.", ".fq")
    r1Output.deleteOnExit()
    val r2Output = File.createTempFile("temp.", ".fq")
    r2Output.deleteOnExit()
    val args = Array(
      "-r",
      resourcePath("/paired01a.fq"),
      "--ref2",
      resourcePath("/paired01b.fq"),
      "-i",
      resourcePath("/paired01a.fq"),
      "-j",
      resourcePath("/paired01b.fq"),
      "-o",
      r1Output.getAbsolutePath,
      "-p",
      r2Output.getAbsolutePath
    )

    FastqSync.main(args)
  }

}
