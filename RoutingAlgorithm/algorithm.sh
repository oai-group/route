#!/usr/bin/env bash

# Shell script absolute path 
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR

# Check if redis_instance and onos22 exist
redis_instance=$(docker ps|grep redis_instance|awk '{print $1}')
onos_instance=$(docker ps|grep onos22|awk '{print $1}')
if [ -z $redis_instance ] || [ -z $onos_instance ]; then  
  echo "onos和redis容器未创建或启动,请先运行docker.sh创建"
  exit
fi

# Install maven
maven_version=$(apt show maven|grep Version|awk '{print $2}')
if [ $maven_version ]; then  
  echo "maven已安装,当前版本为:{$maven_version}"
else
  apt install -y maven
fi

# Docker network create
algorithm_bridge=$(docker network ls|grep algorithm_bridge|awk '{print $1}')
if [ $algorithm_bridge ]; then  
  docker network create --driver bridge algorithm_bridge
  echo "成功创建网桥:algorithm_bridge"
else
  echo "已存在网桥:algorithm_bridge"
fi

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

# 连接redis_instance、algorithm_instance和onos22到网桥algorithm_bridge
docker network connect algorithm_bridge redis_instance
docker network connect algorithm_bridge algorithm_instance
docker network connect algorithm_bridge onos22

# Clean java target
mvn clean