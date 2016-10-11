README for RunIdyno.sh/RunIdyno.bat

Author: Edd Miles - edd@edd-miles.com

RunIdyno allows you to call the Idynomics software with the correct path from anywhere on your computer. It calculates its own absolute path, and then sets the classpath variable based on that. For this reason, it will only function when left in the root idynomics folder. 

The programs take command line input like:
-min X -> sets the initial memory size of the java vm to X (default: 300m)
-max X -> sets the maximum memory size of the java vm to X (default: 600m)

Anything else on the command line will be treated as an input protocol file. Each separate file will be run after the previous one, i.e. sequentially. Thus on a multi-core system, it may be better to call several copies of the program than to run one long queue. This program has been tested with a queue length of one to five protocol files, but should work with any number of files.

If your working directory contains the protocol files when you invoke the script, you MUST path them or a null error will be returned. e.g. instead of calling "..\idynomics\RunIdyno.bat protocol1.xml protocol2.xml" you must instead call "..\idynomics\RunIdyno.bat .\protocol.xml .\protocol.xml"

Please note: The Windows version of this software requires you to be in a folder where you have write access when calling it. This is due to it needing to create a tempfile in lieu of having an array data type.

Also note: In linux you may need to set the +x flag on the RunIdyno.sh file manually

If you discover any abnormal behaviour, please email edd@edd-miles.com

Sample usuage (windows)

RunIdyno.bat -max 1024m protocol\basic_2d.xml

"C:\Program Files\idynomics\RunIdyno.bat" .\Long_run.xml .\Long_run2.xml 

Sample usuage (Linux)

./RunIdyno.sh -max 1024m protocol/basic_2d.xml

/home/milesem/idynomics/RunIdyno.sh ./Foo_run.xml ./Bar_run2.xml
