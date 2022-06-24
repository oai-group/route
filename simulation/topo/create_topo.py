from utils.file_utils import save_pkl, save_json
from utils.log_utils import info, debug
from path_utils import get_prj_root
import os

topo = [[[-1, -1, -1, -1] for _ in range(19)] for _ in range(19)]
link = [100, 0, 0, 0]

def connect(i, j):
	global topo
	topo[i][j] = link


topoInput="\
0 : 1 5  \n\
1 : 0 2 \n\
2 : 1 3 7 \n\
3 : 2 4 6 \n\
4 : 3 5 \n\
5 : 0 4  \n\
6 : 3 7 11 \n\
7 : 2 6 8 \n\
8 : 7 9 \n\
9 : 8 10 12 13 14 \n\
10 : 9 11 13 14 15 \n\
11 : 6 10 \n\
12 : 9 13 17 \n\
13 : 9 10 12 14 17 \n\
14 : 9 10 13 15 16 17 \n\
15 : 10 14 16 \n\
16 : 14 15 18 \n\
17 : 12 13 14 18 \n\
18 : 16 17\
"

topoList = topoInput.split("\n")

for line in topoList:
    tmp=line.split(":")
    i=tmp[0]
    d=tmp[1].split()
    for j in d:
        connect(int(i), int(j))
    
static_dir = os.path.join(get_prj_root(), "static")
# save_json(os.path.join(static_dir,"topo.json"),{"topo":topo})

res = []
for _ in range(44):
	res.append({
		"topo": topo,
		"duration": 0
	})

save_pkl(os.path.join(static_dir, "oai.pkl"), res)
