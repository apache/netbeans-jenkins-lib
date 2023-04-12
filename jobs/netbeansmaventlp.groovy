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
organizationFolder('NetBeans/netbeans-maven-TLP') {
    description('Apache NetBeans maven related jobs (nbm plugin ,archetype, infra, nbpackage)')
    displayName('netbeans-maven-TLP')
    organizations {
      github {
        repoOwner('apache')
        apiUri('https://api.github.com')
        credentialsId('ASF CI for Github PRs etc')
        traits {
          sourceWildcardFilter  {
            includes('netbeans-parent netbeans-webskin netbeans-mavenutils-* netbeans-nbpackage')
            excludes('')
          } 
          gitHubBranchDiscovery {
            strategyId(3)
          } 
          headRegexFilter {
           regex('master')
          } 
          cleanAfterCheckout {
            extension {
              deleteUntrackedNestedRepositories(true)
            }
          }
          cleanBeforeCheckout {
            extension {
              deleteUntrackedNestedRepositories(true)
            }
          }  
        }
      }
    }
    projectFactories {
        workflowMultiBranchProjectFactory {
            // Relative location within the checkout of our Pipeline script.
            scriptPath("Jenkinsfile.groovy")
        }
    }
    orphanedItemStrategy {
        discardOldItems {
            numToKeep(4)
            daysToKeep(5)
        }
    }
    triggers {
        periodicFolderTrigger {
            interval("1d")
        }
    }
}
listView('NetBeans/maven') {
    jobs {
        name('NetBeans/netbeans-maven-TLP')
    }
}
