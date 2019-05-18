#!groovy

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

// this script is taken from olamy works on archiva-jenkins-lib for the Apache Archiva project
def call(Map params = [:]) {
    // variable needed for apidoc
    def myAnt = ""
    def apidocurl = ""
    def date  = ""
    def atomdate = ""
    def jdktool = ""
    pipeline {
        options {
            buildDiscarder(logRotator(numToKeepStr: '1'))
            disableConcurrentBuilds() 
        }
        agent { node { label 'ubuntu' } }
        stages{
            stage("Test"){
                agent { node { label 'ubuntu' } }
                options { timeout(time: 180, unit: 'MINUTES') }
                steps{
                    script {
                        // test if we can do that 
                        sh 'curl "https://gitbox.apache.org/repos/asf?p=netbeans-jenkins-lib.git;a=blob_plain;f=meta/netbeansrelease.json" -o netbeansrelease.json'
                        def releaseData = readJSON file: 'netbeansrelease.json'
                        sh 'rm -f netbeansrelease.json'
                        myAnt = releaseData[env.BRANCH_NAME].ant;
                        apidocurl = releaseData[env.BRANCH_NAME].apidocurl
                        date  = releaseData[env.BRANCH_NAME].releaseDate
                        atomdate = releaseData[env.BRANCH_NAME].atomreleaseDate
                        jdktool = releaseData[env.BRANCH_NAME].jdk
 
                    }
                }
            }
            stage ("Apidoc") {
                tools {
                    jdk jdktool
                }
                steps {
                    withAnt(installation: myAnt) {                  
                        script {
                            sh 'ant'
                            if (env.BRANCH_NAME=="master") {
                                sh "ant build-javadoc -Djavadoc.web.zip=${env.WORKSPACE}/WEBZIP.zip"
                            } else {
                                sh "ant build-javadoc -Djavadoc.web.root='${apidocurl}' -Dmodules-javadoc-date='${date}' -Datom-date='${atomdate}' -Djavadoc.web.zip=${env.WORKSPACE}/WEBZIP.zip"
                            }
                        }
                        
                    }
                    archiveArtifacts 'WEBZIP.zip'
                    
                }
            }
        }
        post {
            cleanup {
                cleanWs() // deleteDirs: true, notFailBuild: true, patterns: [[pattern: '**/.repository/**', type: 'INCLUDE']]
            }
            success {
                slackSend (channel:'#netbeans-builds', message:"SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL}) ",color:'#00FF00')
            }
            failure {
                slackSend (channel:'#netbeans-builds', message:"FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'  (${env.BUILD_URL})",color:'#FF0000')
            }
            
        }
    }
}
