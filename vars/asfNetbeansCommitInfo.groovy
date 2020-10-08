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
 
def call(Map params = [:]) {
    pipeline {
        agent { node { label 'ubuntu' } }
        stages {
            stage("ICLA Checker") {
                when { changeRequest() }
                steps {
                    script {
                        // GitHub requires new lines in comments to be windows style
                        String NL = "\r\n";
                   
                        // Mark the comments we create
                        String MARKER_COMMENT = "<!-- Autocomment ICLA Checker Bot -->"
                        
                        //Message start
                        String message = MARKER_COMMENT + NL
                                        
                        message += "ICLA Checker" + NL
                     
                        def response = sh(script: "curl -H \"Accept: application/vnd.github.v3+json\" https://api.github.com/repos/apache/netbeans/pulls/${pullRequest.id}", returnStdout: true)
                        message += response + NL
                                    
                        message += "Generation date: ${sh(returnStdout: true, script: "date '+%Y-%m-%d %H:%M:%S'").trim()}" + NL
                        
                        // https://github.com/jenkinsci/pipeline-github-plugin#commit
                        
                        for (commit in pullRequest.commits) {
                            message += "${commit.author}" + " " + "${commit.committer}" + NL
                        }
                        
                        // Check the existing comments of the PR, which are marked to come from this job
                        def jobComments = []
                        for (comment in pullRequest.comments) {
                            if (comment.body.contains(MARKER_COMMENT)) {
                                jobComments << comment
                            }
                        }
                        
                        if(jobComments.size() == 0) {
                            // If there is not existing bot comment, add a new comment with the summary
                            pullRequest.comment(message)
                        } else {
                            // If there is already one bot comment, the first bot comment is updated and the further bot comments are removed
                            pullRequest.editComment(jobComments[0].id, message)
                            for(int i = 1; i < jobComments.size(); i++) {
                                pullRequest.deleteComment(jobComments[i].id)
                            }
                        }
                        
                    }
                }
            }
        }
    }
}
