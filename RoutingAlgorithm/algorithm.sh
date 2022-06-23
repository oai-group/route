#!/usr/bin/env bash

# Shell script absolute path 
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "当前脚本所在路径为: $DIR"
cd $DIR

# Check if redis_instance and onos22 exist
redis_instance=$(docker ps|grep redis_instance|awk '{print $1}')
onos_instance=$(docker ps|grep onos22|awk '{print $1}')
if [ -z $redis_instance ] || [ -z $onos_instance ]; then  
  echo "onos和redis容器未创建或启动,请先运行docker.sh创建"
  exit
fi

# Check if onos-apps-smartfwd-oar.oar exists
oar_file=$(ls ../smartfwd|grep onos-apps-smartfwd-oar.oar)
if [ -z $oar_file ]; then  
  echo "onos-apps-smartfwd-oar.oar文件不存在,请先运行build_smartfwd.sh创建"
  exit
fi

# Install sshpass
sshpass_version=$(apt show sshpass|grep Version|awk '{print $2}')
if [ $sshpass_version ]; then  
  echo "sshpass已安装,当前版本为:{$sshpass_version}"
else
  apt install -y sshpass
fi

# Install maven
maven_version=$(apt show maven|grep Version|awk '{print $2}')
if [ $maven_version ]; then  
  echo "maven已安装,当前版本为:{$maven_version}"
else
  apt install -y maven > /dev/null
  maven_version=$(apt show maven|grep Version|awk '{print $2}')
  if [ $maven_version ]; then  
    echo "maven安装成功,当前版本为:{$maven_version}"
  else
    echo "maven安装失败,请手动安装"
    exit
  fi
fi

# Docker network create
algorithm_bridge=$(docker network ls|grep algorithm_bridge|awk '{print $1}')
if [ -z $algorithm_bridge ]; then  
  docker network create --driver bridge algorithm_bridge
  echo "成功创建网桥:algorithm_bridge"
else
  echo "已存在网桥:algorithm_bridge"
fi

# Package RoutingAlgorithm-1.0-SNAPSHOT.jar
mvn clean
mvn package

# Build docker image and run algorithm_instance
algorithm_instance=$(docker ps -a|grep algorithm_instance|awk '{print $1}')
if [ $algorithm_instance ]; then  
  docker rm -f $algorithm_instance > /dev/null
  echo "删除旧route algorithm容器: $algorithm_instance"
fi
algorithm_image=$(docker images|grep algorithm|awk '{print $1}')
if [ $algorithm_image ]; then  
  docker rmi -f $algorithm_image > /dev/null
  echo "删除旧route algorithm镜像: $algorithm_instance"
fi

docker build -t algorithm:latest . > /dev/null
algorithm_image=$(docker images|grep algorithm|awk '{print $1}')
if [ $algorithm_image ]; then  
  echo "通过Dockerfile构建新的route algorithm镜像: $algorithm_instance"
fi

docker run -itd --name algorithm_instance -p 1053:1053 algorithm:latest > /dev/null
if [ $algorithm_instance ]; then  
  echo "创建新的route algorithm容器: $algorithm_instance"
  sleep 5
fi

# 连接redis_instance、algorithm_instance和onos22到网桥algorithm_bridge
connect_state=$(docker inspect redis_instance|grep algorithm_bridge|awk '{print $1}')
if [ $connect_state ]; then  
  echo "redis_instance 已连接网桥 algorithm_bridge"
  docker network disconnect algorithm_bridge redis_instance > /dev/null
  docker network connect algorithm_bridge redis_instance > /dev/null
  echo "redis_instance 重新连接网桥 algorithm_bridge"
else
  docker network connect algorithm_bridge redis_instance > /dev/null
fi
connect_state=$(docker inspect redis_instance|grep algorithm_bridge|awk '{print $1}')
if [ $connect_state ]; then  
  echo "redis_instance 成功连接网桥 algorithm_bridge"
else
  echo "redis_instance 连接网桥 algorithm_bridge 失败"
  exit
fi

connect_state=$(docker inspect algorithm_instance|grep algorithm_bridge|awk '{print $1}')
if [ $connect_state ]; then  
  echo "algorithm_instance 已连接网桥 algorithm_bridge"
  docker network disconnect algorithm_bridge algorithm_instance > /dev/null
  docker network connect algorithm_bridge algorithm_instance > /dev/null
  echo "algorithm_instance 重新连接网桥 algorithm_bridge"
else
  docker network connect algorithm_bridge algorithm_instance > /dev/null
fi
connect_state=$(docker inspect algorithm_instance|grep algorithm_bridge|awk '{print $1}')
if [ $connect_state ]; then  
  echo "algorithm_instance 成功连接网桥 algorithm_bridge"
else
  echo "algorithm_instance 连接网桥 algorithm_bridge 失败"
  exit
fi

connect_state=$(docker inspect onos22|grep algorithm_bridge|awk '{print $1}')
if [ $connect_state ]; then  
  echo "onos22 已连接网桥 algorithm_bridge"
  docker network disconnect algorithm_bridge onos22 > /dev/null
  docker network connect algorithm_bridge onos22 > /dev/null
  echo "onos22 重新连接网桥 algorithm_bridge"
else
  docker network connect algorithm_bridge onos22 > /dev/null
fi
connect_state=$(docker inspect onos22|grep algorithm_bridge|awk '{print $1}')
if [ $connect_state ]; then  
  echo "onos22 成功连接网桥 algorithm_bridge"
else
  echo "onos22 连接网桥 algorithm_bridge 失败"
  exit
fi
echo "容器redis_instance algorithm_instance onos22连接网桥algorithm_bridge成功"

# Clean java target
mvn clean

# 卸载fwd模块,安装onos-apps-smartfwd-oar.oar并启动
rm /root/.ssh/known_hosts
active_apps=$(sshpass -p "rocks" ssh -o StrictHostKeyChecking=no -p 8101 onos@localhost "apps -s -a")
fwd_state=$(echo $active_apps|xargs -n 1|grep org.onosproject.fwd)
if [ $fwd_state ]; then  
  docker exec -i onos22 ./bin/onos-app localhost deactivate org.onosproject.fwd > /dev/null
  echo "org.onosproject.fwd deactivated"
fi
docker cp ../smartfwd/onos-apps-smartfwd-oar.oar onos22:/root/onos/apps
docker exec -i onos22 ./bin/onos-app localhost reinstall! ./apps/onos-apps-smartfwd-oar.oar > /dev/null
active_apps=$(sshpass -p "rocks" ssh -o StrictHostKeyChecking=no -p 8101 onos@localhost "apps -s -a")
smartfwd_state=$(echo $active_apps|xargs -n 1|grep org.onosproject.smartfwd)
if [ $smartfwd_state ]; then  
  echo "smartfwd模块安装并启动,请查看是否和algorithm_instance容器建立连接"
else
  echo "smartfwd模块安装失败!!!"
fi