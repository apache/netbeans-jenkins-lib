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

@groovy.transform.Field
def versionpath = ""
@groovy.transform.Field
def apidocurl = ""
@groovy.transform.Field
def date  = ""
@groovy.transform.Field
def atomdate = ""
@groovy.transform.Field
def version=""
@groovy.transform.Field
def rmversion=""
@groovy.transform.Field
def debversion=""
@groovy.transform.Field
def vsixversion=""
@groovy.transform.Field
def month=""
@groovy.transform.Field
def votecandidate=false
@groovy.transform.Field
def vote=""
@groovy.transform.Field
def mavenVersion=""
@groovy.transform.Field
def tooling=[:]
@groovy.transform.Field
def repopluginversion="14.2"
@groovy.transform.Field
def nbpackageversion="1.0-beta5"
@groovy.transform.Field
def branch=""
@groovy.transform.Field
def heavyrelease=true

def call(Map params = [:]) {
    // variable needed for apidoc


    pipeline {
        options {
            buildDiscarder(logRotator(numToKeepStr: '2'))
            disableConcurrentBuilds()
            timeout(time: 340, unit: 'MINUTES')
        }
// 
        agent { node { label 'ubuntu && !ephemeral' } }

        parameters {
            booleanParam(name: 'INSTALLERS', defaultValue: false, description: 'Build installers?')
            booleanParam(name: 'VSIX', defaultValue: false, description: 'Build VSCode plugin?')
            booleanParam(name: 'NIGHTLIES', defaultValue: false, description: 'Publish to nightlies.apache.org?')
        }

        stages {
            stage("Preparing Variable") {
                //agent { node { label 'ubuntu && !nocredentials' } }
                steps {
                    script {
                        // test if we can do that
                        sh 'curl "https://netbeans.apache.org/nbbuild/netbeansrelease.json" -L -o netbeansrelease.json'
                        def releaseInformation = readJSON file: 'netbeansrelease.json'
                        sh 'rm -f netbeansrelease.json'
                        branch = env.BRANCH_NAME
                        def githash = env.GIT_COMMIT
                        println '20231904'
                        println githash
                        println branch
                        // we need npm later test early
                        sh 'npm --version'
                        if (!releaseInformation[branch]) {
                            // no branch definined in json exit build
                            if (releaseInformation[branch.replace('vsnetbeans_preview_','release')]) {
                                // branch is release1234 for vsnetbeans_preview_1237
                                branch = branch.replace('vsnetbeans_preview_','release')
                            } else if (releaseInformation[branch.replace('vsnetbeans_','release')]) {
                                // branch is release1234 for vsnetbeans_1237
                                branch = branch.replace('vsnetbeans_','release')
                            }
                            else {
                                // no branch definined in json exit build
                                currentBuild.result = "FAILURE"
                                throw new Exception("No entry in json for $branch")
                            }
                        }
                        tooling.myAnt = releaseInformation[branch].ant;
                        apidocurl = releaseInformation[branch].apidocurl
                        mavenVersion=releaseInformation[branch].mavenversion

                        switch (releaseInformation[branch].releasedate['month']) {
                        case '01':month  = 'Jan'; break;
                        case '02':month  = 'Feb'; break;
                        case '03':month  = 'Mar'; break;
                        case '04':month  = 'Apr'; break;
                        case '05':month  = 'May'; break;
                        case '06':month  = 'Jun'; break;
                        case '07':month  = 'Jul'; break;
                        case '08':month  = 'Aug'; break;
                        case '09':month  = 'Sep'; break;
                        case '10':month  = 'Oct'; break;
                        case '11':month  = 'Nov'; break;
                        case '12':month  = 'Dec'; break;
                        default: month ='Invalid';
                        }
                        date  = releaseInformation[branch].releasedate['day'] + ' '+ month + ' '+releaseInformation[branch].releasedate['year']
                        //2018-07-29T12:00:00Z
                        atomdate = releaseInformation[branch].releasedate['year']+'-'+releaseInformation[branch].releasedate['month']+'-'+releaseInformation[branch].releasedate['day']+'T12:00:00Z'
                        tooling.jdktool = releaseInformation[branch].jdk
                        if (releaseInformation[branch].jdktoolapidoc) {
                            tooling.jdktoolapidoc = releaseInformation[branch].jdktoolapidoc
                        }else {
                            tooling.jdktoolapidoc = releaseInformation[branch].jdk
                        }
                        tooling.myMaven = releaseInformation[branch].maven
                        version = releaseInformation[branch].versionName;
                        vsixversion = releaseInformation[branch].vsixVersion;
                        // make a new attribute in json for this.
                        heavyrelease = releaseInformation[branch].publish_apidoc == 'true';
                        rmversion = version
                        //
                        if (releaseInformation[branch].milestones) {
                            releaseInformation[branch].milestones.each{key,value ->
                                if (key==githash) {
                                    // vote candidate prior
                                    if (value['vote']) {
                                        votecandidate = true
                                        vote = value['vote']
                                        rmversion = rmversion
                                    } else if (value['version']){
                                        // other named version
                                        rmversion = rmversion+'-'+value['version']
                                    }
                                }
                            }
                        }
                        debversion = rmversion.replace('-','~')
                    }
                }
            }
            stage ('Master build') {
                tools {
                    jdk tooling.jdktool
                }
                when {
                    branch 'master'
                }
                stages {
                    stage ('build javadoc') {
                        steps {
                            withAnt(installation: tooling.myAnt) {
                                sh "ant getallmavencoordinates"
                                sh "ant build-nbms"
                                sh "ant build-source-zips"
                            }
                            withAnt(installation: tooling.myAnt, jdk: tooling.jdktoolapidoc) {

                                sh "ant build-javadoc -Djavadoc.web.zip=${env.WORKSPACE}/WEBZIP.zip"

                                junit 'nbbuild/build/javadoc/checklinks-errors.xml'

                                publishToNightlies("/netbeans/apidocs/${env.BRANCH_NAME}","**/WEBZIP.zip")
                            }
                        }
                    }
                    stage (' Populate Snapshots') {
                        steps {
                            withAnt(installation: tooling.myAnt) {
                                script {
                                    def localRepo = "${env.WORKSPACE}/.repository"
                                    def netbeansbase = "nbbuild"
                                    def commonparam = "-Dexternallist=${netbeansbase}/build/external.info"

                                    sh "rm -rf ${env.WORKSPACE}/.repository"
                                    withMaven(maven:tooling.myMaven,jdk:tooling.jdktool,publisherStrategy: 'EXPLICIT',mavenLocalRepo: localRepo)
                                    {
                                        sh "mvn org.apache.maven.plugins:maven-dependency-plugin:3.5.0:get -Dartifact=org.apache.netbeans.utilities:nb-repository-plugin:${repopluginversion} -DremoteRepositories=apache.snapshots.https::::https://repository.apache.org/snapshots"

                                        //sh "mvn org.apache.netbeans.utilities:nb-repository-plugin:1.5:download -DnexusIndexDirectory=${env.WORKSPACE}/repoindex -DrepositoryUrl=https://repo.maven.apache.org/maven2"
                                        sh "mvn org.apache.netbeans.utilities:nb-repository-plugin:${repopluginversion}:populate -X ${commonparam} -DnetbeansNbmDirectory=${netbeansbase}/nbms -DnetbeansInstallDirectory=${netbeansbase}/netbeans -DnetbeansSourcesDirectory=${netbeansbase}/build/source-zips -DnetbeansJavadocDirectory=${netbeansbase}/build/javadoc -DparentGAV=org.apache.netbeans:netbeans-parent:4 -DforcedVersion=${mavenVersion} -DskipInstall=true -DdeployId=apache.snapshots.https -DdeployUrl=https://repository.apache.org/content/repositories/snapshots"
                                    }
                                }
                            }
                        }
                    }
                }
            }            
            stage ('Release preparation') {
                tools {
                    jdk tooling.jdktool
                }
                when {
                    allOf {
                        //expression { BRANCH_NAME ==~ /release[0-9]+/  || BRANCH_NAME ==~ /vsnetbeans_preview_[0-9]+/ }
                        branch pattern : "release\\d+|vsnetbeans_preview_\\d+",comparator:"REGEXP"
                        //wait for modern 1.4.1
                        expression { month =='Invalid' }
                    }
                }
                steps {
                    script {
                        def clusterconfigs = [/*['platform','netbeans-platform'],*/['release','netbeans']]
                        doParallelClusters(clusterconfigs);
                    }
                }
            }
            stage ('Release branch javadoc rebuild to nightlies') {
                tools {
                    jdk tooling.jdktool
                }
                when {
                    allOf {
                        //expression { BRANCH_NAME ==~ /release[0-9]+/ }
                        branch pattern : "release\\d+",comparator:"REGEXP"
                        //wait for modern 1.4.1
                    }

                }
                stages {
                    stage ('Archive Javadoc') {
                        steps {
                            withAnt(installation: tooling.myAnt) {
                                sh "ant"
                            }
                            // use jdk version aligned to max supported
                            withAnt(installation: tooling.myAnt, jdk: tooling.jdktoolapidoc) {
                                sh "ant build-javadoc -Djavadoc.web.zip=${env.WORKSPACE}/WEBZIP.zip"
                            }
                            junit 'nbbuild/build/javadoc/checklinks-errors.xml'
                            publishToNightlies("/netbeans/apidocs/${env.BRANCH_NAME}","**/WEBZIP.zip")
                        }
                    }
                }

            }
        }


        post {
            cleanup {
                cleanWs()
            }
            success {
                slackSend (channel:'#netbeans-builds', message:"SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL}) ",color:'good')
            }
            unstable {
                slackSend (channel:'#netbeans-builds', message:"UNSTABLE: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL}) ",color:'warning')
            }
            failure {
                slackSend (channel:'#netbeans-builds', message:"FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'  (${env.BUILD_URL})",color:'danger')
            }

        }
    }
}

