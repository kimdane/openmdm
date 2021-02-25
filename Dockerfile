FROM java:8

USER guest
COPY . /opt/openidm
WORKDIR /opt/openidm
ENTRYPOINT ["/opt/openidm/startup.sh"]
