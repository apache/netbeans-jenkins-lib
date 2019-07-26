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
    // Faster build and reduces IO needs
    properties([
            disableConcurrentBuilds(),
            durabilityHint('PERFORMANCE_OPTIMIZED'),
            buildDiscarder(logRotator(numToKeepStr: '5', artifactNumToKeepStr: '3'))
        ])

    // now determine params
    def jdk = params.containsKey('jdk') ? params.jdk : 'JDK 1.8 (latest)'
    // use the cmdLine parameter otherwise default depending on current branch
    def cmdline = params.containsKey('cmdline') ? params.cmdline : (env.BRANCH_NAME == 'master'?"clean deploy site:jar":"clean install")
    def mvnName = params.containsKey('mvnName') ? params.mvnName : 'Maven 3.5.2'


    def defaultPublishers = [artifactsPublisher(disabled: false), junitPublisher(ignoreAttachments: false, disabled: false),
        findbugsPublisher(disabled: true), openTasksPublisher(disabled: true),
        dependenciesFingerprintPublisher(disabled: false), invokerPublisher(disabled: true),
        pipelineGraphPublisher(disabled: false), mavenLinkerPublisher(disabled: false)]

    def publishers = params.containsKey('publishers') ? params.publishers : defaultPublishers


    pipeline {
        agent any
        stages{
            stage("Build"){
                agent { node { label 'ubuntu' } }
                options { timeout(time: 120, unit: 'MINUTES') }
                steps{
                    mavenBuild( jdk, cmdline, mvnName, publishers)
                }
            }
        }
        post {
            always {
                cleanWs() // deleteDirs: true, notFailBuild: true, patterns: [[pattern: '**/.repository/**', type: 'INCLUDE']]
            }
            unstable {
                script{
                    notifyBuild( "Unstable Build ")
                }
            }
            failure {
                script{
                    notifyBuild( "Error in build ")
                }
            }
            success {
                script {
                    def previousResult = currentBuild.previousBuild?.result
                    if (previousResult && !currentBuild.resultIsWorseOrEqualTo( previousResult ) ) {
                        notifyBuild( "Fixed" )
                    }
                }
            }
        }
    }
}

/**
 * To other developers, if you are using this method above, please use the following syntax.
 *
 * mavenBuild("<jdk>", "<profiles> <goals> <plugins> <properties>"
 *
 * @param jdk the jdk tool name (in jenkins) to use for this build
 * @param cmdline the command line in "<profiles> <goals> <properties>"`format.
 * @paran mvnName maven installation to use
 * @param publishers array of publishers to configure (need to be defined as we publisherStrategy: 'EXPLICIT')
 * @return the Jenkinsfile step representing a maven build
 */
def mavenBuild(jdk, cmdline, mvnName, publishers) {
    def localRepo = "../.maven_repositories/${env.EXECUTOR_NUMBER}" // ".repository" //
    //def settingsName = 'archiva-uid-jenkins'
    def mavenOpts = '-Xms1g -Xmx4g -Djava.awt.headless=true'

    withMaven(
        maven: mvnName,
        jdk: "$jdk",
        options: publishers,
        publisherStrategy: 'EXPLICIT',
        //globalMavenSettingsConfig: settingsName,
        mavenOpts: mavenOpts,
        mavenLocalRepo: localRepo) {
        // Some common Maven command line + provided command line
        sh "mvn -V -B -U -e -Dmaven.test.failure.ignore=true $cmdline "
    }
 
}

def notifyBuild(String buildStatus) {
    // default the value
    buildStatus = buildStatus ?: "UNKNOWN"
    def color
    if (buildStatus == 'STARTED') {
        color = '#F0F0F0'
    } else if (buildStatus == 'SUCCESS') {
        color = '#00FF00'
    } else if (buildStatus == 'UNSTABLE') {
        color = '#ffff50'
    } else if (buildStatus == 'UNKNOWN') {
        color = '#a0a0a0'
    }else {
        color = '#FF0000'
    }
    slackSend (channel:'#netbeans-builds', message:"${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL}) ",color: color)
     

     
}

// vim: et:ts=2:sw=2:ft=groovy

