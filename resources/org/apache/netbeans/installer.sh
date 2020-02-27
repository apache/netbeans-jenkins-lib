#!/bin/bash  \n 
                                        BASE_DIR=`pwd` \n
                                        NB_ALL=$BASE_DIR \n
                                        export BASE_DIR NB_ALL

                                        DIST=$BASE_DIR/dist
                                        export DIST

                                        if [ -d $DIST ] ; then
                                        rm -rf $DIST
                                        fi

                                        rm -rf NBI-cache

                                        mkdir -p $DIST/zip/moduleclusters
                                        mkdir -p $DIST/logs

                                        ARTIFACT=netbeans-*-bin
                                        BIN_NAME=`ls $ARTIFACT.zip`
                                        BINARY_NAME=`echo "${BIN_NAME%%.zip*}"`


                                        #create cluster zip files
                                        rm -rf temp
                                        unzip $BINARY_NAME.zip -d temp
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
                                        NB_BUILD_NUMBER=200224
                                        BUILDNUMBER=$NB_BUILD_NUMBER
                                        DATESTAMP=$BUILDNUMBER
                                        NB_VER_NUMBER=11.3
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

                                        OUTPUT_DIR=${NB_ALL}/dist/installers
                                        export OUTPUT_DIR

                                        DONT_SIGN_INSTALLER=y
                                        export DONT_SIGN_INSTALLER

                                        bash -x $NB_ALL/nbbuild/newbuild/build-nbi.sh