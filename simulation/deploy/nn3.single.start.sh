#!/bin/bash

export CUDA_VISIBLE_DEVICES=-1

for model_id in 0 2 4 6 8 10 18 19 20;
  do
    nohup python ./routing/nn3/nn.py --model_id ${model_id} > /tmp/nn3.${model_id}.log 2>&1 &
  done