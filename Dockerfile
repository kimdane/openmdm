FROM java:8

RUN adduser --home /opt/openidm --disabled-password --disabled-login openidm 
USER openidm
WORKDIR /opt/openidm
COPY --chown=openidm:openidm . /opt/openidm
EXPOSE 8080
ENTRYPOINT ["/opt/openidm/startup.sh"]
