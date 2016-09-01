REGISTRY=registry.ng.bluemix.net
NAME_SPACE=wasperf
SD_URL=https://servicediscovery.ng.bluemix.net
SD_TOKEN=1dmkrt3l8j8i8a57ccqgpuf17i6cmim12uqbjqgu38c3dn3jh7ke
MONGO_BRIDGE=MongoBridge1

docker build -f ./Dockerfile_BlueMix -t ${REGISTRY}/${NAME_SPACE}/bookingservice-java .
docker push ${REGISTRY}/${NAME_SPACE}/bookingservice-java

cf ic run -m 128 -e CCS_BIND_APP=${MONGO_BRIDGE} -e SERVICE_NAME=booking -e SD_URL=${SD_URL} -e SD_TOKEN=${SD_TOKEN} --name booking_java1 ${REGISTRY}/${NAME_SPACE}/bookingservice-java
