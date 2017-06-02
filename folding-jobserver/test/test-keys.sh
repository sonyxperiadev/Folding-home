#!/bin/bash
#
# Licensed under the LICENSE.
# Copyright 2017, Sony Mobile Communications Inc.
#

PEMS=(test/Util/client-cert.pem
      test/Util/client-key.pem
      test/Util/jobserver-cacert.pem
      test/Util/pmclient-cert.pem
      test/Util/pmclient-key.pem
      test/Util/pmserver-cacert.pem
      test/Util/pmserver-cert.pem
      test/Util/pmserver-key.pem
      test/Util/pmserver-wrongCN-cert.pem
      test/Util/pmserver-wrongCN-key.pem)

function client {
    openssl req \
            -x509 \
            -nodes \
            -sha1 \
            -newkey rsa:2048 \
            -keyout test/Util/client-key.pem \
            -out test/Util/client-cert.pem \
            -subj '/CN=node-client' \
            -days 1825
}

function jobserver {
    openssl req \
            -x509 \
            -sha1 \
            -nodes \
            -newkey rsa:2048 \
            -out test/Util/jobserver-cacert.pem \
            -subj '/CN=pmca' \
            -days 3650
}

function pmclient {
    openssl req \
            -x509 \
            -nodes \
            -sha1 \
            -newkey rsa:2048 \
            -config test/Util/pmclient.cnf \
            -keyout test/Util/pmclient-key.pem \
            -out test/Util/pmclient-cert.pem \
            -subj '/CN=pmclient/' \
            -extensions ssl_client \
            -days 1825
}

function pmserverca {
    openssl req \
            -x509 \
            -sha1 \
            -nodes \
            -newkey rsa:2048 \
            -keyout test/Util/pmserver-cakey.pem \
            -out test/Util/pmserver-cacert.pem \
            -subj '/CN=localhost /CN=www.example.com/projectAttributes={"server_address":"localhost","server_port":23456,"category":"default","max_job_count":1000,"run_time_limit":2,"execution_time_limit":48}' \
            -days 1825
}

function pmserver {
    mkdir -p test/Util/newcerts
    echo "01" > test/Util/serial
    touch test/Util/index.txt
    openssl req -sha1 \
        -newkey rsa:2048 \
        -config test/Util/pmserver.cnf \
        -keyout test/Util/pmserver-key.pem \
        -out test/Util/pmserver-cert-req.pem \
        -subj '/CN=www.example.com/projectAttributes={\"server_address\":\"www.example.com\",\"server_port\":23456,\"category\":\"default\",\"max_job_count\":1000,\"run_time_limit\":2,\"execution_time_limit\":48,\"storage_limit\":2}/' \
        -extensions ssl_client \
        -nodes
    openssl ca \
        -config test/Util/caconfig.cnf \
        -in test/Util/pmserver-cert-req.pem \
        -out test/Util/pmserver-cert.pem
    rm test/Util/pmserver-cert-req.pem
    openssl x509 -text -in test/Util/pmserver-cert.pem
}

function pmserverWrongCn {
    openssl req \
            -x509 \
            -sha1 \
            -nodes \
            -newkey rsa:2048 \
            -keyout test/Util/pmserver-wrongCN-key.pem \
            -out test/Util/pmserver-wrongCN-cert.pem \
            -subj '/CN=www.example.co' \
            -days 365
}

function createall {
    client
    jobserver
    pmclient
    pmserverca
    pmserver
    pmserverWrongCn
}

## main
function setupkeys {
    local current_pems=$(ls test/Util/*.pem)
    if [[ -z $current_pems ]]
    then
        createall
    else
        for f in "${PEMS[@]}";
        do
            local isin=0
            for l in $current_pems;
            do
                if [[ "$f" == "$l" ]]
                then
                    isin=1
                fi
            done;
            if [[ $isin -eq 0 ]]
            then
                echo "Missing cert $f. Creating the whole chain..."
                rm ./test/Util/*.pem
                createall
                break;
            fi
        done
    fi
    echo "Keys setup done..."
}

function cleanupkeys {
    rm privkey.pem
    rm test/Util/*.pem
    rm test/Util/index.txt*
    rm test/Util/serial*
    rm -rf test/Util/newcerts
    echo "Keys cleaned up..."
}
