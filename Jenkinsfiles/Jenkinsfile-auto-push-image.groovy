
// Configure the build properties
properties([
    buildDiscarder(logRotator(numToKeepStr: '15', daysToKeepStr: '31')),
    disableConcurrentBuilds(),
    parameters([
        string(name: 'IMAGE', description: "CaaSP Image To Use"),
        string(name: 'IMAGE_URL', description: "CaaSP Image URL"),

        string(name: 'PLATFORM_ENDPOINT', defaultValue: '', description: "Endpoint to connect to"),
        string(name: 'CREDENTIALS_ID', defaultValue: '', description: "Jenkins credentials ID"),

        string(name: 'JOB_CI_FILE', defaultValue: 'auto-push-image_trigger-jobs.yaml', description: 'CI configuration file for trigger_jenkins_jobs script'),
        booleanParam(name: 'DRY_RUN', defaultValue: false, description: 'Use dry-run mode when launching the jobs'),

        booleanParam(name: 'WORKSPACE_CLEANUP', defaultValue: true, description: 'Cleanup workspace once done ?')
    ])
])

//TODO remove all zypper steps, pssh, velum-interactions
node {
    checkout scm

    def common = load("./Jenkinsfiles/methods/common.groovy")

    def PLATFORM = common.getPlatformFromJobName(currentBuild)
    def platform = load("./Jenkinsfiles/methods/${PLATFORM}.groovy")

    def defaultJobParametersMap = common.readDefaultJobParameters()
    def jobParametersMap = common.readJobParameters(PLATFORM, params, defaultJobParametersMap)

    jobParametersMap = [
        jobsCiFile: params.get('JOB_CI_FILE'),
        triggerJobDryRun: params.get('DRY_RUN')
    ]

    // workaround to get/initialize the parameters available in the job
    if (currentBuild.number == 1) {
        return
    }

    stage('preparation') {
        stage('node Info') {
            common.nodeInfo()
        }

        stage('set up workspace') {
            common.setUpWorkspace(jobParametersMap)
        }

        stage('clone Kubic repos') {
            common.cloneKubicRepos()
        }
    }

    stage('push image') {
        if (!jobParametersMap.imageSourceUrl) {
            echo 'No image source URL provided, skipping task...'
        } else {
            echo jobParametersMap.image
            echo jobParametersMap.imageSourceUrl
            //platform.pushImage(jobParametersMap)
        }
    }

    stage('trigger jobs') {
        common.triggerJenkinsJobs(jobParametersMap, defaultJobParametersMap)
    }

    stage('Workspace cleanup') {
        if (jobParametersMap.workspaceCleanup) {
            common.workspaceCleanup()
        } else {
            echo "Skipping Cleanup as request was made to NOT cleanup the workspace"
        }
    }
}

