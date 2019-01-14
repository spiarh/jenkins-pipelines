def pushImage(Map jobParams) {
    timeout(120) {
        withCredentials([usernamePassword(credentialsId: jobParams.credentialsId, usernameVariable: 'SSHUSER', passwordVariable: 'SSHPASS')]) {
            sh(script: "set -o pipefail; sshpass -e ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \"${SSHUSER}\"@${jobParams.platformEndpoint} 'Get-ChildItem Env:;git checkout ${BRANCH_NAME}; git pull; caasp-hyperv.ps1 fetchimage -caaspImageSourceUrl ${jobParams.imageSourceUrl} -nochecksum' 2>&1 | tee ${WORKSPACE}/logs/caasp-hyperv.log")
        }
    }
}

def createEnvironment(Map jobParams) {
    timeout(120) {
        // https://github.com/PowerShell/Win32-OpenSSH/issues/1049 -> Use SSH password
        withCredentials([usernamePassword(credentialsId: jobParams.credentialsId, usernameVariable: 'SSHUSER', passwordVariable: 'SSHPASS')]) {
            sh(script: "set -o pipefail; sshpass -e ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \"${SSHUSER}\"@${jobParams.platformEndpoint} 'Get-ChildItem Env:; git checkout ${jobParams.branchName}; git pull; caasp-hyperv.ps1 deploy -caaspImage ${jobParams.image} -stackName ${jobParams.stackName} -adminRam ${jobParams.adminRam} -adminCpu ${jobParams.adminCpu} -masters ${jobParams.masterCount} -masterRam ${jobParams.masterRam} -masterCpu ${jobParams.masterCpu} -workers ${jobParams.workerCount} -workerRam ${jobParams.workerRam} -workerCpu ${jobParams.workerCpu} -Force' 2>&1 | tee ${WORKSPACE}/logs/caasp-hyperv.log")
        }

        // Extract state from log file and generate environment.json
        dir("automation/caasp-hyperv") {
            sh(script: "sed '/^===/,/^===/!d ; /^===.*/d' ${WORKSPACE}/logs/caasp-hyperv.log > ./caasp-hyperv.hvstate ; jq '.' ./caasp-hyperv.hvstate > /dev/null 2>&1")
            sh(script: "cp ./caasp-hyperv.hvstate ../../logs")
            sh(script: "./tools/generate-environment")
            sh(script: "../misc-tools/generate-ssh-config ./environment.json")
            sh(script: "cp environment.json ${WORKSPACE}/environment.json")
            sh(script: "cat ${WORKSPACE}/environment.json")
        }
        archiveArtifacts(artifacts: 'environment.json', fingerprint: true)
    }
}

def destroyEnvironment(Map jobParams) {
    timeout(30) {
        withCredentials([usernamePassword(credentialsId: jobParams.credentialsId, usernameVariable: 'SSHUSER', passwordVariable: 'SSHPASS')]) {
            sh(script: "set -o pipefail; sshpass -e ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \"${SSHUSER}\"@${jobParams.platformEndpoint} 'Get-ChildItem Env:; git checkout ${jobParams.branchName}; git pull; caasp-hyperv.ps1 destroy -caaspImage ${jobParams.image} -stackName ${jobParams.stackName} -masters ${jobParams.masterCount} -workers ${jobParams.workerCount} -Force' 2>&1 | tee ${WORKSPACE}/logs/caasp-hyperv.log")
        }
    }
}

return this;