def publishToNightlies(remotedirectory , source, prefix="") {
    // test if sshPublisher is known
    //if (this.getBinding().hasVariable('sshPublisher')) {
        sshPublisher(publishers: [
                sshPublisherDesc(configName: 'Nightlies', transfers: [
                        sshTransfer(cleanRemote: true,
                            excludes: '',
                            execCommand: '',
                            execTimeout: 0,
                            flatten: false,
                            makeEmptyDirs: false,
                            noDefaultExcludes: false,
                            patternSeparator: '[, ]+',
                            remoteDirectory: remotedirectory,
                            remoteDirectorySDF: false,
                            removePrefix: prefix,
                            sourceFiles: source)],
                    usePromotionTimestamp: false,
                    useWorkspaceInPromotion: false,
                    verbose: false)])
    //} else {
    //     println "NO SSH PUBLISHER TO PUSH TO NIGHTLIES"
    //}
}
// in fact not parallel otherwise workspace not cleaned
def doParallelClusters(cconfigs) {
    for (cluster in cconfigs) {
        def clustername = cluster[0]
        def path = cluster[1]
        // prepare versionned path
        def versionnedpath = "/${path}/${versionpath}"
        stage("prepare ${clustername}") {
            sh "rm -rf nbbuild/build"
            withAnt(installation: tooling.myAnt) {
                sh "ant build-source-config -Dcluster.config=${clustername} -Dbuildnum=666 -Dmetabuild.branch=${branch}"
                script {
                    def targets = ['verify-libs-and-licenses','rat','build']
                    for (String target in targets) {
                        stage("${target} for ${clustername}") {

                            def localRepo = ".repository"
                            // prepare a clean subfolder target - clustername prefixed
                            sh "rm -rf ${target}-${clustername}-temp && mkdir ${target}-${clustername}-temp && unzip -q nbbuild/build/${clustername}*.zip -d ${target}-${clustername}-temp && cp .gitignore ${env.WORKSPACE}/${target}-${clustername}-temp"
                            def add = "";

                            //
                            if (target=="build" && env.BRANCH_NAME!="release90") {
                                add=" -Ddo.build.windows.launchers=true"
                            }

                            // build the target on the cluster defined common to all
                            sh "ant -f ${target}-${clustername}-temp/build.xml ${target} -Dcluster.config=${clustername} ${add} -Dmetabuild.branch=${branch}"

                            // for verify-libs-and-licenses we only want the reports
                            if (target=='verify-libs-and-licenses') {
                                junit "verify-libs-and-licenses-${clustername}-temp/nbbuild/build/verifylibsandlicenses.xml"
                            }

                            // for rat we only want the reports (junit fail at the moment empty test)
                            if (target=='rat') {
                                // save report and test for rat and verify..
                                archiveArtifacts "rat-${clustername}-temp/nbbuild/build/rat-report.txt"
                                junit testResults: "rat-${clustername}-temp/nbbuild/build/rat/*.xml" , allowEmptyResults:true
                            }

                            // build target is more complex,
                            if (target=='build') {
                                
                               

                                sh "mkdir -p dist${versionnedpath}"
                                // source
                                sh "cp nbbuild/build/*${clustername}*.zip dist${versionnedpath}${path}-${rmversion}-source.zip"
                                // binaries
                                sh "cp build-${clustername}-temp/nbbuild/*${clustername}*.zip dist${versionnedpath}${path}-${rmversion}-bin.zip"

                                // special case for release prepare bits, maven, javadoc installer
                                if (clustername == "release") {

                                    // installer we prepare a folder so that release manager can build mac os on his own
                                    sh "mkdir -p dist${versionnedpath}nbms"
                                    sh "mkdir -p dist/installers"
                                    sh "mkdir -p dist/vsix"
                                    if (params.INSTALLERS) { // skip installers unless requested
                                        println "BUILDING INSTALLERS"
                                        def binaryfile = "../../../dist${versionnedpath}${path}-${rmversion}-bin.zip"
                                        def timestamp = sh(returnStdout: true, script: 'date +%y%m%d').trim()

                                        // we archive put to nightlies only exe for window, nbpackage is intended to do the installler
                                        // XXX take too long 18012023 publishToNightlies("/netbeans/candidate/installerspreparation","distpreparation/**/**","distpreparation")

                                        
                                        sh "mkdir -p nbpackage${versionnedpath}installer"
                                        withMaven(maven:tooling.myMaven,jdk:tooling.jdktool,publisherStrategy: 'EXPLICIT',mavenLocalRepo: localRepo,options:[artifactsPublisher(disabled: true)])
                                        {
                                            // unpack nbpackage snapshot can later
                                            sh "mvn org.apache.maven.plugins:maven-dependency-plugin:3.5.0:get  -Dartifact=org.apache.netbeans:nbpackage:${nbpackageversion}:zip:bin -Dmaven.repo.local=${env.WORKSPACE}/.repository -DremoteRepositories=apache.snapshots.https::::https://repository.apache.org/snapshots"
                                            sh "mvn org.apache.maven.plugins:maven-dependency-plugin:3.5.0:unpack -DoutputDirectory=nbpackage${versionnedpath}installer -Dartifact=org.apache.netbeans:nbpackage:${nbpackageversion}:zip:bin -Dmaven.repo.local=${env.WORKSPACE}/.repository -DremoteRepositories=apache.snapshots.https::::https://repository.apache.org/snapshots"

                                            // build installer only deb for testing.
                                            sh "cd nbpackage${versionnedpath}installer/ && nbpackage-${nbpackageversion}/bin/nbpackage -v --type linux-deb -Pname=\"Apache NetBeans\" -Pversion=${debversion} -Purl=\"https://netbeans.apache.org\"  -Pdeb.maintainer=\"NetBeans Mailing List <users@netbeans.apache.org>\"  -Pdeb.desktop-filename=\"apache-netbeans-ide-${rmversion}\"  -Pdeb.wmclass=\"Apache NetBeans IDE ${rmversion}\"  --input ../../../dist${versionnedpath}${path}-${rmversion}-bin.zip "
                                            // debug output
                                            sh "cp nbpackage${versionnedpath}installer/*.deb dist/installers/ "
                                            sh "cd nbpackage${versionnedpath}installer/ && nbpackage-${nbpackageversion}/bin/nbpackage -v --type linux-rpm -Pname=\"Apache NetBeans\" -Pversion=${debversion} -Purl=\"https://netbeans.apache.org\"  -Prpm.desktop-filename=\"apache-netbeans-ide-${rmversion}\"  -Prpm.wmclass=\"Apache NetBeans IDE ${rmversion}\"  --input ../../../dist${versionnedpath}${path}-${rmversion}-bin.zip "
                                            sh "cp nbpackage${versionnedpath}installer/*.rpm dist/installers/ "
                                            // archiveArtifacts "nbpackage${versionnedpath}installer/**"
                                        }

                                    } else {
                                        println "SKIPPING INSTALLER BUILDS"
                                    }


                                    // the installer phase is ok we should have installer for linux / windows + scripts and a bit of source to build macos later


                                    // additionnal target to have maven ready
                                    // javadoc build for maven artefacts, do the more we can using jdk of build.
                                    sh "ant -f build-${clustername}-temp/build.xml build-nbms build-source-zips generate-uc-catalog -Dcluster.config=release -Ddo.build.windows.launchers=true -Dmetabuild.branch=${branch}"
                                    // try to build javadoc using jdkapidoc
                                    withAnt(installation: tooling.myAnt, jdk: tooling.jdktoolapidoc) {
                                    sh "ant -f build-${clustername}-temp/build.xml build-javadoc -Djavadoc.web.root='${apidocurl}' -Dmodules-javadoc-date='${date}' -Datom-date='${atomdate}' -Dmetabuild.branch=${branch}"
                                    }
                                    sh "cp -r build-${clustername}-temp/nbbuild/nbms/** dist${versionnedpath}nbms/"
                                    
                                    def netbeansbase = "build-${clustername}-temp/nbbuild"
                                    sh "ant -f build-${clustername}-temp/build.xml getallmavencoordinates -Dmetabuild.branch=${branch}"
                                    withMaven(maven:tooling.myMaven,jdk:tooling.jdktool,publisherStrategy: 'EXPLICIT',mavenLocalRepo: localRepo,options:[artifactsPublisher(disabled: true)])
                                    {
                                        sh "mvn org.apache.maven.plugins:maven-dependency-plugin:3.5.0:get -Dartifact=org.apache.netbeans.utilities:nb-repository-plugin:${repopluginversion} -Dmaven.repo.local=${env.WORKSPACE}/.repository -DremoteRepositories=apache.snapshots.https::::https://repository.apache.org/snapshots"
                                        def commonparam = "-Dexternallist=${netbeansbase}/build/external.info"
                                        //sh "mvn org.apache.netbeans.utilities:nb-repository-plugin:1.5:download ${commonparam} -DrepositoryUrl=https://repo.maven.apache.org/maven2"
                                        if (heavyrelease) { // skip mavenrepo for vscode
                                            sh "mvn org.apache.netbeans.utilities:nb-repository-plugin:${repopluginversion}:populate ${commonparam} -DnetbeansNbmDirectory=${netbeansbase}/nbms -DnetbeansInstallDirectory=${netbeansbase}/netbeans -DnetbeansSourcesDirectory=${netbeansbase}/build/source-zips -DnetbeansJavadocDirectory=${netbeansbase}/build/javadoc -DparentGAV=org.apache.netbeans:netbeans-parent:4 -DforcedVersion=${mavenVersion} -DskipInstall=true -DdeployUrl=file://${env.WORKSPACE}/mavenrepository"
                                            zip zipFile:'mavenrepo.zip',dir:'mavenrepository',archive:'true'
                                        }
                                        if (params.VSIX) {
                                            // make vsix available to dist to pickup (only for main release) need a maven setup
                                            println "BUILDING VSCODE PLUGIN"
                                            sh "ant -f build-${clustername}-temp/java/java.lsp.server build-vscode-ext -Dvsix.version=${vsixversion} -Dmetabuild.branch=${branch}"
                                            sh "cp -r build-${clustername}-temp/java/java.lsp.server/build/*.vsix dist/vsix/"
                                        } else {
                                            println "SKIPPING VSCODE PLUGIN"
                                        }
                                    }
                                }

                                // do checksum
                                def extensions = ['*.zip','*.nbm','*.gz','*.jar','*.xml','*.license']
                                for (String extension in extensions) {
                                    sh "cd dist"+' && for z in $(find . -name "'+"${extension}"+'") ; do cd $(dirname $z) ; sha512sum ./$(basename $z) > $(basename $z).sha512; cd - >/dev/null; done '
                                }
                                archiveArtifacts 'dist/**'                             
                            }
                        }
                    }
                }
            }
        }
        stage("publish to nightlies ${versionnedpath}") {
            if (params.NIGHTLIES) {
                println "PUBLISHING TO NIGHTLIES"
                publishToNightlies("/netbeans/candidate/${versionnedpath}","dist${versionnedpath}/*","dist${versionnedpath}")
                publishToNightlies("/netbeans/candidate/installers","dist/installers/*","dist/installers/")
                publishToNightlies("/netbeans/candidate/vsix","build-${clustername}-temp/java/java.lsp.server/build/*.vsix","build-${clustername}-temp/java/java.lsp.server/build")
            } else {
                println "SKIPPING PUBLISH TO NIGHTLIES"
            }
        }
    }
}
