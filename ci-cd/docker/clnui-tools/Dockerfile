# Use the official Kafdrop image (Ubuntu-based)
FROM obsidiandynamics/kafdrop:4.2.0

ARG KAFKA_VERSION
ARG SCALA_VERSION

# Install tools using apt (Ubuntu package manager)
RUN apt-get update && apt-get install -y --no-install-recommends git gzip jq vim watch wget zsh \
    && rm -rf /var/lib/apt/lists/*  # Clean up cache

# add zsh
RUN chsh -s $(which zsh)
RUN sh -c "$(wget https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh -O -)"
RUN git clone https://github.com/zsh-users/zsh-autosuggestions ${ZSH_CUSTOM:-~/.oh-my-zsh/custom}/plugins/zsh-autosuggestions
RUN sed -i 's|plugins=(git)|plugins=(git zsh-autosuggestions)|g' /root/.zshrc

# Kafka cli installation
ENV BASE_DIR="/etc/clnui-tools"
ENV KAFKA_BASE_DIR="${BASE_DIR}/kafka"
ENV KAFKA_HOME="${KAFKA_BASE_DIR}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}"
ENV KAFKA_CONFIG_HOME="${KAFKA_BASE_DIR}/configs"
RUN mkdir -p ${KAFKA_HOME} ${KAFKA_CONFIG_HOME}
RUN wget -q "https://archive.apache.org/dist/kafka/${KAFKA_VERSION}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz" -O - | tar -xz -C ${KAFKA_BASE_DIR}

COPY kafka-sample-cmd.md ${KAFKA_CONFIG_HOME}/kafka-sample-cmd.md
WORKDIR ${BASE_DIR}
# Verify CLI tools (optional)
RUN ls -l ${KAFKA_BASE_DIR}

# Keep the original Kafdrop entrypoint
CMD ["/kafdrop.sh"]