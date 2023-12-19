<p align="center">
  <img src="assets/testkube-logo.svg" alt="Testkube Logo" width="80"/>
</p>

<p align="center">
  Welcome to Testkube - Your friendly cloud-native testing framework for Kubernetes
</p>

<p align="center">
  <a href="https://testkube.io">Website</a> |
  <a href="https://kubeshop.github.io/testkube">Documentation</a> |
  <a href="https://twitter.com/testkube_io">Twitter</a> |
  <a href="https://discord.gg/hfq44wtR6Q">Discord</a> |
  <a href="https://kubeshop.io/category/testkube">Blog</a>
</p>

<p align="center">
  <img title="MIT license" src="https://img.shields.io/badge/License-MIT-yellow.svg"/>
</p>

<hr>

# Testkube Jenkins Plugin

This is a Jenkins Plugin for managing your Testkube installation.

Use it to install Testkube CLI to manage your resources, run tests and test suites, or anything else.

## Table of content

- [Testkube Jenkins Plugin](#testkube-jenkins-plugin)
  - [Table of content](#table-of-content)
  - [Installation](#installation)
  - [Usage](#usage)
    - [Testkube Pro](#testkube-pro)
    - [Self-hosted instance](#self-hosted-instance)
    - [Examples](#examples)
  - [Inputs](#inputs)
    - [Common](#common)
    - [Kubernetes (`kubectl`)](#kubernetes-kubectl)
    - [Pro and Enterprise](#pro-and-enterprise)


## Installation
TODO: Add instructions on how to install the Jenkins plugin

## Usage

### Testkube Pro

To use the Jenkins Plugin for the [**Testkube Pro**](https://app.testkube.io), you need to [**create API token**](https://docs.testkube.io/testkube-pro/organization-management#api-tokens).

Then, pass the `TK_ORG` , `TK_ENV` and `TK_API_TOKEN` environment variables to configure the CLI. Additional parameters can be passed to the CLI directly based on your use case:

```
pipeline {
    agent any

    environment {
        TK_ORG = "org-id"
        TK_ENV = "env-id"
        TK_API_TOKEN = credentials("tk_api_token")
    }
    stages {
        stage('Example') {
            steps {
                script {
                    setupTestkube()
                    sh 'testkube run test your-test"
                    sh 'testkube run testsuite your-test-suite --some-arg --other-arg"
                }
            }
        }
    }
}
```

It will be probably unsafe to keep directly in the workflow's YAML configuration, so you may want to use [**Jenkins Credentials**](https://www.jenkins.io/doc/book/using/using-credentials/) instead.

### Self-hosted instance

To connect to the self-hosted instance, you need to have `kubectl` configured for accessing your Kubernetes cluster, and simply passing optional namespace, if the Testkube is not deployed in the default `testkube` namespace, i.e.:

```
pipeline {
    agent any

    environment {
        TK_NAMESPACE: 'custom-testkube-namespace'
        TK_URL: 'custom-testkube-url'
    }
    stages {
        stage('Example') {
            steps {
                script {
                    setupTestkube()
                    sh 'testkube run test your-test"
                }
            }
        }
    }
}
```

### Examples


## Inputs

In addition to the common inputs, there are specific inputs for connecting to kubectl and Testkube Pro.

### Common

| Required | Name              | Description                                                                                                                  |
|:--------:|-------------------|------------------------------------------------------------------------------------------------------------------------------|
|    ✗     | `TK_CHANNEL`      | Distribution channel to install the latest application from - one of `stable` or `beta` (default: `stable`)                  |
|    ✗     | `TK_VERSION`      | Static Testkube CLI version to force its installation instead of the latest                                                  |
|    ✗     | `NO_COLOR`        | Disables ANSI coloring, which improves console output if the Jenkins AnsiColor is not configured                             |
|    ✗     | `TK_DEBUG`        | Set to "1" or "true" to print Java stack trace to the console output                                                         |

### Kubernetes (`kubectl`)

| Required | Name           | Description                                                                            |
|:--------:|----------------|----------------------------------------------------------------------------------------|
|    ✗     | `TK_NAMESPACE`    | Set namespace where Testkube is deployed to (default: `testkube`)                   |

### Pro and Enterprise

| Required | Name           | Description                                                                                                                                                                                                                               |
|:--------:|----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|    ✓     | `TK_ORG` | The organization ID from Testkube Pro or Enterprise - it starts with `tkc_org`, you may find it i.e. in the dashboard's URL                                                                                                             |
|    ✓     | `TK_ENV`  | The environment ID from Testkube Pro or Enterprise - it starts with `tkc_env`, you may find it i.e. in the dashboard's URL                                                                                                              |
|    ✓     | `TK_API_TOKEN`        | API token that has at least a permission to run specific test or test suite. You may read more about [**creating API token**](https://docs.testkube.io/testkube-pro/organization-management#api-tokens) in Testkube Pro or Enterprise |
|    ✗     | `TK_URL`          | URL of the Testkube Enterprise instance, if applicable                                                                                                                                                                                    |
