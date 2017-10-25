# Manual

The tool requires two FASTQ files,two output FASTQ file and two reference FASTQ files.

This tool works with gzipped or non-gzipped FASTQ files. The output
file will be gzipped when the input is also gzipped.

Example:
```bash
java -jar FastqSync-version.jar \
--in1 input1.fastq
--in2 input2.fastq
--ref1 ref1.fastq
--ref2 ref2.fastq
--out1 output1.fastq
--out2 output2.fastq
```