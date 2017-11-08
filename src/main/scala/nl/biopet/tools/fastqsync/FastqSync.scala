package nl.biopet.tools.fastqsync

import java.io.File

import htsjdk.samtools.fastq.{
  AsyncFastqWriter,
  BasicFastqWriter,
  FastqReader,
  FastqRecord
}
import nl.biopet.utils.tool.ToolCommand

import scala.annotation.tailrec
import scala.collection.JavaConverters._

object FastqSync extends ToolCommand[Args] {
  def emptyArgs: Args = Args()
  def argsParser = new ArgsParser(toolName)
  def main(args: Array[String]): Unit = {
    val cmdArgs = cmdArrayToArgs(args)

    logger.info("Start")

    idSufixes = findR1R2Suffixes(cmdArgs.refFastq1, cmdArgs.refFastq2)

    val refReader = new FastqReader(cmdArgs.refFastq1)
    val AReader = new FastqReader(cmdArgs.inputFastq1)
    val BReader = new FastqReader(cmdArgs.inputFastq2)
    val AWriter =
      new AsyncFastqWriter(new BasicFastqWriter(cmdArgs.outputFastq1), 3000)
    val BWriter =
      new AsyncFastqWriter(new BasicFastqWriter(cmdArgs.outputFastq2), 3000)

    try {
      val (numDiscA, numDiscB, numKept) =
        syncFastq(refReader, AReader, BReader, AWriter, BWriter)
      println(s"Filtered $numDiscA reads from first read file.")
      println(s"Filtered $numDiscB reads from second read file.")
      println(s"Synced files contain $numKept reads.")
    } finally {
      refReader.close()
      AReader.close()
      BReader.close()
      AWriter.close()
      BWriter.close()
    }

    logger.info("Done")
  }

  /**
    * This method will look up the unique suffix for R1 and R2
    *
    * @param fastqR1 input R1 file
    * @param fastqR2 Input R2 file
    * @return suffix for (R1, R2)
    */
  def findR1R2Suffixes(fastqR1: File, fastqR2: File): (String, String) = {
    val refReader1 = new FastqReader(fastqR1)
    val refReader2 = new FastqReader(fastqR2)
    val r1Name = refReader1.next().getReadName.split(" ").head
    val r2Name = refReader2.next().getReadName.split(" ").head
    refReader1.close()
    refReader2.close()

    val genericName = new String(
      r1Name.zip(r2Name).takeWhile(x => x._1 == x._2).map(_._1).toArray)

    (r1Name.stripPrefix(genericName), r2Name.stripPrefix(genericName))
  }

  /** Regex for capturing read ID ~ taking into account its read pair mark (if present) */
  private[tools] var idSufixes: (String, String) = _

  /** Implicit class to allow for lazy retrieval of FastqRecord ID without any read pair mark */
  private implicit class FastqPair(fq: FastqRecord) {
    lazy val fragId: String = fq.getReadName.split(" ").head match {
      case x if x.endsWith(idSufixes._1) => x.stripSuffix(idSufixes._1)
      case x if x.endsWith(idSufixes._2) => x.stripSuffix(idSufixes._2)
      case x => x
    }
  }

  /**
    * Filters out FastqRecord that are not present in the input iterators, using
    * a reference sequence object
    *
    * @param pre FastqReader over reference FASTQ file
    * @param seqA FastqReader over read 1
    * @param seqB FastqReader over read 2
    * @return
    */
  def syncFastq(pre: FastqReader,
                seqA: FastqReader,
                seqB: FastqReader,
                seqOutA: AsyncFastqWriter,
                seqOutB: AsyncFastqWriter): (Long, Long, Long) = {
    // counters for discarded A and B seqections + total kept
    // NOTE: we are reasigning values to these variables in the recursion below
    var (numDiscA, numDiscB, numKept) = (0, 0, 0)

    /**
      * Syncs read pairs recursively
      *
      * @param pre Reference sequence, assumed to be a superset of both seqA and seqB
      * @param seqA Sequence over read 1
      * @param seqB Sequence over read 2
      * @return
      */
    @tailrec
    def syncIter(pre: Stream[FastqRecord],
                 seqA: Stream[FastqRecord],
                 seqB: Stream[FastqRecord]): Unit =
      (pre.headOption, seqA.headOption, seqB.headOption) match {
        // where the magic happens!
        case (Some(r), Some(a), Some(b)) =>
          val (nextA, nextB) =
            (a.fragId == r.fragId, b.fragId == r.fragId) match {
              // all IDs are equal to ref
              case (true, true) =>
                numKept += 1
                seqOutA.write(a)
                seqOutB.write(b)
                (seqA.tail, seqB.tail)
              // B not equal to ref and A is equal, then we discard A and progress
              case (true, false) =>
                numDiscA += 1
                (seqA.tail, seqB)
              // A not equal to ref and B is equal, then we discard B and progress
              case (false, true) =>
                numDiscB += 1
                (seqA, seqB.tail)
              case (false, false) =>
                (seqA, seqB)
            }
          syncIter(pre.tail, nextA, nextB)
        // recursion base case: both iterators have been exhausted
        case (_, None, None) => ;
        // illegal state: reference sequence exhausted but not seqA or seqB
        case (None, Some(_), _) | (None, _, Some(_)) =>
          throw new NoSuchElementException(
            "Reference record stream shorter than expected")
        // keep recursion going if A still has items (we want to count how many)
        case (_, _, None) =>
          numDiscA += 1
          syncIter(pre.tail, seqA.tail, seqB)
        // like above but for B
        case (_, None, _) =>
          numDiscB += 1
          syncIter(pre.tail, seqA, seqB.tail)
      }

    syncIter(pre.iterator.asScala.toStream,
             seqA.iterator.asScala.toStream,
             seqB.iterator.asScala.toStream)

    (numDiscA, numDiscB, numKept)
  }

}
