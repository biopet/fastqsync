/*
 * Copyright (c) 2014 Sequencing Analysis Support Core - Leiden University Medical Center
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

import nl.biopet.utils.tool.{AbstractOptParser, ToolCommand}

class ArgsParser(toolCommand: ToolCommand[Args])
    extends AbstractOptParser[Args](toolCommand) {

  head(s"""
          |$cmdName - Sync paired-end FASTQ files.
          |
          |This tool works with gzipped or non-gzipped FASTQ files. The output
          |file will be gzipped when the input is also gzipped.
      """.stripMargin)

  opt[File]('r', "ref1") unbounded () required () valueName "<fastq>" action {
    (x, c) =>
      c.copy(refFastq1 = x)
  } validate { x =>
    if (x.exists) success else failure("Reference FASTQ file not found")
  } text "Reference R1 FASTQ file"

  opt[File]("ref2") unbounded () required () valueName "<fastq>" action {
    (x, c) =>
      c.copy(refFastq2 = x)
  } validate { x =>
    if (x.exists) success else failure("Reference FASTQ file not found")
  } text "Reference R2 FASTQ file"

  opt[File]('i', "in1") unbounded () required () valueName "<fastq>" action {
    (x, c) =>
      c.copy(inputFastq1 = x)
  } validate { x =>
    if (x.exists) success else failure("Input FASTQ file 1 not found")
  } text "Input FASTQ file 1"

  opt[File]('j', "in2") unbounded () required () valueName "<fastq[.gz]>" action {
    (x, c) =>
      c.copy(inputFastq2 = x)
  } validate { x =>
    if (x.exists) success else failure("Input FASTQ file 2 not found")
  } text "Input FASTQ file 2"

  opt[File]('o', "out1") unbounded () required () valueName "<fastq[.gz]>" action {
    (x, c) =>
      c.copy(outputFastq1 = x)
  } text "Output FASTQ file 1"

  opt[File]('p', "out2") unbounded () required () valueName "<fastq>" action {
    (x, c) =>
      c.copy(outputFastq2 = x)
  } text "Output FASTQ file 2"
}
