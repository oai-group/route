package fpsdk

import "github.com/willf/bloom"

//bloom filter
//https://llimllib.github.io/bloomfilter-tutorial/#:~:text=A%20Bloom%20filter%20is%20a,may%20be%20in%20the%20set.

type Memory struct {
	id uint64
	portPairsInStr map[string]struct{}
	//set
	portPairs map[PortPair]struct{}
}


type Core struct {
	ids map[uint64]struct{}
	filters map[uint64]*bloom.BloomFilter
	memories map[uint64] *Memory
}

func newDefaultBloomFilter() *bloom.BloomFilter {
	return bloom.New(1000*100000,10)
}

var core *Core
func init()  {
	core=&Core{
		filters:  make(map[uint64]*bloom.BloomFilter),
		memories: make(map[uint64]*Memory),
	}
}

func (c *Core)NewPairPort(id uint64) *PortPair{
	res:=&PortPair{
		Sport: 0,
		Dport: 0,
	}
	//find and store
	return res
}

func (c *Core)ReturnPortPair(id uint64,ports *PortPair){

}
