FROM websphere-liberty:microProfile
RUN installUtility install  --acceptLicense logstashCollector-1.0
COPY server.xml /config/server.xml
COPY target/bookingservice-java-2.0.0-SNAPSHOT.war /config/apps/

ENV MONGO_HOST=booking-db