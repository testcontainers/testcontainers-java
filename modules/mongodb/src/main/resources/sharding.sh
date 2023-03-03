#!/bin/bash

CONFIGSVR=/tmp/mongod/configsvr
SHARDSVR=/tmp/mongod/shardsvr

function retry() {
    COUNT=${COUNT:-0}
    if [ $COUNT == 5 ]
    then
        echo Failed $COUNT attempts
        exit 1
    fi

    sleep $COUNT
    echo "Attempt #$[ $COUNT  + 1 ] '$*' "
    eval $*
    if [ $? -ne 0 ]
    then
        COUNT=$[ $COUNT + 1 ]
        retry $*
    fi
    unset COUNT
}

function initReplSet() {
    PORT=$1
    COUNT=${2:-1}

    CMD="mongosh --quiet --port $PORT --eval \"if(db.adminCommand({replSetGetStatus: 1})['myState'] != 1) quit(900)\""
    eval $CMD
    retVal=$?
    if [ $retVal -ne 0 -a $COUNT -ne 5 ]
    then
        echo "Initiating replSet (attempt $COUNT)"
        mongosh --quiet --port $PORT --eval 'rs.initiate();'
        if [ $? -ne 0 ]
        then
            sleep $COUNT
            initReplSet $PORT $[ $COUNT + 1 ]
        fi
    fi
    unset COUNT
}

rm -rf $CONFIGSVR $SHARDSVR
mkdir -p $CONFIGSVR
mkdir -p $SHARDSVR

echo "Starting configsvr"
mongod --bind_ip_all --configsvr --port 27019 --replSet configsvr-rs --dbpath $CONFIGSVR --logpath /tmp/configsvr.log &
echo "Initiating configsvr replSet"
initReplSet 27019

echo "Starting shardsvr"
mongod --bind_ip_all --shardsvr --port 27018 --replSet shardsvr-rs --dbpath $SHARDSVR --logpath /tmp/shardsvr.log &

echo "Initiating shardsvr replSet"
initReplSet 27018

echo "Starting mongos"
mongos --bind_ip_all --configdb configsvr-rs/localhost:27019 --logpath /tmp/mongos.log &

echo "Adding a shard"
retry "mongosh --eval 'sh.addShard(\"shardsvr-rs/`hostname`:27018\");'"

echo "mongos ready"
mongosh --quiet tctest --eval "db.testcollection.insertOne({});"
sleep 36000
