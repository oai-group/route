#!/usr/bin/env bash

#ssh-keygen -f "/home/stack/.ssh/known_hosts" -R "[localhost]:8101"
docker stop onos22
docker rm onos22

#docker stop redis_instance
#docker rm redis_instance

# service docker restart
rm -rf /tmp/data
mkdir /tmp/data
cp /home/yx/telemetry.flow.json /tmp/data
systemctl restart docker

docker run -t -d -p 8181:8181 -p 8101:8101 -p 6666:6653 -p 1050:1050 -p 1051:1051 -p 5005:5005 -p 830:830 -p 7896:7896 -p 1054:1054 -p 1060:1060 -v /tmp/data:/data --name onos22 onosproject/onos

echo "Sleep for 20 seconds"
sleep 20


ssh-keygen -f "/root/.ssh/known_hosts" -R "[localhost]:8101"

#sshpass -p "karaf" ssh localhost "app activate org.onosproject.fwd"
#sshpass -p "karaf" ssh localhost "app activate org.onosproject.openflow"
#sshpass -p "karaf" ssh localhost "app activate org.onosproject.proxyarp"
#docker restart mysql_instance
docker restart redis_instance
echo "Onos docker setup done"
