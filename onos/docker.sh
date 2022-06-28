#!/usr/bin/env bash

# Shell script absolute path 
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "当前脚本所在路径为: $DIR"

# Check docker
docker_version=$(apt show docker|grep Version|awk '{print $2}')
if [ $docker_version ]; then  
  echo "docker已安装"
else
  echo "docker未安装,请手动安装docker"
  exit
fi
iptables -t filter -N DOCKER

# Install sshpass
sshpass_version=$(apt show sshpass|grep Version|awk '{print $2}')
if [ $sshpass_version ]; then  
  echo "sshpass已安装,当前版本为:{$sshpass_version}"
else
  apt update && apt install -y sshpass
  sshpass_version=$(apt show sshpass|grep Version|awk '{print $2}')
  if [ $sshpass_version ]; then  
    echo "sshpass安装成功,当前版本为:{$sshpass_version}"
  else
    echo "sshpass安装失败,请手动安装"
    exit
  fi
fi

# Docker network create
oai_bridge=$(docker network ls|grep oai_bridge|awk '{print $1}')
if [ -z $oai_bridge ]; then  
  docker network create -d bridge -o com.docker.network.bridge.name=oai --subnet 172.19.0.0/24 oai_bridge
  echo "成功创建网桥: oai_bridge"
else
  echo "已存在网桥: oai_bridge"
fi

# Start Redis:存储链路时延、丢包率和路由信息
redis_instance=$(docker ps -a|grep redis_instance|awk '{print $1}')
if [ $redis_instance ]; then
  connect_state=$(docker inspect redis_instance|grep oai_bridge|head -1|awk '{print $2}')
  if [ $connect_state ]; then  
    docker network disconnect oai_bridge redis_instance > /dev/null
    echo "断开 redis_instance 与网桥 oai_bridge 的连接"
  fi
  docker rm -f $redis_instance > /dev/null
  echo "删除旧redis容器: $redis_instance"
fi

docker run -itd --name redis_instance --network oai_bridge -p 6379:6379 redis > /dev/null

redis_instance=$(docker ps -a|grep redis_instance|awk '{print $1}')
if [ $redis_instance ]; then  
  echo "创建新的redis容器: $redis_instance"
  connect_state=$(docker inspect redis_instance|grep oai_bridge|head -1|awk '{print $2}')
  if [ $connect_state ]; then  
    echo "redis_instance 已连接网桥 oai_bridge"
  else
    echo "redis_instance 连接网桥 oai_bridge 失败"
    exit
  fi
  sleep 3
fi

# Start ONOS:启动ONOS控制器,加载telemetry.flow.json
onos_instance=$(docker ps -a|grep onos22|awk '{print $1}')
if [ $onos_instance ]; then
  connect_state=$(docker inspect onos22|grep oai_bridge|head -1|awk '{print $2}')
  if [ $connect_state ]; then  
    docker network disconnect oai_bridge onos22 > /dev/null
    echo "断开 onos22 与网桥 oai_bridge 的连接"
  fi
  docker rm -f $onos_instance > /dev/null
  echo "删除旧onos容器: $onos_instance"
fi
rm -rf /tmp/data
mkdir /tmp/data
cp $DIR/telemetry.flow.json /tmp/data
echo "拷贝telemetry.flow.json"

docker run -itd --network oai_bridge -p 8181:8181 -p 8101:8101 -p 6666:6653 -p 1050:1050 -p 1051:1051 -p 5005:5005 -p 830:830 -p 7896:7896 -p 1054:1054 -p 1060:1060 -v /tmp/data:/data --name onos22 onosproject/onos > /dev/null

onos_instance=$(docker ps -a|grep onos22|awk '{print $1}')
if [ $onos_instance ]; then  
  echo "创建新的onos容器: $onos_instance"
  connect_state=$(docker inspect onos22|grep oai_bridge|head -1|awk '{print $2}')
  if [ $connect_state ]; then  
    echo "onos22 已连接网桥 oai_bridge"
  else
    echo "onos22 连接网桥 oai_bridge 失败"
    exit
  fi
fi
echo "等待onos容器加载......"
sleep 30
echo "onos docker setup done"

# 加载fwd、proxyarp和openflow模块
docker exec -i onos22 ./bin/onos-app localhost activate org.onosproject.fwd > /dev/null
docker exec -i onos22 ./bin/onos-app localhost activate org.onosproject.proxyarp > /dev/null
docker exec -i onos22 ./bin/onos-app localhost activate org.onosproject.openflow > /dev/null

rm /root/.ssh/known_hosts
active_apps=$(sshpass -p "rocks" ssh -o StrictHostKeyChecking=no -p 8101 onos@localhost "apps -s -a")
fwd_state=$(echo $active_apps|xargs -n 1|grep org.onosproject.fwd)
proxyarp_state=$(echo $active_apps|xargs -n 1|grep org.onosproject.proxyarp)
openflow_state=$(echo $active_apps|xargs -n 1|grep 'openflow$')
if [ $fwd_state ]; then  
  echo "org.onosproject.fwd activated"
fi
if [ $proxyarp_state ]; then  
  echo "org.onosproject.proxyarp activated"
fi
if [ $openflow_state ]; then  
  echo "org.onosproject.openflow activated"
fi