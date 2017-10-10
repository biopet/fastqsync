package nl.biopet.tools.fastqsync

import java.io.File

import nl.biopet.utils.tool.AbstractOptParser

class ArgsParser(cmdName: String) extends AbstractOptParser[Args](cmdName) {

  head(s"""
          |$cmdName - Sync paired-end FASTQ files.
          |
          |This tool works with gzipped or non-gzipped FASTQ files. The output
          |file will be gzipped when the input is also gzipped.
      """.stripMargin)

  opt[File]('r', "ref1") unbounded () required () valueName "<fastq>" action { (x, c) =>
    c.copy(refFastq1 = x)
  } validate { x =>
    if (x.exists) success else failure("Reference FASTQ file not found")
  } text "Reference R1 FASTQ file"

  opt[File]("ref2") unbounded () required () valueName "<fastq>" action { (x, c) =>
    c.copy(refFastq2 = x)
  } validate { x =>
    if (x.exists) success else failure("Reference FASTQ file not found")
  } text "Reference R2 FASTQ file"

  opt[File]('i', "in1") unbounded () required () valueName "<fastq>" action { (x, c) =>
    c.copy(inputFastq1 = x)
  } validate { x =>
    if (x.exists) success else failure("Input FASTQ file 1 not found")
  } text "Input FASTQ file 1"

  opt[File]('j', "in2") unbounded () required () valueName "<fastq[.gz]>" action { (x, c) =>
    c.copy(inputFastq2 = x)
  } validate { x =>
    if (x.exists) success else failure("Input FASTQ file 2 not found")
  } text "Input FASTQ file 2"

  opt[File]('o', "out1") unbounded () required () valueName "<fastq[.gz]>" action { (x, c) =>
    c.copy(outputFastq1 = x)
  } text "Output FASTQ file 1"

  opt[File]('p', "out2") unbounded () required () valueName "<fastq>" action { (x, c) =>
    c.copy(outputFastq2 = x)
  } text "Output FASTQ file 2"
}
