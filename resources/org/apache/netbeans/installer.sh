#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# 
## param 1 workspace filename for binaries-zip 
## param 2 version number xx.y (mandatory)
## param 3 timestamp  YYMMDD

BASE_DIR=`pwd`
NB_ALL=$BASE_DIR
export BASE_DIR NB_ALL

`echo "$1 $2 $3" > parameters.info`

DIST=$BASE_DIR/dist
export DIST

if [ -d $DIST ] ; then
rm -rf $DIST
fi

rm -rf NBI-cache

mkdir -p $DIST/zip/moduleclusters
mkdir -p $DIST/logs

ARTIFACT=netbeans-*-bin
BIN_NAME=`basename $1`
BINARY_NAME=`echo "${BIN_NAME%%.zip*}"`


#create cluster zip files
rm -rf temp
unzip $1 -d temp
cd temp
mkdir javase
mkdir javase/netbeans
mkdir javaee
mkdir javaee/netbeans
mkdir webcommon
mkdir webcommon/netbeans
mkdir php
mkdir php/netbeans
mkdir extide
mkdir extide/netbeans

cd netbeans
#java
mv apisupport ../javase/netbeans
mv ergonomics ../javase/netbeans
mv java ../javase/netbeans
mv javafx ../javase/netbeans
mv profiler ../javase/netbeans

#javaee
mv enterprise ../javaee/netbeans
mv groovy ../javaee/netbeans

#webcommon
mv webcommon ../webcommon/netbeans

#php
mv php ../php/netbeans

#websvccommon
mv websvccommon ../extide/netbeans

#create cluster zip files
cd ..
echo `pwd`
echo $BINARY_NAME
zip -r $BINARY_NAME-base.zip netbeans
mv $BINARY_NAME-base.zip ..

echo `pwd`

cd javase
zip -r $BINARY_NAME-java.zip netbeans
mv $BINARY_NAME-java.zip ../..
cd ..

cd javaee
zip -r $BINARY_NAME-enterprise.zip netbeans
mv $BINARY_NAME-enterprise.zip ../..
cd ..

cd php
zip -r $BINARY_NAME-php.zip netbeans
mv $BINARY_NAME-php.zip ../..
cd ..

cd webcommon
zip -r $BINARY_NAME-webcommon.zip netbeans
mv $BINARY_NAME-webcommon.zip ../..
cd ..

cd extide
zip -r $BINARY_NAME-websvccommon.zip netbeans
mv $BINARY_NAME-websvccommon.zip ../..
cd ../..

rm -rf temp

mv $BINARY_NAME-*.zip $DIST/zip/moduleclusters

export BINARY_NAME

cd $BASE_DIR
NB_BUILD_NUMBER=$3
BUILDNUMBER=$NB_BUILD_NUMBER
DATESTAMP=$BUILDNUMBER
NB_VER_NUMBER=$2
BASENAME_PREFIX=Apache-NetBeans-$NB_VER_NUMBER-bin
BUILD_DESC=$BASENAME_PREFIX
export NB_VER_NUMBER BUILDNUMBER BASENAME_PREFIX NB_BUILD_NUMBER DATESTAMP BUILD_DESC

MAC_PATH=$DIST
#export MAC_PATH

MAC_LOG_NEW=$DIST/logs/native_mac-$BUILDNUMBER.log
export MAC_LOG_NEW
BUILD_NB=1
BUILD_NETBEANS=0
BUILD_NBJDK6=0
BUILD_NBJDK7=0
BUILD_NBJDK8=0
BUILD_NBJDK11=0

export BUILD_NETBEANS BUILD_NB
export BUILD_NBJDK6 BUILD_NBJDK7 BUILD_NBJDK8 BUILD_NBJDK11
BUNDLE_JDK_PLATFORM=
export BUNDLE_JDK_PLATFORM


DONT_SIGN_INSTALLER=y
export DONT_SIGN_INSTALLER

bash -x $NB_ALL/nbbuild/newbuild/build-nbi.sh

## cleanup cache
rm -rf NBI-cache
