#!/bin/bash

rm -rf testsReport
mkdir testsReport

#Remove any paused state
rm ../main/assets/jobattributes.json ../main/assets/current_script.js ../main/assets/job_project_certificate.json

jshint ../main/assets/ --jslint-reporter > testsReport/jshint.xml
sed -E 's?<file name="(.*)\?">?<file name="'`pwd`'/\1">?' testsReport/jshint.xml > testsReport/jshint-proper.xml
jshint ../main/assets/ --checkstyle-reporter > testsReport/checkstyle-jshint.xml
sed -E 's?<file name="(.*)\?">?<file name="'`pwd`'/\1">?' testsReport/checkstyle-jshint.xml > testsReport/checkstyle-jshint-proper.xml

gjslint --strict --custom_jsdoc_tags callback,module --max_line_length=100 -r ../main/assets/ -e node_modules > testsReport/errorsGsLint
php ../../../../utils/gjslintReport.php testsReport/errorsGsLint testsReport/gjslint.xml

cp ../internal/assets/jobserver-cacert.pem ../main/assets/
cp ../internal/assets/environment.js ../main/assets/

mocha -r blanket --reporter mocha-cobertura-reporter   > testsReport/cobertura.xml
mocha -r blanket -R html-cov   > testsReport/coverage.html
mocha -r blanket -t 10000 -R tap --check-leaks > testsReport/tap.out
EXIT_CODE=`echo "$?"`
FILES=./test/test*.js
for f in $FILES
do
  out=$(basename "$f")
  out="${out%.*}"
  mocha -r blanket -R tap -t 10000 $f> testsReport/${out}.tap
done

rm ../main/assets/environment.js ../main/assets/jobserver-cacert.pem

if [ $EXIT_CODE -eq '0' ]; then
	echo "******************"
    echo "*All tests passed*"
    echo "******************"
else
	echo "*******************"
    echo "****Test failed****"
    echo "*******************"
fi
