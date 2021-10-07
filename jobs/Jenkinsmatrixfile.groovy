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
                sh 'rm -f nbbuild/NetBeans-dev-NetBeans/*.zip'
                sh 'wget -Onetbeansrelease.json "https://gitbox.apache.org/repos/asf?p=netbeans-jenkins-lib.git;a=blob_plain;f=meta/netbeansrelease.json" '
                sh 'ant build -Dmetabuild.jsonurl=file:netbeansrelease.json'
                sh 'ant build-test-dist -Dmetabuild.jsonurl=file:netbeansrelease.json'
                stash includes: 'nbbuild/build/testdist.zip', name: 'testbuildzip'
                sh 'mv nbbuild/NetBeans-dev-Netbeans/netbeans*.zip nbbuild/NetBeansIDE.zip '
                stash includes: 'nbbuild/NetBeansIDE.zip', name: 'idebuildzip'
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
                                unzip  zipFile: 'nbbuild/NetBeansIDE.zip', dir:'netbeans'
                                sh 'java -version'
                                sh 'ant -version'
                                // this is not finished
                                sh "ant -f testdist/build.xml -Dtest.clusters=${env.MODULE} -Dtest.types=unit -Dnetbeans.dest.dir=${WORKSPACE}/netbeans"
                                sh "ant -f testdist/build.xml -Dtest.clusters=${env.MODULE} -Dtest.types=qa-functional -Dnetbeans.dest.dir=${WORKSPACE}/netbeans"
                                
                            }
                        }
                        
                    }
                }}
        } 
        
    }
    
}
