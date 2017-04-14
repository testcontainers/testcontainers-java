#!/usr/bin/env bash

rm fakejar.jar
zip -r fakejar.jar META-INF/ recursive/
mv fakejar.jar repo/fakejar/fakejar/0/fakejar-0.jar