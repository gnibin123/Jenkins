pipeline {
    agent any

    parameters {
        // Add a string parameter for version input
        string(name: 'VERSION', defaultValue: '', description: 'Enter the version number (e.g., 6.7.4.62.15a)')
    }

    environment {
        SFTP_SERVER = 'RHB_SFTP'
        REMOTE_DIR = '/users/rclient035/Deployments'
        LOCAL_DIR = 'D:/Clients/RHB/Artifacts'
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

                    // Using withCredentials to access username and password securely
                    withCredentials([usernamePassword(credentialsId: 'rhb-sftp-credentials', usernameVariable: 'SFTP_USER', passwordVariable: 'SFTP_PASSWORD')]) {
                        // Use SFTP to check if the file exists in the remote directory
                        def checkFileCommand = """
                        sftp -oBatchMode=no -b - ${SFTP_USER}:${SFTP_PASSWORD}@${SFTP_SERVER} <<EOF
                        cd ${REMOTE_DIR}
                        ls ${filePattern}
                        EOF
                        """
                        def result = sh(script: checkFileCommand, returnStatus: true, returnStdout: true)

                        if (result != 0) {
                            error "File ${filePattern} not found on the SFTP server."
                        } else {
                            echo "File ${filePattern} found."
                        }
                    }
                }
            }
        }

        stage('Download Versioned File from SFTP') {
            steps {
                script {
                    // Construct the full SFTP command to download the file
                    def downloadCommand = """
                    sftp -oBatchMode=no -b - ${SFTP_USER}:${SFTP_PASSWORD}@${SFTP_SERVER} <<EOF
                    cd ${REMOTE_DIR}
                    get ${filePattern} ${LOCAL_DIR}/
                    bye
                    EOF
                    """

                    // Execute the SFTP download command
                    withCredentials([usernamePassword(credentialsId: 'sftp-credentials', usernameVariable: 'SFTP_USER', passwordVariable: 'SFTP_PASSWORD')]) {
                        sh downloadCommand
                        echo "File ${filePattern} has been downloaded successfully."
                    }
                }
            }
        }
    }
}
