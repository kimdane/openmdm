FROM java:8

RUN adduser --home /opt/openmdm --disabled-password --disabled-login openmdm 
USER openmdm
WORKDIR /opt/openmdm
COPY --chown=openmdm:openmdm . /opt/openmdm
EXPOSE 8080
ENTRYPOINT ["/opt/openmdm/startup.sh"]
