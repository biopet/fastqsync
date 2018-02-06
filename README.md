# FastqSync


Sync paired-end FASTQ files.
Some QC tools are not aware of paired-end sequencing. These tools
often delete one read of the read pair, while leaving the other. This will lead
to the FASTQ files for the reads being out of sync.

FastqSync will check back with the original FASTQ files (before QC) and
make sure that when a read is removed from one pair, the other read from
the pair is also removed.
    

# Documentation

For documentation and manuals visit our [github.io page](https://biopet.github.io/fastqsync).

# About


FastqSync is part of BIOPET tool suite that is developed at LUMC by [the SASC team](http://sasc.lumc.nl/).
Each tool in the BIOPET tool suite is meant to offer a standalone function that can be used to perform a
dedicate data analysis task or added as part of [BIOPET pipelines](http://biopet-docs.readthedocs.io/en/latest/).

All tools in the BIOPET tool suite are [Free/Libre](https://www.gnu.org/philosophy/free-sw.html) and
[Open Source](https://opensource.org/osd) Software.
    

# Contact


<p>
  <!-- Obscure e-mail address for spammers -->
For any question related to FastqSync, please use the
<a href='https://github.com/biopet/fastqsync/issues'>github issue tracker</a>
or contact
 <a href='http://sasc.lumc.nl/'>the SASC team</a> directly at: <a href='&#109;&#97;&#105;&#108;&#116;&#111;&#58;&#115;&#97;&#115;&#99;&#64;&#108;&#117;&#109;&#99;&#46;&#110;&#108;'>
&#115;&#97;&#115;&#99;&#64;&#108;&#117;&#109;&#99;&#46;&#110;&#108;</a>.
</p>

     

