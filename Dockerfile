FROM java:8

WORKDIR /opt/openmdm
COPY . /opt/openmdm
RUN adduser --disabled-password --disabled-login --gecos '' openmdm && \
    mkdir -p /opt/openmdm/logs && \
    mkdir -p /opt/openmdm/audit && \
    chown -R openmdm:openmdm /opt/

USER openmdm
EXPOSE 8080
ENTRYPOINT ["/opt/openmdm/startup.sh"]
