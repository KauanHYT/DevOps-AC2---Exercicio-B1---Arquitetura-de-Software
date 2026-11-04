// B1 / Desafio 6 (ADR-006) - esteira do monorepo, com um caminho independente por componente.
//
// Cada componente (Core Java, ia-service, notificacao-service) passa pelas mesmas barreiras:
//   testes (no Core, inclui as regras ArchUnit dos ADRs) -> imagem -> homologação + smoke test.
// Um componente só roda quando o código DELE mudou desde o último build bem-sucedido,
// e a falha de um não impede o deploy dos outros (failFast desligado).

pipeline {
    agent any

    options {
        skipDefaultCheckout()
        disableConcurrentBuilds()
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    parameters {
        string(name: 'BRANCH', defaultValue: 'feature/desafio-6',
               description: 'Branch do repositório local (montado em /repo) a construir')
        booleanParam(name: 'CONSTRUIR_TUDO', defaultValue: false,
                     description: 'Ignora a detecção de mudanças e roda todos os componentes')
    }

    environment {
        REDE_HOMOLOG = 'b1-ci'
    }

    stages {
        stage('Checkout e mudanças') {
            steps {
                script {
                    def scmVars = checkout scm
                    env.TAG = scmVars.GIT_COMMIT.substring(0, 7)
                    def anterior = scmVars.GIT_PREVIOUS_SUCCESSFUL_COMMIT
                    def tudo = !anterior || params.CONSTRUIR_TUDO
                    def arquivos = []
                    if (!tudo) {
                        arquivos = sh(script: "git diff --name-only ${anterior} ${scmVars.GIT_COMMIT}",
                                      returnStdout: true).trim().tokenize('\n')
                    }
                    env.MUDOU_CORE = (tudo || mudou(arquivos, ['src/', 'pom.xml', 'Dockerfile', '.mvn/', 'mvnw'])).toString()
                    env.MUDOU_IA = (tudo || mudou(arquivos, ['ia-service/'])).toString()
                    env.MUDOU_NOTIFICACAO = (tudo || mudou(arquivos, ['notificacao-service/'])).toString()
                    echo "commit ${env.TAG} | último sucesso: ${anterior ?: 'nenhum'} | " +
                         "core=${env.MUDOU_CORE} ia=${env.MUDOU_IA} notificacao=${env.MUDOU_NOTIFICACAO}"
                }
            }
        }

        stage('Componentes') {
            failFast false
            parallel {
                stage('Core (Java)') {
                    when { environment name: 'MUDOU_CORE', value: 'true' }
                    stages {
                        stage('Core: testes e ArchUnit') {
                            steps {
                                sh 'chmod +x mvnw && ./mvnw -B verify'
                            }
                            post {
                                always { junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml' }
                            }
                        }
                        stage('Core: imagem') {
                            steps {
                                sh "docker build -t ac1/core:${env.TAG} ."
                            }
                        }
                        stage('Core: homologação') {
                            steps {
                                sh "sh ci/deploy-homolog.sh ac1-core-homolog ac1/core:${env.TAG} 8080 /actuator/health"
                            }
                        }
                    }
                }

                stage('ia-service (Python)') {
                    when { environment name: 'MUDOU_IA', value: 'true' }
                    stages {
                        stage('ia-service: testes') {
                            steps {
                                dir('ia-service') {
                                    sh '''
                                        python3 -m venv .venv
                                        . .venv/bin/activate
                                        pip install -q -r requirements-dev.txt
                                        pytest -q --junitxml=relatorio-testes.xml
                                    '''
                                }
                            }
                            post {
                                always { junit allowEmptyResults: true, testResults: 'ia-service/relatorio-testes.xml' }
                            }
                        }
                        stage('ia-service: imagem') {
                            steps {
                                sh "docker build -t ac1/ia-service:${env.TAG} ia-service"
                            }
                        }
                        stage('ia-service: homologação') {
                            steps {
                                sh "sh ci/deploy-homolog.sh ac1-ia-homolog ac1/ia-service:${env.TAG} 8000 /health"
                            }
                        }
                    }
                }

                stage('notificacao-service (Python)') {
                    when { environment name: 'MUDOU_NOTIFICACAO', value: 'true' }
                    stages {
                        stage('notificacao-service: testes') {
                            steps {
                                dir('notificacao-service') {
                                    sh '''
                                        python3 -m venv .venv
                                        . .venv/bin/activate
                                        pip install -q -r requirements-dev.txt
                                        pytest -q --junitxml=relatorio-testes.xml
                                    '''
                                }
                            }
                            post {
                                always { junit allowEmptyResults: true, testResults: 'notificacao-service/relatorio-testes.xml' }
                            }
                        }
                        stage('notificacao-service: imagem') {
                            steps {
                                sh "docker build -t ac1/notificacao-service:${env.TAG} notificacao-service"
                            }
                        }
                        stage('notificacao-service: homologação') {
                            steps {
                                // sem RabbitMQ na homologação: sobe só a API, com o consumidor desligado
                                sh "sh ci/deploy-homolog.sh ac1-notificacao-homolog ac1/notificacao-service:${env.TAG} 8001 /health -e CONSUMIDOR_HABILITADO=false"
                            }
                        }
                    }
                }
            }
        }
    }
}

// Algum arquivo alterado está dentro dos caminhos do componente?
boolean mudou(List arquivos, List caminhos) {
    for (String arquivo : arquivos) {
        for (String caminho : caminhos) {
            if (arquivo.startsWith(caminho)) {
                return true
            }
        }
    }
    return false
}