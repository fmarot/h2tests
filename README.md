# h2tests

## Problem demonstrated
This demonstrate a problem in H2 database where, when run in TCP mode (not embedded) and storing large files (near MAX_INT bytes or larger, ie 2Go): H2 is stuck

** The problem does NOT happen when run in embedded mode ** => so the problem seems not to come from the way H2 stores the data...

## how to use
This is a typical Maven project.

You can import in your IDE or execute directly from command line: mvn exec:java

Please run class com.teamtter.H2Stuckclient and it will create a large file, try to store it in a BLOB and become stuck.
This behavior is reproducible with versions of H2 from 1.3.167 to current (1.4.186) on Linux.


If you want to have an example that works fine, please set H2Stuckclient.useWorkingExample to true and it will use a little text file that will be stored and then retreived correctly from H2.

## Analysis of the problem
It seems that if the big file does not contain only 0x00 as is showcased here but contains some random characters, the behavior may differ (in TspServerThread.process() the variable operation = transfer.readInt(); is filled with random values and hence the connection is closed with message "Unknown operation: <randomValue>".

Current analysis is that some buffer may be not fully read or read at a wrong index...
For a file size of MAX_INT the problem happens
For a file size of MAX_INT less a few bytes the problem still happens
For a file size of MAX_INT less even more bytes the problem does not happens (TODO: find exact values ?)

