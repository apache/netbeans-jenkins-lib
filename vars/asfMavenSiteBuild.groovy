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
    def jdk = params.jdk
    def mvnName = params.mvnName 
    
    def defaultPublishers = [artifactsPublisher(disabled: false), junitPublisher(ignoreAttachments: false, disabled: false),
        findbugsPublisher(disabled: true), openTasksPublisher(disabled: true),
        dependenciesFingerprintPublisher(disabled: false), invokerPublisher(disabled: true),
        pipelineGraphPublisher(disabled: false), mavenLinkerPublisher(disabled: false)]

    def publishers = params.containsKey('publishers') ? params.publishers : defaultPublishers
    def scmUrl = scm.getUserRemoteConfigs()[0].getUrl()

    pipeline {
        agent any
	triggers {
            pollSCM('H/5 * * * * ')
	}
        stages{
            stage("Build site") {
                agent { node { label 'ubuntu && !nocredentials' } }
                options { timeout(time: 120, unit: 'MINUTES') }                
                steps{
                    mavenBuild( jdk, 'clean install site:site site:stage', mvnName, publishers,true)        
                }
            }   
            stage("Publish site") {
                agent { node { label 'git-websites' } }
                steps{
                    sh 'rm -rf build'
                    sh "git clone --depth 1 --branch asf-site ${scmUrl} build"
                    sh "cd build;mkdir $BRANCH_NAME"
                    dir ("build/$BRANCH_NAME") {
                        unstash 'site'
                    }
                    dir ('build') {
                        sh 'git add .'
                        sh 'echo `git commit -m "site build"`'
                        sh "git push ${scmUrl} asf-site"
                    }
                    
                    
                    
                }
            }
        }
        post {
            always {
                cleanWs()
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
def mavenBuild(jdk, cmdline, mvnName, publishers,archive) {
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
        sh "mvn -V -B -U -e -DskipBrowserTests -Dmaven.test.failure.ignore=true $cmdline "
	//sh "cd target/staging; zip ../../WEBSITE.zip ."
    }
    if (archive) {
	stash includes:'./target/staging/*', name: 'site'
    }
}

def notifyBuild(String buildStatus) {
    // default the value
    buildStatus = buildStatus ?: "UNKNOWN"
    def color
    if (buildStatus == 'STARTED') {
        color = '#F0F0F0'
    } else if (buildStatus == 'SUCCESS' || buildStatus=='Fixed') {
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

