#!/usr/bin/env bash

# Shell script absolute path 
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "当前脚本所在路径为: $DIR"
cd $DIR
cd ..

# Install ethtool
ethtool=$(dpkg --status ethtool 2>/dev/null|grep Version|awk '{print $2}')
if [ $ethtool ]; then  
  echo "ethtool已安装,当前版本为:{$ethtool}"
else
  apt update && apt install -y ethtool
  ethtool=$(dpkg --status ethtool 2>/dev/null|grep Version|awk '{print $2}')
  if [ $ethtool ]; then  
    echo "ethtool安装成功,当前版本为:{$ethtool}"
  else
    echo "ethtool安装失败,请手动安装"
    exit
  fi
fi

# Check Python3
python3_version=$(python3 -V|awk '{print $2}')
if [ $python3_version ]; then  
  echo "python3已安装,当前版本为:{$python3_version}"
else
  apt update && apt install -y python3
fi

# Check Python3-venv
python3venv_version=$(dpkg --status python3-venv 2>/dev/null|grep Version|awk '{print $2}')
if [ $python3venv_version ]; then  
  echo "python3-venv已安装,当前版本为:{$python3venv_version}"
else
  apt update && apt install -y python3-venv
fi

# Check if .py38 has been built
py38=$(ls -a ../|grep .py38)
if [ $py38 ]; then  
  echo "Python3虚拟环境目录已安装"
else
  python3 -m venv .py38
fi

# .py38 install requirements
source ./.py38/bin/activate
pip install -r ./requirements.txt