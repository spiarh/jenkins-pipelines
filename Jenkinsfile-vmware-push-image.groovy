// Configure the build properties
properties([
    buildDiscarder(logRotator(numToKeepStr: '15', daysToKeepStr: '31')),
    disableConcurrentBuilds(),
    parameters([
        string(name: 'IMAGE', defaultValue: '', description: 'CaaSP VMware Image To Use'),
        string(name: 'IMAGE_URL', defaultValue: '', description: 'CaaSP VMware Image URL'),

        string(name: 'PLATFORM_ENDPOINT', defaultValue: 'jazz.qa.prv.suse.net', description: 'vCenter endpoint to connect to'),
        string(name: 'CREDENTIALS_ID', defaultValue: 'vcenter-api', description: 'vCenter API credentials ID'),

        booleanParam(name: 'WORKSPACE_CLEANUP', defaultValue: false, description: 'Cleanup workspace once done ?')
    ])
])

def PLATFORM = "vmware"

def configurationMap = [
    platformEndpoint: params.get('PLATFORM_ENDPOINT'),
    credentialsId: params.get('CREDENTIALS_ID'),
    branchName: 'master',

    image: params.get('IMAGE'),
    imageSourceUrl: params.get('IMAGE_URL'),

    workspaceCleanup: params.get('WORKSPACE_CLEANUP')
]

node {
    checkout scm

    def common = load("${WORKSPACE}/methods/common.groovy")
    def platform = load("${WORKSPACE}/methods/${PLATFORM}.groovy")

    stage('preparation') {
        stage('node Info') {
            common.nodeInfo()
        }

        stage('set up workspace') {
            common.setUpWorkspace()
        }

        stage('clone Kubic repos') {
            common.cloneKubicRepos(configurationMap)
        }
    }

    stage('push image') {
        if (!configurationMap.imageSourceUrl) {
            echo 'No image source URL provided, skipping task...'
        } else {
            echo configurationMap.image
            echo configurationMap.imageSourceUrl
            //platform.pushImage(configurationMap)
        }
    }

    stage('Workspace cleanup') {
        if (configurationMap.workspaceCleanup) {
            common.workspaceCleanup()
        } else {
            echo "Skipping Cleanup as request was made to NOT cleanup the workspace"
        }
    }
}

