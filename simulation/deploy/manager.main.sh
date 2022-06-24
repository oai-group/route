#!/bin/bash

root_dir=`dirname $( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )`

source "${root_dir}/deploy/python.rc"
clear;

echo $root_dir
echo ">Enter project":
echo ">0. OAI"
echo ">1. Demo"
read prj
echo "Project ${prj}"


if [[ $prj -eq 0 ]]
then
      python ./topo/distributed/main.py \
  --config "${root_dir}/static/oai.config.json" \
  --topos_fn "${root_dir}/static/oai.pkl"
elif [[ $prj -eq 1 ]]
then
      python ./topo/distributed/main.py \
  --config "${root_dir}/static/demo.config.json" \
  --topos_fn "${root_dir}/static/demo.pkl"
else
  echo "Invalid project"
fi
