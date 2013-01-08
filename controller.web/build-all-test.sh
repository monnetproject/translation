#!/bin/bash

#mkdir deploy

rm src/main/resources/load

ln -s /home/john/projects/config_monnet/translate/generic/load src/main/resources/load

mvn clean install

cp target/eu.monnetproject.translation.controller.web.war deploy/translate_generic_test.war


rm src/main/resources/load

ln -s /home/john/projects/config_monnet/translate/financial/load src/main/resources/load

mvn clean install

cp target/eu.monnetproject.translation.controller.web.war deploy/translate_financial_test.war


rm src/main/resources/load

ln -s /home/john/projects/config_monnet/translate/publicservices/load src/main/resources/load

mvn clean install

cp target/eu.monnetproject.translation.controller.web.war deploy/translate_publicservices_test.war

