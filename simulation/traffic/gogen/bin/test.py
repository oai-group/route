import os
params = "--id {} " \
		         "--pkts {} " \
		         "--mtu {} " \
		         "--int {} " \
		         "--forcetarget " \
		         "--target {} " \
		         "--ftype {} " \
		         "--workers {}".format(
			1,
			"/home/sdn/simulation/traffic/gogen/pkts/video",
			1420,
			"eno2",
			"10.0.0.5",
			0,
			1,
		)

commands = "{} {}".format("/home/sdn/simulation/traffic/gogen/bin/gogen", params)
os.system(commands)