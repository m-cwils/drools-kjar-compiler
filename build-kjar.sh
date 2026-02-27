#!/usr/bin/env bash
# build-kjar.sh – convenience wrapper to compile a folder of .drl rules into a KJar.
#
# Usage:
#   ./build-kjar.sh <rules-folder> <output.jar>
#
# Example:
#   ./build-kjar.sh ./my-rules ./target/my-rules.jar
#
# Prerequisites:
#   1. Run 'mvn package' once to build the fat jar:
#        mvn package -DskipTests
#   2. Then use this script to compile any .drl folder.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FAT_JAR="${SCRIPT_DIR}/target/drools-kjar-compiler-1.0.0-jar-with-dependencies.jar"

if [[ ! -f "${FAT_JAR}" ]]; then
  echo "Fat jar not found. Building project first..."
  mvn -f "${SCRIPT_DIR}/pom.xml" package -DskipTests
fi

if [[ $# -lt 2 ]]; then
  echo "Usage: $0 <rules-folder> <output.jar>"
  exit 1
fi

RULES_FOLDER="$1"
OUTPUT_JAR="$2"

java -jar "${FAT_JAR}" "${RULES_FOLDER}" "${OUTPUT_JAR}"
