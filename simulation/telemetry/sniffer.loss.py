from argparse import PARSER
from itertools import count
from time import sleep

from utils.process_utils import start_new_thread_and_run
from scapy.all import *
from utils.time_utils import now_in_milli
from path_utils import get_prj_root
from scapy.layers.inet import IP, UDP
from scapy.layers.l2 import Ether, Dot1Q
from utils.file_utils import file_exsit
from path_utils import get_prj_root
import os
from scapy import config
from telemetry.base_telemeter import BaseTelemeter
from typing import Tuple, List, Dict
from scapy.all import *

conf.use_pcap = True
# scapy.config.Conf.use_pcap=True
from utils.log_utils import debug, info, err
import threading
import argparse
from utils.file_utils import load_json
from path_utils import get_prj_root
from utils.file_utils import load_pkl, save_pkl
import json

from telemetry.store import Store

paths = None
links = None
true_topo = None
vlan_to_link = {}


# todo store stats in redis
class Sniffer:
	def __init__(self, count: int, intf: str, filter: str, link_to_vlan_fn: str,
	             rip: str = "192.168.1.106", rport: int = 6379) -> None:
		self.pkt_count = count
		self.intf = intf
		self.filter = filter
		self.each_cache = []
		self.cache=[]
		self.edge_port = {}  # vlan_id ->linkkk
		self.switch_count = {}
		self.loss_count = 0
		self.link_to_vlan_fn: str = link_to_vlan_fn

		self.store=Store(rip,rport,7)

	def __load_topo(self):
		self.edge_port = load_pkl(self.link_to_vlan_fn)
		# for  link in self.edge_port.keys():
		# 	if self.edge_port[link]==567:
		# 		debug("567---{}".format(link))
	# port[(0, self.monitor)]  (1, 1)
	# return port

	def __calculate_rtt_and_store(self):
		self.loss_count = 0
		switch_msg = {}
		link_rtt = {}
		# switch_msg: ip->port->time
		for pkt in self.each_cache:
			t = pkt.time
			port = int(pkt[Dot1Q].vlan)
			src = str(pkt[IP].src)

			if src in switch_msg.keys():
				# uncomment
				pass
				# if port in switch_msg[src].keys():
				# 	err("error, {} already in items".format(port))
				# 	# return
				# switch_msg[src][port] = t
			else:
				# uncomment
				pass
				# temp = {}
				# temp[port] = t
				# switch_msg[src] = temp
			if src in self.switch_count.keys():
				if port in self.switch_count[src].keys():
					self.switch_count[src][port] += 1
				else:
					self.switch_count[src][port] = 1
			else:
				temp = {}
				temp[port] = 1
				self.switch_count[src] = temp
		# debug("fuck")
		# for u, v in links:
		# 	t1 = None
		# 	t2 = None
		# 	x, y, z = self.find_port(u, v)
		# 	try:
		# 		t1 = switch_msg["172.18.1.{}".format(z)][self.edge_port[(z, y)]]
		# 		t2 = switch_msg["172.18.1.{}".format(y)][self.edge_port[(y, x)]]
		# 		link_rtt[(u, v)] = abs(t1 - t2) * 1000
		# 	except Exception as e:
		# 		if "172.18.1.{}".format(z) not in switch_msg.keys():
		# 			self.loss_count += 1
		# 			if "172.18.1.{}".format(y) not in switch_msg.keys():
		# 				self.loss_count += 1
		# 		self.loss_count += 1
		# 		err("exception:{}".format(e))
		# # debug("link {}'s rtt => {}  ----  1 ".format((u, v), abs(t1 - t2) * 1000))
		# debug("link_rtt:{}".format(link_rtt))
		# debug("loss count:{}".format(self.loss_count))
		# ma = -1
		# ma_link = None
		# for u, v in link_rtt.keys():
		# 	true_delay = int(true_topo[u - 1][v - 1][1]) * 2
		# 	if ma < abs(true_delay - link_rtt[(u, v)]) / true_delay:
		# 		ma_link = (u, v)
		# 		ma = abs(true_delay - link_rtt[(u, v)]) / true_delay

		# debug("Delay estimation maximum error ratio {} on link {}".format(ma, ma_link))
		# fixed link stats
		# fixed_link_stats = {}
		# for u, v in link_rtt.keys():
		# 	fixed_link_stats[(u - 1, v - 1)] = link_rtt[(u, v)]
		# 	self.store.write_delay((u-1,v-1),fixed_link_stats[(u-1,v-1)])
		# 	fixed_link_stats[(v - 1, u - 1)] = link_rtt[(u, v)]
			# self.store.write_delay((v-1,u-1),fixed_link_stats[(u-1,v-1)])

	# save_pkl("/tmp/telemetry.link.pkl", fixed_link_stats)

	def __calculate_loss(self):
		loss = {}
		for u, v in links:
			x, y, z = self.find_port(u, v)
			try:
				count1 = self.switch_count["172.18.1.{}".format(z)][self.edge_port[(z, y)]]
				count2 = self.switch_count["172.18.1.{}".format(y)][self.edge_port[(y, x)]]
				loss[(u, v)] = (count2 - count1) / count2
				# a=loss[(u,v)]
				# a=1-(1-a)**0.5
				# loss[(u,v)]=a
			except Exception as e:
				err("exception:{},cannot calculate loss on link {}".format(e, (y, z)))
			for msg in loss.items():
				pass
				# debug("packet loss:{}".format(msg))

		fixed_link_stats = {}
		for u, v in links:
			fixed_link_stats[(u - 1, v - 1)] = loss[(u, v)]
			if loss[(u,v)]>=0:
				self.store.write_loss((u-1,v-1),loss[(u,v)])
			fixed_link_stats[(v - 1, u - 1)] = loss[(u, v)]
		# self.store.write_loss((v-1,u-1),loss[(u,v)])
		
		debug("packet loss:{}".format(loss))

	def find_port(self, u, v):
		for path in paths:
			for i in range(1, len(path) - 1):
				if path[i] == u and path[i + 1] == v:
					return path[i - 1], u, v
				elif path[i] == v and path[i + 1] == u:
					return path[i - 1], v, u
		print(u, v)

	def __send_telemetry_packet_and_listen(self) -> Tuple[int, str]:
		sniffer_lock = threading.Lock()
		current_count = 0
		sniffer_started = False
		sniffer_stopped = False
		recv_vlan = []
		return_link = []
		n=2000

		def sniffer_started_cbk():
			# nonlocal sniffer_lock
			# sniffer_lock.acquire()
			nonlocal sniffer_started
			sniffer_started = True

			debug("AsyncSniffer started and lock acquired")

		def handle_pkt(pkt):
			nonlocal current_count
			# print("fuck")
			pkt.sprintf("{IP:%IP.src%},{Port:%Dot1Q.vlan%}")

			recv_vlan.append(pkt[Dot1Q].vlan)
			self.each_cache.append(pkt)
			self.cache.append(pkt)
			current_count += 1
			if current_count==n*284:
				debug("ok done")
			# debug("receive {} pkts".format(current_count))
			t = pkt.time
			port = int(pkt[Dot1Q].vlan)
			src = str(pkt[IP].src)

			# nonlocal current_count
			# current_count += 1
			# debug("received {} pkts src {} port {}".format(current_count, src, port))

		# if current_count == self.pkt_count:
		# 	debug("Received all pkts so far,release the lock")
			# nonlocal sniffer
		# 	# sniffer.stop()
		# 	# sniffer_lock.release()

		def stop_sniffer():
			nonlocal sniffer, sniffer_stopped
			sniffer.stop()
			sniffer_stopped = True

		sniffer = AsyncSniffer(iface=self.intf, prn=handle_pkt,
		                       started_callback=sniffer_started_cbk, filter=self.filter)
		start_new_thread_and_run(func=sniffer.start, args=())

		# wait for sniffer start
		# sleep(5)
		while not sniffer_started:
			sleep(0.1)
		# sniffer started,now send pkt
		debug("Sniffer started,now send pkt")
		p = Ether() / Dot1Q(vlan=3) / IP(src="172.18.0.1", dst="172.18.0.2") / \
		    UDP(dport=8888, sport=1500) / Raw(load="1234")
		# m=n
		# while m>0:
		# 	m-=1
		sendp(p, iface=self.intf,count=n)

		debug("Telemetry pkt sent,now wait for all pkts received")
		t = threading.Timer(10, stop_sniffer)
		t.start()

		# sniffer_lock.acquire()
		while not sniffer_stopped:
			sleep(0.1)
		debug("timer timeout,return")
		# debug("All returned pkts received")
		# loss_link = []
		# last_link = []
		# paths = load_json(os.path.join(get_prj_root(), "static/telemetry.paths.json"))["paths"]
		# for path in paths:
		# 	if (path[-1], path[-2]) not in last_link:
		# 		last_link.append((path[-1], path[-2]))
		# 	for i in range(1, len(path)):
		# 		if (path[i], path[i - 1]) not in return_link:
		# 			return_link.append((path[i], path[i - 1]))
		# for link in return_link:
		# 	if self.edge_port[link] in recv_vlan:
		# 		continue
		# 	loss_link.append(link)
		# print("return_link:", return_link, len(return_link))
		# print("last_link:", last_link, len(last_link))
		# print("loss_link:", loss_link)
		# print(len(loss_link))
		return 0, ""

	def start(self):
		self.__load_topo()
		# self.__send_telemetry_packet_and_listen()
		# self.__calculate_rtt_and_store()
		# self.__calculate_rtt()
		while True:
			debug("new telemetry")
			# n = 200
			# while n > 0:
			# 	self.loss_count = 0
			# 	self.__send_telemetry_packet_and_listen()
			# 	self.__calculate_rtt_and_store()
			# 	self.cache = []
			# 	# self.loss_count=0
			# 	n -= 1
			# 	debug("{}th turn done".format(200 - n))
			# 	break
			# n = 200
			# for i in range(100):
			self.__send_telemetry_packet_and_listen()
			self.__calculate_rtt_and_store()
			debug("now calculating loss")
			self.__calculate_loss()
			self.each_cache=[]
			# break


