pipeline {
    options {
        buildDiscarder(logRotator(numToKeepStr: '2'))
        disableConcurrentBuilds()
    }
    agent { node { label 'ubuntu' } }

    stages {

        stage("clone and prepare build") {
            tools {
                jdk 'jdk_1.8_latest'
                ant 'ant_latest'
            }
            steps {


                git 'https://github.com/apache/netbeans'
                sh 'rm -f *.json* '
                sh 'rm -f nbbuild/NetBeans-dev-Netbeans/*.zip'
                sh 'wget -Onetbeansrelease.json "https://gitbox.apache.org/repos/asf?p=netbeans-jenkins-lib.git;a=blob_plain;f=meta/netbeansrelease.json" '
                sh "ant build -Dmetabuild.jsonurl=file:netbeansrelease.json -Dzip.dir=${WORKSPACE}/zip"
                sh "cd ${WORKSPACE}/zip/ && cd * && mv *.zip ${WORKSPACE}/zip/NetBeansIDE.zip "
                stash includes: "zip/NetBeansIDE.zip", name: 'idebuildzip'
                sh 'ant build-test-dist -Dmetabuild.jsonurl=file:netbeansrelease.json'
                stash includes: 'nbbuild/build/testdist.zip', name: 'testbuildzip'

            }
        }
        stage ("EffectiveMatrix")
        {
            matrix {
                agent { node { label 'ubuntu' } }

                axes {
                    axis {
                        name 'JDK'
                        values 'jdk_1.8_latest', 'jdk_11_latest', 'jdk_17_latest'
                    }
                    axis {
                        name 'MODULE'
                        values 'platform', 'ide'
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
                                unzip  zipFile: 'nbbuild/build/testdist.zip', dir:'testdir'
                                unzip  zipFile: 'zip/NetBeansIDE.zip', dir:'netbeans'
                                mkdir "-p ${WORKSPACE}/result/unit/${JDK}/${MODULE} "
                                sh 'java -version'
                                sh 'ant -version'
                                // this is not finished
                                wrap([$class: 'Xvfb', additionalOptions: '', assignedLabels: '', displayNameOffset: 0, autoDisplayName:true, installationName: 'Xvfb', parallelBuild: true, screen: '']) {
                                    sh "ant -f ${WORKSPACE}/testdir/build.xml -Dtest-sys-prop.ignore.random.failures=true -Dtest.result.dir=${WORKSPACE}/result/unit/${JDK}/${MODULE} -Dtest.clusters=${env.MODULE} -Dtest.types=unit -Dnetbeans.dest.dir=${WORKSPACE}/netbeans/netbeans "
                                }
                                junit "${WORKSPACE}/result/unit/${JDK}/${MODULE}/**/*.xml"
                                //sh "ant -f ${WORKSPACE}/testdir/build.xml -Dtest.clusters=${env.MODULE} -Dtest.types=qa-functional -Dnetbeans.dest.dir=${WORKSPACE}/netbeans/netbeans"
                                archiveArtifacts artifacts: "${WORKSPACE}/result/unit/${JDK}/${MODULE}/**"
                            }
                        }

                    }
                }

            }
        }
    }
    post {
        cleanup {
            cleanWs disableDeferredWipeout: true, deleteDirs: true
        }
    }
}
