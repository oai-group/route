#!/usr/bin/env bash

# Uninstall redis container
redis_instance=$(docker ps -a|grep redis_instance|awk '{print $1}')
if [ $redis_instance ]; then
  connect_state=$(docker inspect redis_instance|grep oai_bridge|head -1|awk '{print $2}')
  if [ $connect_state ]; then  
    docker network disconnect oai_bridge redis_instance > /dev/null
    echo "断开 redis_instance 与网桥 oai_bridge 的连接"
  fi
  docker rm -f $redis_instance > /dev/null
  echo "删除redis容器: $redis_instance"
fi

# Uninstall onos container
onos_instance=$(docker ps -a|grep onos22|awk '{print $1}')
if [ $onos_instance ]; then
  connect_state=$(docker inspect onos22|grep oai_bridge|head -1|awk '{print $2}')
  if [ $connect_state ]; then  
    docker network disconnect oai_bridge onos22 > /dev/null
    echo "断开 onos22 与网桥 oai_bridge 的连接"
  fi
  docker rm -f $onos_instance > /dev/null
  echo "删除onos容器: $onos_instance"
fi

# Uninstall route algorithm container
algorithm_instance=$(docker ps -a|grep algorithm_instance|awk '{print $1}')
if [ $algorithm_instance ]; then
  connect_state=$(docker inspect algorithm_instance|grep oai_bridge|head -1|awk '{print $2}')
  if [ $connect_state ]; then  
    docker network disconnect oai_bridge algorithm_instance > /dev/null
    echo "断开 algorithm_instance 与网桥 oai_bridge 的连接"
  fi
  docker rm -f $algorithm_instance > /dev/null
  echo "删除route algorithm容器: $algorithm_instance"
fi

# Uninstall route algorithm image
algorithm_image=$(docker images|grep algorithm|awk '{print $1}')
if [ $algorithm_image ]; then  
  docker rmi -f $algorithm_image > /dev/null
  echo "删除route algorithm镜像: $algorithm_instance"
fi

# Docker network remove
oai_bridge=$(docker network ls|grep oai_bridge|awk '{print $1}')
if [ $oai_bridge ]; then  
  docker network rm oai_bridge
  echo "删除网桥: oai_bridge"
fi

for bridge in `ovs-vsctl list-br`; 
do
    ovs-vsctl del-br $bridge
    echo "$bridge" deleted
done

for hid in {0..24}
do
    ip netns del "h${hid}"
done

ip netns del test

for name in $(ifconfig -a | sed 's/[ \t].*//;/^\(lo\|\)$/d' | grep "-")
do
    echo $name
    ip link del dev ${name}
done

ip link del dev nat1
ip link del dev nat2

iptables -P INPUT ACCEPT
iptables -P FORWARD ACCEPT
iptables -P OUTPUT ACCEPT
iptables -t nat -F
iptables -t mangle -F
iptables -F
iptables -X

pkill -f '^gogen$'
pkill -f '^golisten$'
pkill "ovs-tcpdump"
pkill "tcpdump"

service docker restart