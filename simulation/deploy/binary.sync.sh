#!/bin/bash

rsync -av  ./traffic/gogen/bin/* stack@192.168.1.128:/home/stack/code/simulation/traffic/gogen/bin/
rsync  -av ./traffic/gogen/bin/* stack@192.168.1.34:/home/stack/code/simulation/traffic/gogen/bin/
rsync -av  ./traffic/gogen/bin/* stack@192.168.1.110:/home/stack/code/simulation/traffic/gogen/bin/
rsync -av -r ./traffic/gogen/bin/* stack@192.168.1.90:/home/stack/code/simulation/traffic/gogen/bin/
rsync -av -r ./traffic/gogen/bin/* stack@192.168.1.180:/home/stack/code/simulation/traffic/gogen/bin/
rsync -av -r ./traffic/gogen/bin/* stack@192.168.1.132:/home/stack/code/simulation/traffic/gogen/bin/
rsync -av -r ./traffic/gogen/bin/* stack@192.168.1.196:/home/stack/code/simulation/traffic/gogen/bin/
