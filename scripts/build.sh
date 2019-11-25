#!/bin/bash

./gradlew clean build --refresh-dependencies || exit 1

echo "================= Building Docker Images ============================="
VERSION=$(./gradlew :printVersion --console plain | grep -A1 ":printVersion" | grep -v ":printVersion" | tr -d '\n')

echo "Building docker image for version $VERSION."

docker build --build-arg VERSION="$VERSION" --pull --rm --tag "netty-ws-leak:latest" . || exit 1
