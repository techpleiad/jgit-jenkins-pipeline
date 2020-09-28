### Jenkins Pipeline for Automatic code versioning
##### (Code that uses [maven](https://maven.apache.org/) as build tool)

## Introduction
This pipeline is abstraction over [gitflow-workflow](https://nvie.com/posts/a-successful-git-branching-model). Basically gitflow is  git branching and release management workflow that helps developers keep track of features, hotfixes and releases in software projects.

## Why Gitflow!
This [blog](https://jeffkreeftmeijer.com/git-flow/) answers everything!
*I like the title of the blog a lot 'why-arent-you-using-git-flow'.

## Implementation of gitflow
This pipeline is backed by with maven plugin [jgit-flow](https://bitbucket.org/atlassian/jgit-flow) which is basically java implementation of gitflow-workflow by atlassian. 
This plugin abstract out major complexity of gitflow implementation and you can do [semantic versioning](https://semver.org/) by few commands of this plugin e.g. if your current maven code version is 1.0.0-SNAPSHOT *(offcourse that means you are in development).*
Following command will create a release branch that is generally release branch is used for testing and bug fixing purposes and once the code is released, it will also increment the version of the codebase in development. So generally by semantic version once you release the code (taking above development version), your release version would be 1.0.0 or 1.0.0-RC and develop version would be 1.1.0 or 1.0.1 unless.

`mvn jgitflow:release-start -DallowSnapshots -DpushReleases=true -DreleaseVersion=${nextReleaseVersion} -DdevelopmentVersion=${nextDevelopmentVersion}"`

Similarly there are other goals in jgit-flow that helps in implementing complete gitflow-workflow via maven plugin. See [this](https://bitbucket.org/atlassian/jgit-flow/wiki/goals.wiki#!goals-overview) for more details on other goals.

## What this pipeline do
Imagine a scenario where you have microservices architecture and to maintain your code base you have different git repositories for your different microservices.
Now as per basic microservices principle each microservice can have its independent release cycle that essentially means every microservice can have its own code version. 
Now releasing and maintaining that code version manually via release plugin or even with jgit plugin via command line could be very tedious specially if you have many microservices in your system.

In this scenario this pipeline becomes a handy and fast solution. It will help you to release your code base for testing *i.e. in gitflow workflow terms start-release* and in another click once testing and bug fixing and done this pipeline will help you to merge the code in master and create a tag *in gitflow terms it means end-release*

And the best part is **single click would orchestate this for all the microservices (git repositories)**. We just have to be follow some conventions and we deciplined in how we commit code and bring it to dev.
Rest everything will be taken care by this pipeline.

## How this pipeline works

##### Step 1: Add jgit maven plugin as code 
At the time of this pipeline release [1.0-m5.1 version](https://mvnrepository.com/artifact/external.atlassian.jgitflow/jgitflow-maven-plugin) of jgitflow-maven-plugin was latest.
```
 <build>
        <plugins>
            <plugin>
                <groupId>external.atlassian.jgitflow</groupId>
                <artifactId>jgitflow-maven-plugin</artifactId>
                <version>1.0-m5.1</version>
                <configuration>
                    <noDeploy>true</noDeploy>
                    <squash>false</squash>
                    <allowSnapshots>true</allowSnapshots>
                    <autoVersionSubmodules>true</autoVersionSubmodules>
                    <flowInitContext>
                        <masterBranchName>master</masterBranchName>
                        <developBranchName>dev</developBranchName>
                        <releaseBranchPrefix>release-</releaseBranchPrefix>
                        <versionTagPrefix>dt-hgw-</versionTagPrefix>
                    </flowInitContext>
                </configuration>
            </plugin>

           ...
        </plugins>
```
##### Step 2: Add jgit.version file at the root of project
This file will derive the logic of calculating next development version and release version of the codebase when release-start command is executed.

##### Step 3: Configure jenkins pipeline in jenkins using this repository

##### Step 3: Execute that job


1. Pipeline takes input all the code repositions links.
2. This pipeline offers two modes right now -
    - start-release
    - end-release
    
##### Step 3a: Start-Release
- Start-Release will execute following steps for all the code repositories
    - If will compare the code of configured 'dev' and 'master' branch and *smartly figure out* if there code changes to release or not
    - If there are code changes, pipeline with execute jgitflow:release-start command to create the release branch from develop branch with proper code versioning of dev and release.
    - If no code changes are found then nothing happens for that repository. 

##### Visual workflow for start-release mode
![start-release pipeline flow](http://www.plantuml.com/plantuml/png/VP51Qzj058Jl-ok6z98O6cXxCoM4jXIQKsXxB9YLD99V-lHMxAxizDzNtiYYZd4F0h7pzfiti_Sf-vZ7NfslSuYu-iBT5Nn2mvqR9abg6RnbN3tGVekb8ZwLVxnHwRQvYwGvDrt0sxkZjCOtaonUyH0gN7yQHHli9M_nwAFlcHdJZNFEQvucXp6Z3H6335GnkfcoyDRuaJdWtNeUzgh8MsArgaD3H0uXIGvHc5OhN1_bNkeO4kDEQYQSZTvSyda_-qpyAHOr31ErYjxgBSptXBTcqdgVCkEfT7k9-M6Ddr6wZh6ijaIbDSvf20CUvoFmcVMO9LWfvHX6oaPd8Hm0d7ikiDJxcFKFMh4c5L11DDAsZBIQg9Ztf9s4FGrOFBncI8lFhsvBGXj6QyhC_rUflTQZ-ioNFOpF6RFbiS8TOv9W1_zV5gdqYSiXsXZwVxfes8gnWHYUEictnxbMbBUNz_a-OvSknYJM5GG67JinLovJ4s7XTLyE5ycRpV8y-6Y-vnqamK86LJgfbEL5vroR3VQYYehGq7LygP3MkCEtq0yLhV5LJD8MJw5ozvCyzleN)

##### Step 3b: End-Release
- End-Release will execute following steps for all the code repositories
    - If will figure out for what all repositories release branch exists by the configured release branch pattern.
    - If release branch is found then end release command is triggered to finish the release branch and merge the code in master and create tag for the same. 
    - If no release branch is found then that repository will be skipped.

##### Visual workflow for end-release mode
![end-release pipeline flow](http://www.plantuml.com/plantuml/png/TP11Qzmm58Jl-XN3zf8ijD0UJGcXb49p2ctlGRR6zjlAJqAItUJVrnQxBkqs1yF3Ctmpe_jSR2hpv8tjIaWuVEPoWCrmN4nS9UaaMYquF51_YkOgdkgDDrw6iGo9L6CS5xozkqWDlgeKiHuFeSNDfrcymoyf8nvjiXCBKxurnbXVLcUS2_gG42NIYzASAJklmVV2YFVDsMozHnc9ux4BZaYC8Ki9IPYFHznSv4cw53BJ8Xqphgoth-LyQF0ZoJ0mLSnUY-gCyI54F9v_WS_ivY91wu1BiDIMLu460AupTrWlwZAahwoDCyX2_OVpWaHFcuasMUr6y5booGW96igxcb6VjSUtlurzcDLL_NyrThTaK1qTsfTQuZH8GVJp85kLQR8hf5BeCw41jP-JlcUYTZHuDUBfQT-2Zsedta7oLXlqCaZhkKtV1r9TtRCTPll-41sN1bz49OyuXTRSh_8y-Ty0)

## Configure Pipeline
Follow the steps on this wiki page to configure the pipeline https://github.com/dhruv-bansal/jgit-jenkins-pipeline/wiki/Configuring-pipeline



