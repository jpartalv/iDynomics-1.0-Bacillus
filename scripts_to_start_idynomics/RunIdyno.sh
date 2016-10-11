#!/bin/bash
#
# Author: Edd Miles - edd@edd-miles.com
# Script to start the idynomics software with multiple input files from the command line under Linux.
# Three (optional) command switches may be provided:
# *DEPRECATED* -i for the protocol files to be worked on *DEPRECATED*
# Files to be worked on can now be listed without -i
# -min for the java -Xms variable
# -max from the java -Xmx variable
# For ease of use, it is advised to add the idynomics folder to your PATH variable

# The default xmin and xmax values
XMIN="300m"
XMAX="2000m"


# Loop through the arguments and set the correct value.
# This is not quite the most efficient way to do this for three arguments
# but it does make adding extra arguments (if necessary) easy. 
while [ "$1" != "" ]; do 
	case "$1" in
		"-min")	if [ "$2" != "" ]; then
				XMIN=$2 
			else
				echo "NO MINIMUM SPECIFIED"
				exit
			fi
			shift;;
		"-max")	if [ "$2" != "" ]; then
				XMAX=$2 
			else
				echo "NO MAXIMUM SPECIFIED"
				exit
			fi 
			shift;;
		"-i") if [ -f $2 ]; #check if the file exists
			then
				FILE=($2)
				echo "Warning: Use of -i is deprechiated"
				shift
			else
				echo "NO SUCH FILE $2"
				exit
			fi;; 
		*) 	if [ -f $1 ]; then 
				if [ ${FILE[0]} ]; then
					FILE=("${FILE[@]}" $1)
				else
					FILE=($1)
				fi
			else
				echo "NO SUCH FILE $1"
				exit
			fi;;
	esac
	shift
done

### Get the path to the idynomics folder (NPATH)
### Comment in/out the option that works for you
# Option 1
#NPATH=`dirname $0`
#NPATH="`( cd \"$NPATH\" && pwd )`"
# Option 2: simple, hard wired path to idynomics, you will need to change the path to where you have idynomics on your system
NPATH=~/idynomics
# Option 3
#NPATH=${0%/*}

# for debugging
echo $NPATH
echo

### Set the absolute CLASSPATH (divided for ease of reading)/modification
# Jan: Depending on your settings in Eclipse, the class files may be in the src directory with the java files
# or in a separate bin directory. You need to check your installation!

### Use this if you have all class files in the src directory tree
#CLASSPATH=$NPATH"/src"
# Jan: this does not seem to be needed
##CLASSPATH=$CLASSPATH":"$NPATH
#CLASSPATH=$CLASSPATH":"$NPATH"/src/lib/jcommon-1.0.12.jar"
#CLASSPATH=$CLASSPATH":"$NPATH"/src/lib/jfreechart-1.0.9.jar"
#CLASSPATH=$CLASSPATH":"$NPATH"/src/lib/jdom.jar"
#CLASSPATH=$CLASSPATH":"$NPATH"/src/lib/truezip-6.jar"
#CLASSPATH=$CLASSPATH":"$NPATH"/src/lib/Jama-1.0.2.jar"

### Use this if you have the class files separate in a bin folder
CLASSPATH=$NPATH"/bin"
# Jan: this does not seem to be needed
#CLASSPATH=$CLASSPATH":"$NPATH
CLASSPATH=$CLASSPATH":"$NPATH"/bin/lib/jcommon-1.0.12.jar"
CLASSPATH=$CLASSPATH":"$NPATH"/bin/lib/jfreechart-1.0.9.jar"
CLASSPATH=$CLASSPATH":"$NPATH"/bin/lib/jdom.jar"
CLASSPATH=$CLASSPATH":"$NPATH"/bin/lib/truezip-6.jar"
CLASSPATH=$CLASSPATH":"$NPATH"/bin/lib/Jama-1.0.2.jar"

# for debugging
echo $CLASSPATH
echo

#call the program for each file in the FILE array
len=${#FILE[*]}
if [ $len == 0 ]; then
	java -Xms$XMIN -Xmx$XMAX -cp $CLASSPATH idyno.Idynomics
else
	i=0
	while [ $i -lt $len ]; do
		java -Xms$XMIN -Xmx$XMAX -cp $CLASSPATH idyno.Idynomics ${FILE[$i]}
		let i++
	done
fi
