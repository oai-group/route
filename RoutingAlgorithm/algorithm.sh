#!/usr/bin/env bash

# Shell script absolute path 
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR

# Install maven
maven_version=$(apt show maven|grep Version|awk '{print $2}')
if [ $maven_version ]; then  
  echo "maven已安装,当前版本为:{$maven_version}"
else
  apt install -y maven
fi

# Docker network create:连接redis_instance、algorithm_instance和onos22

# Package RoutingAlgorithm-1.0-SNAPSHOT.jar
mvn clean
mvn package

# Build docker image and run
algorithm_instance=$(docker ps -a|grep algorithm_instance|awk '{print $1}')
if [ $algorithm_instance ]; then  
  docker rm -f $algorithm_instance
fi
algorithm_image=$(docker images|grep algorithm|awk '{print $1}')
if [ $algorithm_image ]; then  
  docker rmi -f $algorithm_image
fi
docker build -t algorithm:latest .
docker run -itd --name algorithm_instance -p 1053:1053 algorithm:latest

# Clean java target
mvn clean