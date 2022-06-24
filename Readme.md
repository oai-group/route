# smartfwd

- smartfwd/src/main/java/constants/App.java
  - 31 : public static final String ALG_CLASSIFIER_IP="algorithm_instance";
  - 34 : public static final String DEFAULT_ROUTING_IP="algorithm_instance";
  - 37 : public static final String OPT_ROUTING_IP="algorithm_instance";
  - 42 : public static final String C_DOWN_ROUTING_IP="algorithm_instance";

> 修改为RoutingAlgorithm-1.0-SNAPSHOT.jar运行的ip,通过脚本部署则不需要修改,会通过容器名algorithm_instance进行通信

- smartfwd/src/main/java/AppComponent.java
  - 294-305 ： 192.168.2.101/32 192.168.2.103/32 192.168.2.104/32
  - 516-523 ： 192.168.2.101/32 192.168.2.103/32 192.168.2.104/32

> start == 0表示的是交换机s0 : 192.168.2.101/32表示挂载在s0上的物理网卡与之网线直连的另一台主机的物理网卡配置的ip地址
>
> start == 4表示的是交换机s4 : 192.168.2.103/32表示挂载在s4上的物理网卡与之网线直连的另一台主机的物理网卡配置的ip地址
>
> start == 11表示的是交换机s11 : 192.168.2.104/32表示挂载在s11上的物理网卡与之网线直连的另一台主机的物理网卡配置的ip地址
>
> 此处需要注意的是安装的路由是0-18的,即上述代码所在交换机都可以接入enb,epc所连接的网卡必须接入到s18
>
> 物理网卡挂载ovs有simulation模块控制

# Simulation

- static/oai.config.json

  - 43 :  "workers_intf": ["eno1"]

  - 95 :  "controller": "192.168.1.106:6666"

  - 98-100 :  "enable_delay_constraint": 0,

     				  "enable_rate_constraint": 1,
	
     				  "enable_loss_constraint": 0,

> 43 :  nat网桥出口网卡,设置为实际可以上网的物理网卡（不设置也行,只是为了namespace能访问网络）
>
> 95:   onos控制器ip地址,由于用容器启动,ip不能写127.0.0.1,由于启动是容器映射端口为6666:6653,所以端口为6666
>
> 98-100 :  控制topobuilder中带宽时延丢包率设置是否生效 1生效 0不生效

- telemetry/sniffer.delay.py
  - 40 :  rip: str = "192.168.1.106", rport: int = 6379
- telemetry/sniffer.loss.py
  - 40 :  rip: str = "192.168.1.106", rport: int = 6379

> 设置redis数据库的ip和端口,容器部署redis但127.0.0.1和localhost不能访问,请写成具体的ip地址

- simulation/topo/distributed/topobuilder.py
  - 496-500/766 : add_hosts_to_switches...self.hostids.append(hostid)/self._do_find_host()
  - 870-877 :  attach_interface_to_sw("s0", "enp24s0f0"); os.system("ifconfig enp24s0f0 up")
  - 599-604 :  rate, delay, loss, _ = [50, 10, 10, 0]...

> 496-500/766 :  若不需要Host,可以注释这些代码
>
> 870-877 :   attach_interface_to_sw("s0", "enp24s0f0")
>
> ​    			   os.system("ifconfig enp24s0f0 up")
>
> ​					   attach_interface_to_sw("s4", "enp24s0f1")
>
> ​					   os.system("ifconfig enp24s0f1 up")
>
> ​					   attach_interface_to_sw("s11", "enp24s0f2")
>
> ​					   os.system("ifconfig enp24s0f2 up")
>
> ​    				 attach_interface_to_sw("s18", "enp24s0f3")
>
> ​    				 os.system("ifconfig enp24s0f3 up")
>
> 将enp24s0f0挂载到ovs bridge s0上
>
> Note :  一定要将epc的网卡绑定在s18上！！！
>
> 599-604 :  rate, delay, loss, _ = [50, 10, 10, 0]
>
> ​				    rate = rate if int(self.config["enable_rate_constraint"]) == 1 else None
>
> ​         	delay = delay if int(self.config["enable_delay_constraint"]) == 1 else None
>
> ​           loss = loss if int(self.config["enable_loss_constraint"]) == 1 else None
>
> rate, delay, loss, _ = [50, 10, 10, 0] :  表示设置带宽为50Mbps,时延为10ms,单向丢包率为10%;注意需要配合enable_rate_constraint，enable_delay_constraint，enable_loss_constraint控制是否设置
>
> 上述代码为全局设置，若要单独设置某条链路的丢包率可以将 loss = loss if int(self.config["enable_loss_constraint"]) == 1 else None改为loss = loss if (sa_id == 0 and sb_id == 1) else None，表示设置s0->s1和s1->s0的链路设置丢包率为10%；sa_id必须小于sb_id 

- 控制台说明
  - 0. set up local switch
  - 1. set up first topo
  - 2. start traffic actor
  - 3. stop traffic actor
  - 4. quit

> 背景流量生成可以直接在控制台程序中输入2， 开始注入背景流量
>
> 也可以在控制台程序中输入 1-2:30 使“1-2”这一条链路注入大量流量（30表示30个进程，一个进程大概能产生2Mbit/s的流量