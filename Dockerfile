FROM websphere-liberty:microProfile
RUN installUtility install  --acceptLicense ejbLite-3.2
COPY server.xml /config/server.xml
COPY target/bookingservice-java-2.0.0-SNAPSHOT.war /config/apps/

ENV MONGO_HOST=booking-db