#!/bin/sh
# Starts wallpi on http://localhost:8080 and opens it in the browser.
# Builds first when no build exists or when called with --build.
set -e

cd "$(dirname "$0")/wallpi"

if [ "$1" = "--build" ] || [ ! -f target/quarkus-app/quarkus-run.jar ]; then
    mvn -q -B package -DskipTests
fi

(
    until curl -s -o /dev/null http://localhost:8080/; do sleep 0.5; done
    open http://localhost:8080
) &

# run from wallpi/ so the relative frontend path ../app/src resolves
exec java -jar target/quarkus-app/quarkus-run.jar
