#!/bin/bash

CONFIGSVR=/tmp/mongod/configsvr
SHARDSVR=/tmp/mongod/shardsvr

function retry {
    eval $*
    retVal=$?
    if [ $retVal -ne 0 ]
    then
        COUNT=1
        while [ $retVal -ne 0 -a $COUNT != 5 ]
        do
            sleep $COUNT
            COUNT=$[ $COUNT + 1 ]
            echo "Retry [$COUNT] '$*' "
            eval $*
            retVal=$?
        done
        if [ $COUNT == 5 ]
        then
            exit 1
        fi
    fi
}

rm -rf $CONFIGSVR $SHARDSVR
mkdir -p $CONFIGSVR
mkdir -p $SHARDSVR

echo "Staring configsvr"
mongod --configsvr --port 27019 --replSet configsvr-rs --dbpath $CONFIGSVR --syslog &
echo "Initiating configsvr replSet"
retry "mongosh --port 27019 --eval 'rs.initiate();'"

echo "Starting shardsvr"
mongod --shardsvr --port 27018 --replSet shardsvr-rs --dbpath $SHARDSVR --syslog &

echo "Initiating shardsvr replSet"
retry "mongosh --port 27018 --eval 'rs.initiate();'"

echo "Starting mongos"
mongos --configdb configsvr-rs/localhost:27019 --syslog &
echo "Adding a shard"
retry "mongosh --eval 'sh.addShard(\"shardsvr-rs/localhost:27018\");'"


echo "mongos ready"
mongosh
