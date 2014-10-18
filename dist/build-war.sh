
WAR_NAME="backend-eu.war"

cd $(dirname "$0")
DIST_DIR=$(pwd -P)
echo $DIST_DIR

cd ../war

zip -r --filesync --no-dir-entries ../dist/$WAR_NAME .
