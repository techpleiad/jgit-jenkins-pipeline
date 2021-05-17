### Jenkins Pipeline for Automatic code versioning

##### (Code that uses [maven](https://maven.apache.org/) as build tool)

## Introduction

This pipeline is abstraction over [gitflow-workflow](https://nvie.com/posts/a-successful-git-branching-model). Basically gitflow is git branching and release management workflow that helps developers keep track of features, hotfixes and releases in software projects.

## Why Gitflow!

This [blog](https://jeffkreeftmeijer.com/git-flow/) answers everything!
\*I like the title of the blog a lot 'why-arent-you-using-git-flow'.

## Implementation of gitflow

This pipeline is backed by with maven plugin [jgit-flow](https://bitbucket.org/atlassian/jgit-flow) which is basically java implementation of gitflow-workflow by atlassian.
This plugin abstract out major complexity of gitflow implementation and you can do [semantic versioning](https://semver.org/) by few commands of this plugin e.g. if your current maven code version is 1.0.0-SNAPSHOT _(offcourse that means you are in development)._
Following command will create a release branch that is generally release branch is used for testing and bug fixing purposes and once the code is released, it will also increment the version of the codebase in development. So generally by semantic version once you release the code (taking above development version), your release version would be 1.0.0 or 1.0.0-RC and develop version would be 1.1.0 or 1.0.1 unless.

`mvn jgitflow:release-start -DallowSnapshots -DpushReleases=true -DreleaseVersion=${nextReleaseVersion} -DdevelopmentVersion=${nextDevelopmentVersion}"`

Similarly there are other goals in jgit-flow that helps in implementing complete gitflow-workflow via maven plugin. See [this](https://bitbucket.org/atlassian/jgit-flow/wiki/goals.wiki#!goals-overview) for more details on other goals.

## What this pipeline do

Imagine a scenario where you have microservices architecture and to maintain your code base you have different git repositories for your different microservices.
Now as per basic microservices principle each microservice can have its independent release cycle that essentially means every microservice can have its own code version.
Now releasing and maintaining that code version manually via release plugin or even with jgit plugin via command line could be very tedious specially if you have many microservices in your system.

In this scenario this pipeline becomes a handy and fast solution. It will help you to release your code base for testing _i.e. in gitflow workflow terms start-release_ and in another click once testing and bug fixing and done this pipeline will help you to merge the code in master and create a tag _in gitflow terms it means end-release_

And the best part is **single click would orchestate this for all the microservices (git repositories)**. We just have to be follow some conventions and be deciplined in how we commit code and bring it to dev.
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
  - If will compare the code of configured 'dev' and 'master' branch and _smartly figure out_ if there code changes to release or not
  - If there are code changes, pipeline with execute jgitflow:release-start command to create the release branch from develop branch with proper code versioning of dev and release.
  - If no code changes are found then nothing happens for that repository.

##### Visual workflow for start-release mode

![start-release pipeline flow](http://www.plantuml.com/plantuml/png/VP51ZzCm58Jl_XMZSjcjH2lWLg7L2X0IUre1HwJAYIVfAyvpP3ljykz9JHgXbdA85FcDlpSpVapPnZnqtjWwXuZ59nuMi0cE1zEF8wcbjvgnUAB-54rbk2__UEMbtUKsachCU06l7ywZ3RwePB4UJw72tOT1lCDHyWxFDjgEcJ7T6sEipwEo7pAQ4D5BJozACr5ctWRVCdkysKnYwptoZbWkw-28o3uaoI4AqtA9k_Euapg6n7YGcWbNqljRUzvEUqR-UZq6emzzHythzKYlejLw1xKTOTKLJMTJPhmShsxntstmFKhRCXQRSsogCyQ7qEDvEW9VM0zPWfQ2pi6akBiAP03WUkSCIvsDsVz28-fI0HM4awPXfDP4nNmazP_1hyEMVUK96toy84b24mPrHVD_MxNrzU1jvXzFofUCQVcvpWDZag0d3pyqqjCcpeTeOkZ-wiMn4QM3AFQjvDLPruZdwhQVhnDsxcL8ecr1eFT3AshAPVf4QBZipk0cURkTdmhlrNPyW2HeoA2NLYhFyhfJhkqMH_4U5U53sz8LCjMPqpVJJnLjy5bKqWxxK9d7SJnq_ZS0)

##### Step 3b: End-Release

- End-Release will execute following steps for all the code repositories
  - If will figure out for what all repositories release branch exists by the configured release branch pattern.
  - If release branch is found then end release command is triggered to finish the release branch and merge the code in master and create tag for the same.
  - If no release branch is found then that repository will be skipped.

##### Visual workflow for end-release mode

![end-release pipeline flow](http://www.plantuml.com/plantuml/png/TP11RzGm48Nl_XLFowLT2IGuHgYg44AYfmguLquySMPnRCip2VJVuxKsGNJfRPGt-TutlXUrHPSv63jeAZXyflK0At2qcbmZ7ZZRg3WyKFp9KLfuQZFU1FQJ-a8KZQa_uEtT5dNu6bdP1Zoa7ZUV5WuElrWdFDfYPr8gSckCiK7hPbuKOstFd2bm97EXxBmEtvKotdUNPHi2T28SZqDoX48v2LT_9ZaUSRF5Cmyb2PML1n9ShTw-rlEXmu_2tbDfc5sBeZCcf9JnUFa4-at3efnYEs0hvcPvLMW0uBfpXuqSdO37nZfpFAyKNt2UwnZ82g4lDWvJLM1HGIg8IT-rZlQi8xxzkpYc9RgM_rTZssO1HKSE_NEJE7bMvB1uFglCiwsGHg4WL3jV-1Yysl3qj9-2Zz7ET0UMipKeiES-KBjyurMBTivvQ6__b-XexV258yk4K-hDVOsNEVm1)

##### Step 3c: Update-Release

- Update-Release will execute following steps for all the code repositories
  - It will figure out for what all repositories release branch exists by the configured release branch pattern.
  - If will compare the code of configured 'dev' and 'release' branch and _smartly figure out_ if there code changes to release or not
  - If there are code changes, pipeline will create a copy of dev branch 'dev_branch' and version it according to the release branch
  - It will then merge this copy into release branch and eradicate the copy of dev branch to create a release branch with proper versioning
  - If no code changes are found then nothing happens for that repository.

##### Visual workflow for update-release mode

![update-release pipeline flow](http://www.plantuml.com/plantuml/png/jPD1Qzmm48Nl-XMFz98ij4jxN4e8RIWqK0XjxulMVlPEgfMCD7QQVr-ji1rgK_RKWy7M6xttpKY-IsgZzYTdRAKXukgUmnLiGit1T5cIIZhh5LSFzB_4fo8-bNUyST8UjOsaDoPlmDlRIThXgnSLw_0G3hZ-r8khyInwnAEDzaHbJ3V66EiqLtQzeWaHdNHqucb6bzblXn_A3kztOx5r3deaDfigr4Ha5v9aVc7QR70zoIUfOaYCWrHCM9NU_2ldmmu_exGjOx6PiUXhOrm87Hx79V0tgrub-DB052o5SXLe062TEPiruki4QOhNsQULWNxXyu98Hvk8GxI-EcO4IPeG8dpGToMZZBN1zxyBcz3xkkWNEDB9HdKl8yyPf23KqZICz5Lsf3wJFX_0Tbjps6xpsAslo_U8TruRmsQWpmiRIGke2s3FuXogIAk4pL3T2qA3332sEF7ipz8CDJ0c2HwYi5KLOYs-XOR572ymV6DikVONhs7PzGHJxdiNGySuCUJa2pdtTDHra9ctqIN2X-MHNB3Tx-TRX8_UddWBIV11GIUj71pBfHkxsE_Bg74WN6XRlXvG-GLUCLP3_Ay49VtylBhN77Ru8bxI4K_XOEwot9_S7m00)

## Configure Pipeline

Follow the steps on this wiki page to configure the pipeline https://github.com/dhruv-bansal/jgit-jenkins-pipeline/wiki/Configuring-pipeline
