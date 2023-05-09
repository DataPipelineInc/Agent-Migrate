#!/bin/bash
set -e

VERSION=$(./gradlew properties -q | grep "version:" | awk '{print $2}')
DIR_NAME="agent-migrate"
ROOT_DIR=$(pwd)/$DIR_NAME
CONF_DIR=$ROOT_DIR/conf

JAR_NAME="agent-migrate-${VERSION}.jar"
sed -i '' 's/agent-migrate.*.jar/'"${JAR_NAME}"'/g' "$(pwd)/start.sh"

help() {
cat << EOF
  Usage: -[j]
    Build project and Tar.
       -j only build jar, do not tar
EOF
}

while getopts hj opt; do
  case $opt in
    h)
      help
      exit 0
      ;;
    j)
      ONLY_JAR=true
      echo "Only build jar"
      ;;
    ?)
      help
      exit 1
      ;;
  esac
done

./gradlew clean
echo "execute gradle clean"

./gradlew jar
echo "execute gradle jar"

if [[  -n "$ONLY_JAR" ]]; then
  cp build/libs/agent-migrate-"${VERSION}".jar "$(pwd)"
else
  if [ ! -e "$ROOT_DIR" ]; then
    mkdir "$ROOT_DIR"
  fi
  rm -f "$ROOT_DIR"/*.jar
  cp build/libs/agent-migrate-"${VERSION}".jar "$DIR_NAME"
  cp "$(pwd)/start.sh" "$DIR_NAME"
  if [ ! -e "$CONF_DIR" ]; then
    mkdir "$CONF_DIR"
  fi
  cp src/main/kotlin/com/datapipeline/agent/conf/config.yml "$CONF_DIR"
  tar cvf agent-migrate-linux-"${VERSION}".tar "$DIR_NAME"
fi
