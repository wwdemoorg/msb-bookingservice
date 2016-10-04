REGISTRY=${1}
NAME_SPACE=${2}
SD_URL=${3}
SD_TOKEN=${4}
MONGO_BRIDGE=${5}

docker build -f ./Dockerfile_BlueMix -t ${REGISTRY}/${NAME_SPACE}/bookingservice-java .
docker push ${REGISTRY}/${NAME_SPACE}/bookingservice-java

cf ic run -m 128 -e CCS_BIND_APP=${MONGO_BRIDGE} -e SERVICE_NAME=booking -e SD_URL=${SD_URL} -e SD_TOKEN=${SD_TOKEN} --name booking_java1 ${REGISTRY}/${NAME_SPACE}/bookingservice-java
