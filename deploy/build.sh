#!/bin/sh
# Сборка всех модулей (локально) и копирование артефактов в artifacts/.
set -eu
. "$(dirname "$0")/env.sh"

cd "$LAB_HOME"
sh ./mvnw -q package "$@"
mkdir -p "$ARTIFACTS_DIR"
cp flats-service/target/api.war agency-service/target/agency.war client/target/client.jar "$ARTIFACTS_DIR/"
ls -l "$ARTIFACTS_DIR"
