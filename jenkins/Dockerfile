# B1 / Desafio 6 (ADR-006): o Jenkins da esteira, configurado por arquivo (sem assistente de instalação).
FROM jenkins/jenkins:lts-jdk21

USER root

# Python para testar os serviços Python (git e curl já vêm na imagem)
RUN apt-get update \
 && apt-get install -y --no-install-recommends python3 python3-venv python3-pip \
 && rm -rf /var/lib/apt/lists/* \
 && git config --system --add safe.directory '*'

# Cliente Docker + buildx: o Jenkins constrói as imagens usando o Docker da própria máquina
COPY --from=docker:29-cli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=docker:29-cli /usr/local/libexec/docker/cli-plugins/docker-buildx /usr/local/libexec/docker/cli-plugins/docker-buildx

# Plugins e configuração versionados no repositório
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

COPY casc.yaml /var/jenkins_config/casc.yaml
ENV CASC_JENKINS_CONFIG=/var/jenkins_config/casc.yaml
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false -Dhudson.plugins.git.GitSCM.ALLOW_LOCAL_CHECKOUT=true"