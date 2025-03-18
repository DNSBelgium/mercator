mvn package -DskipTests -Dsnyk.skip

# This script illustrates how you can run Mercator in batch mode.
# It will process the the file specified in the 'mercator.input.file' property, which by default is ${user.home}/mercator/input.csv

#java -Dspring.profiles.active=local,batch -jar target/mercator-2.0.0-SNAPSHOT.jar

# you could specify another input file like this:

export MERCATOR_INPUT=$(pwd)/src/test/resources/test-data/delimited.csv
echo "MERCATOR_INPUT:${MERCATOR_INPUT}"
cat ${MERCATOR_INPUT}

java -Dspring.profiles.active=local,batch -Dmercator.input.file=${MERCATOR_INPUT} -jar target/mercator-2.0.0-SNAPSHOT.jar

EXIT_CODE=$?
echo "EXIT_CODE: ${EXIT_CODE}"