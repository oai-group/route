#!/usr/bin/env bash

# Clear all
for bridge in `ovs-vsctl list-br`; 
do
  ovs-vsctl del-br $bridge
  echo "$bridge" deleted
done

for hid in {0..18}
do
  if [ $(ip netns|awk '{print $1}'|grep h${hid}$) ]; then
    ip netns del "h${hid}"
    echo "h${hid}" deleted
  fi
done

for name in $(ifconfig -a | sed 's/[ \t].*//;/^\(lo\|\)$/d' | grep "-")
do
  echo "$name" deleted
  ip link del dev ${name} 2>/dev/null 
done

if [ $(ifconfig | grep nat1 | awk '{print $1}') ]; then
  ip link del dev nat1
fi

if [ $(ifconfig|grep nat2|awk '{print $1}') ]; then
  ip link del dev nat2
fi

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

# Shell script absolute path 
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "当前脚本所在路径为: $DIR"
cd $DIR
cd ..

# Check if .py38 has been built
py38=$(ls -a ./|grep .py38)
if [ $py38 ]; then  
  echo "Python3虚拟环境目录已安装"
else
  echo "请先运行init.sh创建Python3虚拟环境并下载依赖"
  exit
fi

# Enter Python3 virtual environment
source ./.py38/bin/activate
source ./python.rc
python ./topo/distributed/router.py