#!/bin/bash

echo 1 > /proc/sys/net/ipv4/ip_forward

ip netns del test
ip netns add test
ip link del test-nat
ip link del nat-test
ip link add test-nat type veth peer name nat-test
ip link set test-nat netns test

iptables -P INPUT ACCEPT
iptables -P FORWARD ACCEPT
iptables -P OUTPUT ACCEPT
iptables -t nat -F
iptables -t mangle -F
iptables -F
iptables -X

ip netns exec test ifconfig test-nat 172.168.1.90/24 up
ip netns exec test ifconfig lo up


ifconfig nat-test 172.168.1.91/24 up
ip netns exec test ip route add default via 172.168.1.91
iptables -A FORWARD -o nat-test -i enp24s0f0 -j ACCEPT
iptables -A FORWARD -o enp24s0f0 -i nat-test -j ACCEPT

iptables -t nat -A POSTROUTING -s 172.168.1.0/24 -o enp24s0f0  -j MASQUERADE

