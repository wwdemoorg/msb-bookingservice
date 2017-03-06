FROM websphere-liberty:microProfile

ADD Dockerfile /root/Dockerfile

RUN rm -rf /opt/ibm/wlp/usr/servers/defaultServer/server.xml
ADD server.xml /opt/ibm/wlp/usr/servers/defaultServer/server.xml

RUN installUtility install --acceptLicense defaultServer
RUN rm -rf /opt/ibm/wlp/usr/servers/defaultServer/workarea

ADD ./build/libs/*.war /opt/ibm/wlp/usr/servers/defaultServer/apps

EXPOSE 9080

ENV MONGO_HOST=booking_db

CMD ["/opt/ibm/wlp/bin/server", "run", "defaultServer"]
