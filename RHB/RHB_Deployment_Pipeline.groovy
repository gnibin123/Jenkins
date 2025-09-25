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

                    // Securely use credentials
                    withCredentials([usernamePassword(credentialsId: 'rhb-sftp-credentials', usernameVariable: 'SFTP_USER', passwordVariable: 'SFTP_PASSWORD')]) {
                        // Use PowerShell to run the SFTP command
                        def checkFileCommand = """
                        sftp -oBatchMode=no -b - ${SFTP_USER}:${SFTP_PASSWORD}@${SFTP_SERVER} <<< "cd ${REMOTE_DIR}; ls ${filePattern};"
                        """
                        powershell(script: checkFileCommand, returnStatus: true) // Execute the command in PowerShell
                    }
                }
            }
        }

        stage('Download Versioned File from SFTP') {
            steps {
                script {
                    def downloadCommand = """
                    sftp -oBatchMode=no -b - ${SFTP_USER}:${SFTP_PASSWORD}@${SFTP_SERVER} <<< "cd ${REMOTE_DIR}; get ${filePattern} ${LOCAL_DIR}/; bye"
                    """

                    // Securely use credentials
                    withCredentials([usernamePassword(credentialsId: 'rhb-sftp-credentials', usernameVariable: 'SFTP_USER', passwordVariable: 'SFTP_PASSWORD')]) {
                        powershell(script: downloadCommand) // Execute the SFTP download command in PowerShell
                    }
                }
            }
        }
    }
}
