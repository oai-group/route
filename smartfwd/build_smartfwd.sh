#!/usr/bin/env bash

# Shell script absolute path 
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR

# Remove old onos-apps-smartfwd-oar.oar
file=$(ls|grep onos-apps-smartfwd-oar.oar)
if [ $file ]; then  
  rm onos-apps-smartfwd-oar.oar
fi

# Install zcq/onos:latest
image=$(docker images|grep zcq/onos|awk '{print $1}')
if [ $image ]; then  
  echo "$image镜像已安装"
else
  imagefile=$(ls|grep onos.tar)
  if [ $imagefile ]; then
    echo "通过onos.tar导入容器镜像"  
    docker load -i onos.tar -q
    image=$(docker images|grep zcq/onos|awk '{print $1}')
    if [ $image ]; then  
      echo "$image镜像已安装"
    else
      echo "镜像导入失败,请重新重新尝试"
      exit
    fi
  else
    echo "请先在$DIR目录下导入onos.tar"
  fi
fi

# Build onos-apps-smartfwd-oar.oar
container=$(docker ps -a|grep onos_for_build|awk '{print $1}')
if [ $container ]; then  
  docker rm -f $container 
fi
docker run -itd -w /home/onos --name onos_for_build zcq/onos:latest
docker cp $DIR/smartfwd onos_for_build:/home/onos/apps
docker exec -i onos_for_build bazel build //apps/smartfwd:all
docker cp onos_for_build:/home/onos/bazel-bin/apps/smartfwd/onos-apps-smartfwd-oar.oar $DIR
docker rm -f onos_for_build