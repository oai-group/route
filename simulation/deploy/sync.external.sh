#!/usr/bin/env bash


rsync -av --exclude '*.git' --exclude '*pyc' --exclude '__pycache__' --exclude 'video.bk' --exclude "*.hdf5" --exclude ".process" . stack@192.168.195.145:/home/stack/code/simulation/;


ssh stack@192.168.195.145 << EOF
cd /home/stack/code/simulation;
#computing
rsync -av --exclude '*.git' --exclude '.process' --exclude '*pyc' --exclude '__pycache__' --exclude 'video.bk' --exclude "*.hdf5" --exclude "*.pkl" --exclude "*.pkts" --exclude "*.partition" . stack@192.168.1.128:/home/stack/code/simulation/;
rsync -av --exclude '*.git' --exclude '.process' --exclude '*pyc' --exclude '__pycache__' --exclude 'video.bk' --exclude "*.hdf5" --exclude "*.pkl" --exclude "*.pkts" --exclude "*.partition" . stack@192.168.1.34:/home/stack/code/simulation/;
rsync -av --exclude '*.git' --exclude '.process' --exclude '*pyc' --exclude '__pycache__' --exclude 'video.bk' --exclude "*.hdf5" --exclude "*.pkl" --exclude "*.pkts" --exclude "*.partition" . stack@192.168.1.110:/home/stack/code/simulation/;
rsync -av --exclude '*.git' --exclude '.process'  --exclude '*pyc' --exclude '__pycache__' --exclude 'video.bk' --exclude "*.hdf5" --exclude "*.pkl" --exclude "*.pkts" --exclude "*.partition" . stack@192.168.1.180:/home/stack/code/simulation/;
rsync -av --exclude '*.git' --exclude '.process'  --exclude '*pyc' --exclude '__pycache__' --exclude 'video.bk' --exclude "*.hdf5" --exclude "*.pkl" --exclude "*.pkts" --exclude "*.partition" . stack@192.168.1.90:/home/stack/code/simulation/;

#gpu
rsync -av --exclude '*.git' --exclude '.process' --exclude '*pyc' --exclude '__pycache__' --exclude 'video.bk' --exclude "*.hdf5" --exclude "*.pkl" --exclude "*.pkts" --exclude "*.partition" . stack@192.168.1.196:/home/stack/code/simulation/;
rsync -av --exclude '*.git' --exclude '.process'  --exclude '*pyc' --exclude '__pycache__' --exclude 'video.bk' --exclude "*.hdf5" --exclude "*.pkl" --exclude "*.pkts" --exclude "*.partition" . stack@192.168.1.36:/home/stack/code/simulation/;

EOF

