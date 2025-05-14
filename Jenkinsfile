pipeline {
    agent any
    tools {
        maven 'Maven_3.8.6'
        jdk 'jdk17'
    }
    environment {
        // Token de autenticación para SonarCloud (opcional si está en Jenkins global config)
        SONAR_TOKEN = credentials('SONAR_TOKEN')

        // Variables de entorno para R2DBC
        DB_URL = credentials('R2DBC_URL')
        DB_USERNAME = credentials('R2DBC_USERNAME')
        DB_PASSWORD = credentials('R2DBC_PASSWORD')

        // Variables para Kafka
        BOOTSTRAP_SERVER = credentials('KAFKA_BOOTSTRAP_SERVERS')
        KAFKA_USERNAME = credentials('KAFKA_USERNAME')
        KAFKA_PASSWORD = credentials('KAFKA_PASSWORD')

        // Variables para Supabase
        SUPABASE_PROJECT_URL = credentials('SUPABASE_PROJECT_URL')
        SUPABASE_API_KEY = credentials('SUPABASE_API_KEY')
        SUPABASE_BUCKET = credentials('SUPABASE_BUCKET')
        SUPABASE_FOLDER = credentials('SUPABASE_FOLDER')

        // Puerto de aplicación
        PORT = '8086'
    }
    stages {
        stage('Clonar repositorio') {
            steps {
                git url: 'https://github.com/LizbetArias/AS222S6_ms_PRS1.git', branch: 'Backend'
            }
        }
        stage('Compilar Proyecto') {
            steps {
                sh 'mvn clean compile'
            }
        }
        stage('Ejecutar pruebas') {
            steps {
                sh 'mvn test'
            }
        }
        stage('Generar Artefacto') {
            steps {
                sh 'mvn package'
                archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
            }
        }
        stage('Análisis SonarCloud') {
            steps {
                withSonarQubeEnv('SonarCloud') { // Este nombre debe coincidir con el configurado en Jenkins > Configurar sistema
                    sh '''
                        mvn sonar:sonar \
                        -Dsonar.projectKey=LizbetArias_AS222S6_ms_PRS1 \
                        -Dsonar.organization=lizbetarias \
                        -Dsonar.host.url=https://sonarcloud.io
                    '''
                }
            }
        }
        stage('Esperar análisis en Sonar') {
            steps {
                timeout(time: 1, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
    }
    post {
        always {
            echo 'Pipeline ejecutado - revisando resultados'
            // cleanWs() // Descomenta si quieres limpiar el workspace al final
        }
        success {
            echo 'Pipeline exitoso'
        }
        failure {
            echo 'Pipeline falló - revisar logs de error'
        }
    }
}