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
// ant version
def antversion='ant_1.10_latest'

// Common job with all default
// jdk
// log rotation
// wrappers
// git clone from apache netbeans branch master
def netbeansBaseJob(Map m, Closure c = {}) {
    freeStyleJob("NetBeans/netbeans-${m.name}") {
        logRotator {
            numToKeep(2)
            daysToKeep(7)
        }
        jdk('jdk_17_latest')    
        triggers {
            scm('H/5 * * * *')
        }
        wrappers {
            if (m.xvfb) {
              xvfb('Xvfb') { }
            }    
            preBuildCleanup()
        }
        scm {
            git {
                remote {
                    url('https://github.com/apache/netbeans.git')
                }
                branch('*/master')
                extensions {
                    cleanBeforeCheckout()
                    cleanCheckout {
                        deleteUntrackedNestedRepositories(true)
                    } 
                }
            }
        }
        c.delegate = delegate 
        c()
    }
}

netbeansBaseJob(name:'linux',xvfb:true) {
    description("""job tasks:
 <ul>
  <li>Builds Apache NetBeans from <a href="https://github.com/apache/netbeans">its Github repository</a></li>
  <li>runs platform tests that aren't marked with <code>@RandomlyFails</code> annotation</li>
  <li>the artifacts are used as dev-build and linked from the README (and therefore github)</li>
</ul> 

<p>
  tests:
<pre>
\$ ant test-platform
</pre>
<p>
There is also a <a href="../netbeans-windows">windows version</a> of this job.
The <b>licenses</b> are checked by the <a href="../netbeans-license/lastCompletedBuild/testReport/ ">license job</a>.""")
    
    label('ubuntu')
    steps {
        ant {
            targets(['build','build-nbms', 'generate-uc-catalog', 'build-source-zips', 'index-layer-paths'])
            props('do.build.windows.launchers': 'true')
            antInstallation(antversion)
        }
    }
    publishers {
        archiveArtifacts('nbbuild/**/*.zip,nbbuild/nbms/**,nbbuild/build/generated/**')
        //archiveJunit('**/test/*/results/TEST*.xml')
    } 
}

netbeansBaseJob(name:'windows',xvfb:false) {
    description("""<html>Builds Apache NetBeans from <a href="https://github.com/apache/netbeans">its Github repository</a>
and runs platform tests (ant test-platform) that aren't marked with
<code>@RandomlyFails</code> annotation.
There is also a <a href="../netbeans-linux">Linux version</a> of this build.</html>""")        
    label('Windows')
    steps {
        ant {
            targets(['build','test-platform'])
            props('test-unit-sys-prop.ignore.random.failures': 'true','continue.after.failing.tests':'true')
            antInstallation(antversion+"_windows")
        }
    }
    publishers {
        archiveJunit('**/test/*/results/TEST*.xml')
    }  
}

netbeansBaseJob(name:'license',xvfb:true) {
    description("""Checks licenses of Apache NetBeans from <a href="https://github.com/apache/netbeans">its Github repository</a>:
<p>
<pre>
\$ ant rat verify-libs-and-licenses
</pre>
<p>
The real code check is done by a <a href="../netbeans-linux">linux job</a>.""")     
     
    label('ubuntu')
    steps {
        ant {
            targets(['rat','verify-libs-and-licenses'])
            antInstallation(antversion)
        }
    }
    publishers {
        archiveArtifacts('nbbuild/build/rat-report.txt')
        archiveJunit('nbbuild/build/rat/*.xml,nbbuild/build/verifylibsandlicenses.xml')
    } 
}

netbeansBaseJob(name:'apisigcheck',xvfb:true) {
    description("""Checks sig of Apache NetBeans from <a href="https://github.com/apache/netbeans">its Github repository</a>:
<p>
<pre>
\$ ant build gen-sigtests-release
</pre>
<p>
The real code check is done by a <a href="../netbeans-linux">linux job</a>.""")
    
    label('ubuntu')
    steps {
        ant {
            targets(['build','gen-sigtests-release'])
            props('sigtest.gen.fail.on.error':'false')
            antInstallation(antversion)
        }
    }
    publishers {
        archiveJunit('**/sigtest/results/*.xml')
    } 
}
