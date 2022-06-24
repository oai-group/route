package fpsdk

import "fmt"

type FlowSpecifier struct {
	Sip string `json:"src_ip"`
	Dip string `json:"dst_ip"`
	Sport int `json:"src_port"`
	Dport int `json:"dst_port"`
	Protocol string `json:"proto"`
}


type PortPair struct {
	Sport int `json:"src_port"`
	Dport int `json:"dst_port"`
}

func (pp *PortPair)HashKey() int{
	return 0
}

func (pp *PortPair)String() string  {
	return fmt.Sprintf("%d-%d",pp.Sport,pp.Dport);
}

func (fp *FlowSpecifier)String() string {
	return fmt.Sprintf("%s-%s-%d-%d-%s",fp.Sip,fp.Dip,fp.Sport,fp.Dport,fp.Protocol)
}