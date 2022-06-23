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

# Install sshpass
sshpass_version=$(apt show sshpass|grep Version|awk '{print $2}')
if [ $sshpass_version ]; then  
  echo "sshpass已安装,当前版本为:{$sshpass_version}"
else
  apt install -y sshpass
fi

# Start Redis:存储链路时延、丢包率和路由信息
redis_instance=$(docker ps -a|grep redis_instance|awk '{print $1}')
if [ $redis_instance ]; then  
  docker rm -f $redis_instance > /dev/null
  echo "删除旧redis容器: $redis_instance"
fi
docker run -itd --name redis_instance -p 6379:6379 redis > /dev/null
redis_instance=$(docker ps -a|grep redis_instance|awk '{print $1}')
if [ $redis_instance ]; then  
  echo "创建新的redis容器: $redis_instance"
fi

# Start ONOS:启动ONOS控制器,加载telemetry.flow.json
onos_instance=$(docker ps -a|grep onos22|awk '{print $1}')
if [ $onos_instance ]; then  
  docker rm -f $onos_instance > /dev/null
  echo "删除旧onos容器: $onos_instance"
fi
rm -rf /tmp/data
mkdir /tmp/data
cp $DIR/telemetry.flow.json /tmp/data
echo "拷贝telemetry.flow.json"

docker run -itd -p 8181:8181 -p 8101:8101 -p 6666:6653 -p 1050:1050 -p 1051:1051 -p 5005:5005 -p 830:830 -p 7896:7896 -p 1054:1054 -p 1060:1060 -v /tmp/data:/data --name onos22 onosproject/onos > /dev/null
onos_instance=$(docker ps -a|grep onos22|awk '{print $1}')
if [ $onos_instance ]; then  
  echo "创建新的onos容器: $onos_instance"
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