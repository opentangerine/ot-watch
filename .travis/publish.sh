#!/bin/bash
if [ -z "$SONATYPE_USERNAME" ]
then
    echo "Error: please set SONATYPE_USERNAME and SONATYPE_PASSWORD environment variable"
    exit 1
fi

if [ -z "$SONATYPE_PASSWORD" ]
then
    echo "Error: please set SONATYPE_PASSWORD environment variable"
    exit 1
fi

if [ ! -z "$TRAVIS_TAG" ]
then
    echo "Tagged build. Overriding project version = $TRAVIS_TAG"
    mvn org.codehaus.mojo:versions-maven-plugin:2.3:set -DnewVersion=$TRAVIS_TAG 1>/dev/null 2>/dev/null
else
    echo "Not on a tag. Publishing snapshot version."
fi

openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in $TRAVIS_DIR/pubring.gpg.enc -out $TRAVIS_DIR/pubring.gpg -d
openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in $TRAVIS_DIR/secring.gpg.enc -out $TRAVIS_DIR/secring.gpg -d

mvn clean deploy -Possrh --settings $TRAVIS_DIR/settings.xml -DskipTests=true -B -U