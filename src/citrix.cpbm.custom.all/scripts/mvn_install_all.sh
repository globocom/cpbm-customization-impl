#!/bin/bash

cd ../
mvn clean install -Dmaven.test.skip=true
mkdir -p target
cp ../citrix.cpbm.custom.model/target/citrix.cpbm.custom.model*.jar target/
cp ../citrix.cpbm.custom.common/target/citrix.cpbm.custom.common*.jar target/
cp ../citrix.cpbm.custom.portal/target/citrix.cpbm.custom.portal*.jar target/