if __name__ == "__main__":
	parser = argparse.ArgumentParser()
	parser.add_argument(
		"--intf", type=str, help="Interface to send and listen", default="h9-eth0")
	parser.add_argument("--filter", type=str,
	                    help="BPF filter", default="udp port 8888")
	parser.add_argument("--count", type=int,
	                    help="Number of packets to received", default=284)

	parser.add_argument("--links",
	                    type=str,
	                    help="Pickle file to store links",
	                    default=os.path.join(get_prj_root(),
	                                         "static/telemetry.links.pkl"))

	parser.add_argument("--paths",
	                    type=str,
	                    help="JSON file to store paths information",
	                    default=os.path.join(get_prj_root(), "static/telemetry.paths.json")
	                    )

	# parser.add_argument("--topo",
	#                     type=str,
	#                     help="Json file to store topo information",
	#                     default=os.path.join(get_prj_root(), "telemetry/topo0.json"))

	default_link_to_vlan_fn = os.path.join(get_prj_root(), "static/telemetry.link_to_vlan.pkl")
	parser.add_argument("--link_to_vlan",
	                    type=str,
	                    help="Var pickle file path",
	                    default=default_link_to_vlan_fn,
	                    )

	args = parser.parse_args()
	if not file_exsit(args.link_to_vlan):
		err("Vars file not exists {}".format(args.link_to_vlan))
		exit(-1)
	if not file_exsit(args.paths):
		err("Calculated paths json file not found {}".format(args.paths))
		exit(-1)

	sniffer = Sniffer(count=int(args.count),
	                  intf=args.intf,
	                  filter=args.filter,
	                  link_to_vlan_fn=args.link_to_vlan)

	# true_topo = load_pkl(os.path.join(get_prj_root(), "static/military_overall.pkl"))[0]["topo"]
	# true_topo = load_json(os.path.join(get_prj_root(), 'static/topo.json'))["topo"]
	# vlan_to_link_fn = os.path.join(get_prj_root(), "static/telemetry.vlan_to_link.pkl")
	# vlan_to_link = load_pkl(vlan_to_link_fn)

	## warn! fix link by add 1
	links = load_pkl(args.links)
	paths = load_json(args.paths)["paths"]
	sniffer.start()
