pipeline {
    agent any

    parameters {
        string(name: 'VERSION', defaultValue: '', description: 'Enter the version number (e.g., 6.7.4.62.15a)')
    }

    environment {
        SFTP_SERVER = 'RHB_SFTP'
        REMOTE_DIR = '/users/rclient035/Deployments'
        LOCAL_DIR = 'D:/Clients/RHB/Artifacts'  // Use appropriate path
    }

    stages {
        stage('Check for Correct Version') {
            steps {
                script {
                    if (!params.VERSION) {
                        error "Version parameter is required!"
                    }
                    // Build the filename pattern dynamically based on the user input version
                    def filePattern = "IMS_${params.VERSION}.zip"
                    echo "Looking for file: ${filePattern}"

                    withCredentials([usernamePassword(credentialsId: 'rhb-sftp-credentials', usernameVariable: 'SFTP_USER', passwordVariable: 'SFTP_PASSWORD')]) {
                        // Use PowerShell instead of 'sh'
                        def checkFileCommand = """
                        sftp -oBatchMode=no -b - ${SFTP_USER}:${SFTP_PASSWORD}@${SFTP_SERVER} <<EOF
                        cd ${REMOTE_DIR}
                        ls ${filePattern}
                        EOF
                        """
                        powershell(script: checkFileCommand, returnStatus: true) // Use powershell instead of sh
                    }
                }
            }
        }

        stage('Download Versioned File from SFTP') {
            steps {
                script {
                    def downloadCommand = """
                    sftp -oBatchMode=no -b - ${SFTP_USER}:${SFTP_PASSWORD}@${SFTP_SERVER} <<EOF
                    cd ${REMOTE_DIR}
                    get ${filePattern} ${LOCAL_DIR}/
                    bye
                    EOF
                    """

                    withCredentials([usernamePassword(credentialsId: 'sftp-credentials', usernameVariable: 'SFTP_USER', passwordVariable: 'SFTP_PASSWORD')]) {
                        powershell(script: downloadCommand) // Use PowerShell for Windows
                    }
                }
            }
        }
    }
}
