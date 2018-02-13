/*
 * Copyright (c) 2014 Biopet
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
  def argsParser = new ArgsParser(this)
  def main(args: Array[String]): Unit = {
    val cmdArgs = cmdArrayToArgs(args)

    // Require input files to be present
    require(cmdArgs.refFastq1.exists(), "Reference R1 FASTQ file not found")
    require(cmdArgs.refFastq2.exists(), "Reference R2 FASTQ file not found")
    require(cmdArgs.inputFastq1.exists(), "Input FASTQ file 1 not found")
    require(cmdArgs.inputFastq2.exists(), "Input FASTQ file 2 not found")

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
      case x                             => x
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

  def descriptionText: String =
    s"""
      |Sync paired-end FASTQ files.
      |Some QC tools are not aware of paired-end sequencing. These tools
      |often delete one read of the read pair, while leaving the other. This will lead
      |to the FASTQ files for the reads being out of sync.
      |
      |$toolName will check back with the original FASTQ files (before QC) and
      |make sure that when a read is removed from one pair, the other read from
      |the pair is also removed.
    """.stripMargin
  def manualText: String =
    """
      |The tool requires two FASTQ files,two output FASTQ file and the original FASTQ files.
      |
      |This tool works with gzipped or non-gzipped FASTQ files.
      |The output file will be gzipped when the input is also gzipped.
    """.stripMargin

  def exampleText: String =
    s"""
       |To sync two fastq files:
       |
       |${example(
         "--in1",
         "read1.fastq",
         "--in2",
         "read2.fastq",
         "--ref1",
         "beforeQC_read1.fastq",
         "--ref2",
         "beforeQC_read2.fastq",
         "--out1",
         "output1.fastq",
         "--out2",
         "output2.fastq"
       )}
     """.stripMargin
}
