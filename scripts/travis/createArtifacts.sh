#!/usr/bin/env bash

./gradlew clean createZip -x check #skiping checks as we will do the same in above stage

exit "$?"
