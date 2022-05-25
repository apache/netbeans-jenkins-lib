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
organizationFolder('NetBeans/netbeans-TLP') {
    description('Apache NetBeans release jobs and apidoc generation for historical version')
    displayName('NetBeans-TLP')
    
    organizations {
        configure {
            def asf  = it / navigators / 'org.apache.jenkins.gitpubsub.ASFGitSCMNavigator'
            asf << {
                server ('https://gitbox.apache.org/repos/asf')
            }
            def traits = asf / traits
            traits << 'org.apache.jenkins.gitpubsub.ASFMetadataSCMNavigatorTrait' {
                avatarUrl('https://netbeans.apache.org/images/nblogo32x32.png')
            }
            traits << 'jenkins.scm.impl.trait.WildcardSCMSourceFilterTrait' {
                includes('netbeans')
            }
            traits << 'jenkins.plugins.git.traits.BranchDiscoveryTrait' {}
            traits << 'jenkins.scm.impl.trait.RegexSCMHeadFilterTrait' {
                regex('(master|release\\d+$)')
            }
        }
       
        buildStrategies {
            buildNamedBranches {
                filters {
                    regex {
                        regex('(master|release\\d+$|vsnetbeans_preview_\\d+$)')
                        caseSensitive(false)
                    }
                }
            }
        }
        projectFactories {
            workflowMultiBranchProjectFactory {
                // Relative location within the checkout of our Pipeline script.
                scriptPath("nbbuild/jenkins/Jenkinsfile.groovy")
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
}
listView('NetBeans/netbeans') {
    jobs {
        name('NetBeans/netbeans-TLP')
    }
}
