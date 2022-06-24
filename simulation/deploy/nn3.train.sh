#!/bin/bash


for model_id in 0 2 4 6 8 10 18 19 20;do
  python ./routing/nn3/nn.main.py --model_id $model_id
done