#!/bin/sh
docker container stop solr_test_entando && docker container rm solr_test_entando
docker run -d -p 8984:8983 --name solr_test_entando solr solr-precreate entando
