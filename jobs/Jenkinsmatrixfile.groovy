/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

pipeline {
    options {
        buildDiscarder(logRotator(numToKeepStr: '2'))
        disableConcurrentBuilds()
    }
    agent { node { label 'ubuntu' } }

    stages {

        stage("clone and prepare build") {
            tools {
                jdk 'jdk_11_latest'
                ant 'ant_latest'
            }
            steps {

                git 'https://github.com/apache/netbeans'
                sh 'rm -f *.json* '
                sh 'rm -f nbbuild/NetBeans-dev-Netbeans/*.zip'
                sh 'wget -Onetbeansrelease.json "https://gitbox.apache.org/repos/asf?p=netbeans-jenkins-lib.git;a=blob_plain;f=meta/netbeansrelease.json" '
                sh "ant build -Dmetabuild.jsonurl=file:netbeansrelease.json -Dzip.dir=${WORKSPACE}/zip"
                sh "cd ${WORKSPACE}/zip/ && mv *.zip ${WORKSPACE}/zip/NetBeansIDE.zip "
                stash includes: "zip/NetBeansIDE.zip", name: 'idebuildzip'
                sh 'ant build-test-dist -Dmetabuild.jsonurl=file:netbeansrelease.json'
                stash includes: 'nbbuild/build/testdist.zip', name: 'testbuildzip'

            }
        }
        stage ("EffectiveMatrixClustersUNITQA")
        {
            matrix {
                agent { node { label 'ubuntu' } }

                axes {
                    axis {
                        name 'JDK'
                        values 'jdk_1.8_latest', 'jdk_11_latest', 'jdk_17_latest', 'jdk_21_latest','jdk_23_latest'
                    }
                    axis {
                        name 'CLUSTER'
                        values 'platform','ide'
                    }
                }
                stages {
                    stage('test') {
                        tools {
                            ant 'ant_latest'
                        }
                        steps {
                            withAnt(jdk:"${env.JDK}") {
                                unstash 'testbuildzip'
                                unstash 'idebuildzip'
                                unzip  zipFile: 'nbbuild/build/testdist.zip', dir:'testdir', quiet: true
                                unzip  zipFile: 'zip/NetBeansIDE.zip', dir:'netbeans', quiet: true
                                sh "mkdir -p ${WORKSPACE}/result/unit/${env.JDK}/${env.CLUSTER} "
                                sh 'java -version'
                                //sh 'ant -version'
                                // this is not finished
                                wrap([$class: 'Xvfb', additionalOptions: '', assignedLabels: '', displayNameOffset: 0, autoDisplayName:true, installationName: 'Xvfb', parallelBuild: true, screen: '']) {
                                    // echo to return 0 and go further
                                    sh "ant -f ${WORKSPACE}/testdir/build.xml -Dtest-sys-prop.ignore.random.failures=true -Dtest.results.dir=${WORKSPACE}/result/unit/${env.JDK}/${env.CLUSTER} -Dtest.types=unit -Dtest.clusters=${env.CLUSTER} -Dnetbeans.dest.dir=${WORKSPACE}/netbeans/netbeans || echo Failed "
                                    sh "ant -f ${WORKSPACE}/testdir/build.xml -Dtest-sys-prop.ignore.random.failures=true -Dtest.results.dir=${WORKSPACE}/result/unit/qa/${env.JDK}/${env.CLUSTER} -Dtest.types=qa-functional -Dtest.clusters=${env.CLUSTER} -Dnetbeans.dest.dir=${WORKSPACE}/netbeans/netbeans || echo Failed "
                                
                                }
                                // do not use TESTS- as it's redundant with TEST- (inverted to check report readability)
                                //sh "ant -f ${WORKSPACE}/testdir/build.xml -Dtest.clusters=${env.CLUSTER} -Dtest.types=qa-functional -Dnetbeans.dest.dir=${WORKSPACE}/netbeans/netbeans"
                                // html can be done but unusable from jenkins
                                archiveArtifacts artifacts: "result/unit/${env.JDK}/${env.CLUSTER}/**/*"
                                archiveArtifacts artifacts: "result/unit/qa/${env.JDK}/${env.CLUSTER}/**/*"
                            }
                        }

                    }
                }

            }
        }
        stage ("html index") {

            steps {
                script { 
                    // generate an index 
                    // matrix axis  (jdk and cluster) should be copied here matrix do not allow variable
                    def jdk = ['jdk_1.8_latest', 'jdk_11_latest', 'jdk_17_latest','jdk_21_latest','jdk_23_latest']
                    def cluster = ['platform','ide']
                    
                    def content = '<!doctype html><html lang="en"><head><meta charset="utf-8"><title>testing website</title></head><body><h1>Unit and QA functional testing for Apache NetBeans</h1>'
                    content += '<h2>Unit test</h2>'
                    
                    content += '<ul>'
                    for (int i = 0; i < cluster.size(); ++i) {
                        content += '<li>'+cluster[i]+'<ul>'
                        for (int j = 0; j < jdk.size(); ++j) {
                            content += '<li>'+'<a href="./'+jdk[j]+'/'+cluster[i]+'/html/index.html"'+' target="_blank" rel="noopener noreferrer" >'+cluster[i]+' on jdk "'+jdk[j]+'"<a/></li>'
                        }
                        content += '</ul></li>'     
                    }
                    content += '</ul>' 
                    content += '<h2>QA functional</h2>' 
                    content += '<ul>' 
                    for (int i = 0; i < cluster.size(); ++i) {
                        content += '<li>'+cluster[i]+'<ul>'
                        for (int j = 0; j < jdk.size(); ++j) {
                            content += '<li>'+'<a href="./qa/'+jdk[j]+'/'+cluster[i]+'/html/index.html"'+' target="_blank" rel="noopener noreferrer">'+cluster[i]+' on jdk "'+jdk[j]+'"<a/></li>'
                        }
                        content += '</ul></li>'     
                    }
                    content += '</body></html>'
                    writeFile file: 'result/unit/index.html', text:   content 
                    archiveArtifacts artifacts: "result/unit/*.html"
                }
                
            }
        }
        /* */
    }
    post {
        cleanup {
            cleanWs disableDeferredWipeout: true, deleteDirs: true
        }
    }
}
