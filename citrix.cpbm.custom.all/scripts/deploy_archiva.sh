#!/bin/bash

mvn clean package deploy -Dmaven.test.skip=true
