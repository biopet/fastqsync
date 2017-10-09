package nl.biopet.tools.fastqsync

import java.io.File

case class Args(refFastq1: File = null,
                refFastq2: File = null,
                inputFastq1: File = null,
                inputFastq2: File = null,
                outputFastq1: File = null,
                outputFastq2: File = null)
