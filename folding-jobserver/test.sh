#!/bin/bash
#
# Licensed under the LICENSE.
# Copyright 2017, Sony Mobile Communications Inc.
#

success=0
function isok {
    if [ $(echo "$?") -eq '0' ]
    then
        ((success &= 1))
        echo "OK..."
    else
        ((success &= 0))
        echo "FAIL..."
    fi
}

echo "Setup..."
rm -rf testReports
mkdir testReports

echo "jshint..."
jshint ./src --jslint-reporter > testReports/jshint.xml
isok

sed -E 's?<file name="(.*)\?">?<file name="'`pwd`'/\1">?' testReports/jshint.xml > testReports/jshint-proper.xml

echo "checkstyle..."
jshint ./src --checkstyle-reporter > testReports/checkstyle-jshint.xml
isok

sed -E 's?<file name="(.*)\?">?<file name="'`pwd`'/\1">?' testReports/checkstyle-jshint.xml > testReports/checkstyle-jshint-proper.xml

echo "gjslint..."
gjslint --strict --custom_jsdoc_tags callback,module --max_line_length=100 -r src/ > testReports/errorsGsLint
isok

. ./test/test-keys.sh

setupkeys

echo "mocha-cobertura..."
mocha -r blanket --reporter mocha-cobertura-reporter > testReports/cobertura.xml
isok

# html-cov
# nodejs node_modules/mocha/bin/mocha -r blanket -R html-cov > testsReport/coverage.html
# isok

echo "mocha check-leaks..."
mocha -r blanket -t 10000 -R tap --check-leaks > testReports/tap.out
isok

FILES=./test/test*.js
for f in $FILES
do
  out=$(basename "$f")
  out="${out%.*}"

  echo "$out"
  mocha -r blanket -R tap -t 10000 $f> testReports/${out}.tap
  isok
done

echo "jsdoc..."
jsdoc -r ./src -d testReports/doc
isok

cleanupkeys
if (($success))
then
    echo "TESTS OK!!! Congrats!"
else
    echo "Tests failed!"
fi
