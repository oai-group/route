#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
apt install -y sshpass

# Start Redis:存储链路时延、丢包率和路由信息
redis_instance=$(docker ps -a|grep redis_instance|awk '{print $1}')
if [ $redis_instance ]; then  
  docker rm -f $redis_instance
fi
docker run -itd --name redis_instance -p 6379:6379 redis

# Start ONOS:启动ONOS控制器,加载telemetry.flow.json
onos_instance=$(docker ps -a|grep onos22|awk '{print $1}')
if [ $onos_instance ]; then  
  docker rm -f $onos_instance
fi
rm -rf /tmp/data
mkdir /tmp/data
cp $DIR/telemetry.flow.json /tmp/data

docker run -itd -p 8181:8181 -p 8101:8101 -p 6666:6653 -p 1050:1050 -p 1051:1051 -p 5005:5005 -p 830:830 -p 7896:7896 -p 1054:1054 -p 1060:1060 -v /tmp/data:/data --name onos22 onosproject/onos
echo "Sleep for 20 seconds"
sleep 20
echo "onos docker setup done"

# 加载fwd、proxyarp和openflow模块
rm /root/.ssh/known_hosts
sshpass -p "rocks" ssh -o StrictHostKeyChecking=no -p 8101 onos@localhost "app activate org.onosproject.fwd"
echo "org.onosproject.fwd activated"
sshpass -p "rocks" ssh -o StrictHostKeyChecking=no -p 8101 onos@localhost "app activate org.onosproject.proxyarp"
echo "org.onosproject.proxyarp activated"
sshpass -p "rocks" ssh -o StrictHostKeyChecking=no -p 8101 onos@localhost "app activate org.onosproject.openflow"
echo "org.onosproject.openflow activated"