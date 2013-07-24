#!/bin/bash
# This script will update dependencies from M2 repository
#set -x
while read line 
do
	DEPENDENCY=$HOME/.m2/repository/$line
	echo "Updating dependency:" $DEPENDENCY
	if [ ! -f $line ]; then 
		mkdir -p $line
	fi
	cp -r $DEPENDENCY/* $line
done < dependencies.txt

