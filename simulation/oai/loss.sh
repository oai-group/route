#!/usr/bin/env bash

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

# Check if namespace if h9 exists
h9=$(ip netns|grep h9|awk '{print $1}')
if [ -z $h9 ]; then  
  echo "命名空间h9不存在,请先运行topo.sh和manage.sh生成回传网络和Host"
  exit
fi
ip netns exec h9 python telemetry/sniffer.loss.py