FROM websphere-liberty:microProfile
RUN installUtility install  --acceptLicense defaultServer
COPY server.xml /config/server.xml
COPY target/bookingservice-java-2.0.0-SNAPSHOT.war /config/apps/

ENV MONGO_HOST=booking-db