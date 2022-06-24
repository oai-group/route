#!/bin/bash


echo "1" >/proc/sys/net/ipv4/ip_forward

iptables -P INPUT ACCEPT
iptables -P FORWARD ACCEPT
iptables -P OUTPUT ACCEPT
iptables -t nat -F
iptables -t mangle -F
iptables -F
iptables -X


# setup port forwarding

iptables -A PREROUTING -t nat -i eno1 -p tcp --dport 6060 -j DNAT --to 10.1.0.1:6060
iptables -A FORWARD -p tcp -d 10.1.0.1 --dport 6060 -j ACCEPT

ip netns del h1
ip netns del h0

ip link del h0-eth0
ip link del h0-nat
ip link del h1-eth0


ip netns add h0
ip netns add h1

ip link add h0-eth0 type veth peer name h1-eth0
ip link add h0-nat type veth peer name nat-h0


ip link set h0-eth0 address 00:00:00:00:00:01
ip link set h1-eth0 address 00:00:00:00:00:02

ip link set dev h0-eth0 netns h0
ip link set dev h1-eth0 netns h1
ip link set dev h0-nat netns h0


ip link set dev nat-h0 up
ip addr add 10.1.0.2/16 dev nat-h0

ip netns exec h0 ip link set dev h0-eth0 up
ip netns exec h0 ip link set dev h0-nat up

ip netns exec h0 ip link set dev lo up
ip netns exec h1 ip link set dev lo up
ip netns exec h1 ip link set dev h1-eth0 up

ip netns exec h0 ip addr add 10.0.0.1/16 dev h0-eth0
ip netns exec h1 ip addr add 10.0.0.2/16 dev h1-eth0
ip netns exec h0 ip addr add 10.1.0.1/16 dev h0-nat

ip netns exec h0 ip route add default via 10.1.0.2

iptables -A FORWARD -o nat-h0 -i eno1 -j ACCEPT
iptables -A FORWARD -o eno1 -i nat-h0 -j ACCEPT
iptables -t nat -A POSTROUTING -s 10.1.0.0/16 -o eno1 -j MASQUERADE

#ip netns exec h0 tc qdisc add dev h0-eth0 root handle 5:0 hfsc default 1
#ip netns exec h0 tc class add dev h0-eth0 parent 5:0 classid 5:1 hfsc sc rate 500Mbit ul rate 500Mbit
#ip netns exec h0 tc qdisc add dev h0-eth0 parent 5:1 handle 10: netem delay 50ms

