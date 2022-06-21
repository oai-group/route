package algorithm.graph;

import algorithm.graph.abstraction.Algorithm;
import algorithm.graph.abstraction.BaseVertex;
import algorithm.graph.utils.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;

public class oldLS implements Algorithm {

    protected Map<Pair<Integer, Integer>, Demand> demandMap = new HashMap<Pair<Integer, Integer>, Demand>();
    protected Map<Pair<Integer, Integer>, Edge> edgeMap = new HashMap<Pair<Integer, Integer>, Edge>();
    public List<List<Path>> routingList = new ArrayList<>();
    public Integer nodeNum = 19;
    private String flowType = "";


    public List<List<Path>> staticRoutingList = new ArrayList<>();
    //    public List<List<Path>> routingList = new ArrayList<>();
    // all shortest paths of one topology , a list with size (nodeNum*nodeNum)*10, 2d ,i==j is null
    public List<List<Path>> ASPs = new ArrayList<>();
    public List<Double> demandList = new ArrayList<Double>();//nodeNum*(nodeNum -1)  1d, i==j not exist


    private PriorityQueue<Edge> edgePriorityQueue = new PriorityQueue<Edge>();


    public void clear() {
        edgePriorityQueue.clear();
    }

    public void createEdgeQ() throws IOException, ClassNotFoundException {
        clear();
        loadFile();

        // 2.3 calculate the weight of edge
        Double ww = 0d;
        Double www = 0d;

        int x = 0;
        for (int i = 0; i < nodeNum; i++) {
            for (int j = 0; j < nodeNum; j++) {
                if (i == j) {
                    continue;
                }

//                Path routePath = routingList.get(i).get(j);
                Path routePath = routingList.get(i).get(j);
//                Path routePath = new Path();
                // 建立d实例


                Demand d = new Demand(i, j, demandList.get(x));
//                Demand d = new Demand(i,j,demandList.get(x));
//                d.setP(routePath);
                d.oload = d.load;
//                d.oload = demandList.get(x)+routePath.getChangeTimes()*changeTimesWight;
                d.setP(routePath);

                d.setOP(routePath);

                int index = i * nodeNum + j;
                d.setAllPathsSet(ASPs.get(index));
//                System.out.println(i);
//                System.out.println(j);
//                System.out.println(ASPs.get(index));
                demandMap.put(new Pair<>(i, j), d);
                List<BaseVertex> vertexList = routePath.getVertexList();

//                System.out.println(routePath);
                for (int k = 0; k < vertexList.size() - 1; k++) {
                    // vp vertex pair
                    Pair<Integer, Integer> vp = new Pair<>(vertexList.get(k).getId(), vertexList.get(k + 1).getId());

                    edgeMap.get(vp).addDemand(d);
                }
                x++;
            }
        }
//        System.out.println(edgeSet);


        // edgeSet to edgeQueue
        for (Map.Entry<Pair<Integer, Integer>, Edge> entry : edgeMap.entrySet()) {
//            Pair<Integer, Integer> key = entry.getKey();
            Edge e = (Edge) entry.getValue();

            www += e.load;
            edgePriorityQueue.add(e);
        }
        System.out.println(edgePriorityQueue);
    }

    public void next() {
        if (edgePriorityQueue.isEmpty()) {
            return;
        }

        Edge e = edgePriorityQueue.peek();
//        System.out.println(e);

        Demand d = e.chooseDemand();
        if (d == null) {
            return;
        }

//        Path curPath = d.p;
        Path curPath = d.p;
//        Set<Path> allPathsSet = d.allPathsSet;
        Set<Path> allPathsSet = d.allPathsSet;
        double MaxLoad = edgePriorityQueue.peek().load;
        double MinLoad = 0d;
        MinLoad += MaxLoad;
        Path minPath = curPath;
        Path prePath = curPath;
        for (Path path : allPathsSet) {
            prePath = path;
            if (!prePath.equals(curPath)) {
                changePathOfDemand(curPath, prePath, e, d);
//                if(!edgePriorityQueue.isEmpty()&& edgePriorityQueue.peek().load<MinLoad){
                if (edgePriorityQueue.peek().load < MinLoad) {
                    MinLoad = edgePriorityQueue.peek().load;
                    minPath = prePath;
                }
            }
            curPath = prePath;
        }
//        changePathOfDemand(prePath,d.p,e,d);
        changePathOfDemand(prePath, d.p, e, d);
//        d.p = minPath;
        prePath = d.p;
        d.p = minPath;
        if (prePath == minPath) {
            return;
        }
        changePathOfDemand(prePath, minPath, e, d);
    }

    //    public void changePathOfDemand(Path curPath,Path prePath,Edge e,Demand d){
    public void changePathOfDemand(Path curPath, Path prePath, Edge e, Demand d) {
        Set<Edge> edgeSet = new HashSet<>();
//        Path curPath = d.p;
        List<BaseVertex> vertexList = curPath.getVertexList();
        for (int k = 0; k < vertexList.size() - 1; k++) {
            // vp vertex pair
            Pair<Integer, Integer> vp = new Pair<>(vertexList.get(k).getId(), vertexList.get(k + 1).getId());
            edgeSet.add(edgeMap.get(vp));
        }
        Set<Edge> newEdgeSet = new HashSet<>();
        List<BaseVertex> newVertexList = prePath.getVertexList();
        for (int k = 0; k < newVertexList.size() - 1; k++) {
            // vp vertex pair
            Pair<Integer, Integer> newVP = new Pair<>(newVertexList.get(k).getId(), newVertexList.get(k + 1).getId());
            newEdgeSet.add(edgeMap.get(newVP));
        }
//        if(newEdgeSet.contains(e)) return; //最大边仍然在则无意义
        // 删除当前路径边
        for (Edge edge : edgeSet) {
            edgePriorityQueue.remove(edge);
            edge.removeDemand(d);
            edgePriorityQueue.add(edge);
        }
        // 改变d的load
//        d.load = d.load - curPath.getChangeTimes()*changeTimesWight + prePath.getChangeTimes()*changeTimesWight;
        // 加入下一个路径边
        for (Edge edge : newEdgeSet) {
//            if(!edgeSet.contains(edge)) //not deleted
//                edgePriorityQueue.remove(edge);
            edgePriorityQueue.remove(edge);

            edge.addDemand(d);
            edgePriorityQueue.add(edge);
        }
//        System.out.println(edgePriorityQueue);

    }


    public void optimize() {
        long startTime = System.currentTimeMillis();
        int count = 0;
        int timesCount = 0;
        double lastLoad = 0d;
        while (true) {
            next();
            timesCount++;
//            System.out.println(timesCount);
//            if(timesCount>6000){
//                next();
//            }
            long endTime = System.currentTimeMillis();
//            System.out.println("time:"+(endTime-startTime));

            double load = edgePriorityQueue.peek().load;
//            System.out.println(load);
//            if(load==lastLoad) count++;
            if (Math.abs(load - lastLoad) <= Math.pow(10, -8)) {
                count++;
            } else {
                count = 0;
            }
            lastLoad = load;

            if (count > 1000) {
//                System.out.println("time:"+(endTime-startTime));
//                System.out.println("load:" + edgePriorityQueue.peek().load);
//                System.out.println("timesCount:" + timesCount);

                System.out.println(edgePriorityQueue);
                break;
            }

//            System.out.println("load:" + edgePriorityQueue.peek().load);
        }
    }


    public void resetEdgeQ() {
        edgePriorityQueue.clear();
        for (Map.Entry<Pair<Integer, Integer>, Edge> entry : edgeMap.entrySet()) {
            Edge e = (Edge) entry.getValue();
            e.reset();
        }
        int x = 0;
        for (int i = 0; i < nodeNum; i++) {
            for (int j = 0; j < nodeNum; j++) {
                if (i == j) {
                    continue;
                }

                Path routePath = routingList.get(i).get(j);
//                Path routePath = new Path();
                // 读取d实例
                Demand d = demandMap.get(new Pair<>(i, j));
                d.setP(routePath);
                d.setOP(routePath);
                d.load = d.oload;

                List<BaseVertex> vertexList = routePath.getVertexList();

                for (int k = 0; k < vertexList.size() - 1; k++) {
                    // vp vertex pair
                    Pair<Integer, Integer> vp = new Pair<>(vertexList.get(k).getId(), vertexList.get(k + 1).getId());

                    edgeMap.get(vp).addDemand(d);
                }
                x++;
            }
        }
        for (Map.Entry<Pair<Integer, Integer>, Edge> entry : edgeMap.entrySet()) {
            Edge e = (Edge) entry.getValue();
            edgePriorityQueue.add(e);
        }
//        System.out.println(edgePriorityQueue.peek().load);
//        System.out.println(edgePriorityQueue);

    }


    /**
     * 更新当前路由表
     *
     * @param Routing
     */
    public void changeCurRouting(List<List<Path>> Routing) {
        this.routingList = Routing;
    }

    /**
     * 直接用列表更新需求矩阵
     *
     * @param demands
     */
    public void resetEdgeQWithNewDemand(List<Double> demands) {
        this.demandList = demands;

        edgePriorityQueue.clear();
        for (Map.Entry<Pair<Integer, Integer>, Edge> entry : edgeMap.entrySet()) {
            Edge e = (Edge) entry.getValue();
            e.reset();
        }
        int x = 0;
        for (int i = 0; i < nodeNum; i++) {
            for (int j = 0; j < nodeNum; j++) {
                if (i == j) {
                    continue;
                }

                Path routePath = routingList.get(i).get(j);
//                Path routePath = new Path();
                // 读取d实例
                Demand d = demandMap.get(new Pair<>(i, j));
                d.setP(routePath);
                d.setOP(routePath);
                d.load = demandList.get(x);
                d.oload = demandList.get(x);
                List<BaseVertex> vertexList = routePath.getVertexList();

                for (int k = 0; k < vertexList.size() - 1; k++) {
                    // vp vertex pair
                    Pair<Integer, Integer> vp = new Pair<>(vertexList.get(k).getId(), vertexList.get(k + 1).getId());
                    edgeMap.get(vp).addDemand(d);
                }
                x++;
            }
        }
        for (Map.Entry<Pair<Integer, Integer>, Edge> entry : edgeMap.entrySet()) {
            Edge e = (Edge) entry.getValue();
            edgePriorityQueue.add(e);
        }

    }

    /**
     * 用file更新demand
     *
     * @param newFileEnd
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void resetEdgeQWithNewDemand(String newFileEnd) throws IOException, ClassNotFoundException {
        String demandFile = "data/demand/test_0";
        demandList.clear();
//        System.out.println(demandFile);
        // 2.1 read demand data
//        List<Double> demandList = new ArrayList<Double>();
        try {
            FileReader input = new FileReader(demandFile);
            BufferedReader bufRead = new BufferedReader(input);
            String line;    // String that holds current file line

            line = bufRead.readLine();
            while (line != null) {
                // skip the empty line
                if (line.trim().equals("")) {
                    line = bufRead.readLine();
                    continue;
                }

                String[] demandStr = line.trim().split("\\s");

                for (String s : demandStr) {
                    demandList.add(Double.parseDouble(s));
                }
                line = bufRead.readLine();
//                System.out.println(demandList);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        edgePriorityQueue.clear();
        for (Map.Entry<Pair<Integer, Integer>, Edge> entry : edgeMap.entrySet()) {
            Edge e = (Edge) entry.getValue();
            e.reset();
        }
        int x = 0;
        for (int i = 0; i < nodeNum; i++) {
            for (int j = 0; j < nodeNum; j++) {
                if (i == j) {
                    continue;
                }

                Path routePath = routingList.get(i).get(j);
//                Path routePath = new Path();
                // 读取d实例
                Demand d = demandMap.get(new Pair<>(i, j));
                d.setP(routePath);
                d.setOP(routePath);
                d.load = demandList.get(x);
                d.oload = demandList.get(x);
                List<BaseVertex> vertexList = routePath.getVertexList();

                for (int k = 0; k < vertexList.size() - 1; k++) {
                    // vp vertex pair
                    Pair<Integer, Integer> vp = new Pair<>(vertexList.get(k).getId(), vertexList.get(k + 1).getId());

                    edgeMap.get(vp).addDemand(d);
                }
                x++;
            }
        }
        for (Map.Entry<Pair<Integer, Integer>, Edge> entry : edgeMap.entrySet()) {
            Edge e = (Edge) entry.getValue();
            edgePriorityQueue.add(e);
        }
    }


    /**
     * 直接用list更新demand
     *
     * @param newDemandList
     */
    public void resetEdgeQWithNewDemandList(List<Double> newDemandList) {
//        this.ASPs = Routing;
        this.demandList = newDemandList;
        edgePriorityQueue.clear();
        for (Map.Entry<Pair<Integer, Integer>, Edge> entry : edgeMap.entrySet()) {
            Edge e = (Edge) entry.getValue();
            e.reset();
        }
        int x = 0;
        for (int i = 0; i < nodeNum; i++) {
            for (int j = 0; j < nodeNum; j++) {
                if (i == j) {
                    continue;
                }

                Path routePath = routingList.get(i).get(j);
//                Path routePath = new Path();
                // 读取d实例
                Demand d = demandMap.get(new Pair<>(i, j));
                d.setP(routePath);
                d.setOP(routePath);
                d.load = demandList.get(x);
                d.oload = demandList.get(x);
                List<BaseVertex> vertexList = routePath.getVertexList();

                for (int k = 0; k < vertexList.size() - 1; k++) {
                    // vp vertex pair
                    Pair<Integer, Integer> vp = new Pair<>(vertexList.get(k).getId(), vertexList.get(k + 1).getId());

                    edgeMap.get(vp).addDemand(d);
                }
                x++;
            }
        }
        for (Map.Entry<Pair<Integer, Integer>, Edge> entry : edgeMap.entrySet()) {
            Edge e = (Edge) entry.getValue();
            edgePriorityQueue.add(e);
        }
//        System.out.println(edgePriorityQueue.peek().load);
//        System.out.println(edgePriorityQueue);

    }

    public List<List<Path>> getRoutingList() {
        return this.routingList;
    }

    public List<List<Path>> getStaticRoutingList() {
        return this.staticRoutingList;
    }


    public List<List<Path>> getNewRoutingList() {
        List<List<Path>> newRoutingList = new ArrayList<>();
        for (int i = 0; i < nodeNum; i++) {
            List<Path> pathList = new ArrayList<>();
            for (int j = 0; j < nodeNum; j++) {
                if (i == j) {
                    pathList.add(null);
                    continue;
                }
                Pair<Integer, Integer> key = new Pair(i, j);
                Demand d = demandMap.get(key);
                pathList.add(d.p);
            }
            newRoutingList.add(pathList);
        }
//        System.out.println(newRoutingList);
        return newRoutingList;
    }


    @Override
    public void loadFile() throws IOException {
        String end = "display";
        //配置data文件所在的根目录
//        String root = this.getClass().getResource("/").getPath();
//        System.out.println(root);
//        String dataFileName = "data/edge/edge_" + end;
//        String dataFileName = root + "edge/topology";
        Map<Pair<Integer, Integer>, Double> edgeSet = new HashMap<Pair<Integer, Integer>, Double>();
        // 1. read and create edge set
        try {
            // 1.1. read the file and put the content in the buffer
            InputStream is = this.getClass().getResourceAsStream("/edge/topology");
            BufferedReader bufRead = new BufferedReader(new InputStreamReader(is));
            // FileReader input = new FileReader(dataFileName);
            // BufferedReader bufRead = new BufferedReader(input);


            String line;    // String that holds current file line

            line = bufRead.readLine();
            while (line != null) {
                // 1.2.1 skip the empty line
                if (line.trim().equals("")) {
                    line = bufRead.readLine();
                    continue;
                }
                // 1.2.2 find a new edge and put it in the Set
                String[] strList = line.trim().split("\\s");
                int startVertexId = Integer.parseInt(strList[0]);
                int endVertexId = Integer.parseInt(strList[1]);
//                Edge e = new Edge(startVertexId, endVertexId, 0);
//                this.edgePriorityQueue.add()
                double capacity = Double.parseDouble(strList[2]);
                edgeSet.put(new Pair<>(startVertexId, endVertexId), 0d);

                Edge e = new Edge(startVertexId, endVertexId, 0d);
                e.c = capacity;
                edgeMap.put(new Pair<>(startVertexId, endVertexId), e);
                line = bufRead.readLine();
            }
            bufRead.close();
        } catch (IOException e) {
            // If another exception is generated, print a stack trace
            e.printStackTrace();
        }
        String demandFile = "";
//        demandFile = root + "demand/demand2";

        // 2.read demand data and calculate the weight of edge
//        String demandFile = "data/demand/test_0";
//        String demandFile = "data/demand/test_0"+fileEnd;
        // 2.1 read demand data
//        List<Double> demandList = new ArrayList<Double>();
        try {
//            FileReader input = new FileReader(demandFile);
//            BufferedReader bufRead = new BufferedReader(input);
            InputStream is = this.getClass().getResourceAsStream("/demand/demand2");
            BufferedReader bufRead = new BufferedReader(new InputStreamReader(is));
            String line;    // String that holds current file line

            line = bufRead.readLine();
            while (line != null) {
                // skip the empty line
                if (line.trim().equals("")) {
                    line = bufRead.readLine();
                    continue;
                }

                String[] demandStr = line.trim().split("\\s");

                for (String s : demandStr) {
                    demandList.add(Double.parseDouble(s));
                }
                line = bufRead.readLine();
//                System.out.println(demandList);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        // 2.2 read the routing data

        List<BaseVertex> allVertexList = new Vector<BaseVertex>();
        for (int i = 0; i < nodeNum; i++) {
            Vertex vertex = new Vertex(i);
            allVertexList.add(vertex);
        }
        String kspFile = "";
//        kspFile = root + "aksp/all_ksp.json";

        ObjectMapper mapper = new ObjectMapper();
        String strResult = "";
        try {
            InputStream is = this.getClass().getResourceAsStream("/aksp/all_ksp.json");
            BufferedReader bufRead = new BufferedReader(new InputStreamReader(is));
            JsonNode map = mapper.readTree(bufRead);
            strResult = map.get("aksp").toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<List<List<List<Integer>>>> allRouting = mapper.readValue(strResult, List.class);
//        List<List<List<Integer>>> allRouting = tempRouting.get(0);
        List<List<Path>> curRouting = new ArrayList<>();
        for (int i = 0; i < nodeNum; i++) {
            List<Path> tempList = new ArrayList<>();
            for (int j = 0; j < nodeNum; j++) {
                if (i == j) {
                    tempList.add(null);
                    continue;
                }
                List<BaseVertex> tempVertexList = new Vector<BaseVertex>();
                for (int k = 0; k < allRouting.get(i).get(j).get(0).size(); k++) {
                    tempVertexList.add(allVertexList.get(allRouting.get(i).get(j).get(0).get(k)));
                }
                Path path = new Path(tempVertexList, 0);
//                Path Path = new Path(path,0);
                tempList.add(path);
            }
            this.staticRoutingList.add(tempList);
        }
        this.routingList = this.staticRoutingList;


        for (int i = 0; i < nodeNum; i++) {
//            List<List<Path>> tempList = new ArrayList<>();
            for (int j = 0; j < nodeNum; j++) {
                if (i == j) {
                    this.ASPs.add(null);
                    continue;
                }
                List<Path> tempPathList = new ArrayList<>();
                for (int k = 0; k < allRouting.get(i).get(j).size(); k++) {
                    List<BaseVertex> tempVertexList = new Vector<BaseVertex>();
                    for (int n = 0; n < allRouting.get(i).get(j).get(k).size(); n++) {
                        tempVertexList.add(allVertexList.get(allRouting.get(i).get(j).get(k).get(n)));
                    }
                    Path path = new Path(tempVertexList, 0);
//                    Path Path = new Path(path,0);
                    tempPathList.add(path);
                }
                this.ASPs.add(tempPathList);
            }
        }

    }

    @Override
    public Map<String, List<List<Integer>>> getDefaultPath() {
        Map<String, List<List<Integer>>> DefaultPathDict = new HashMap<>();
        List<List<Integer>> defaultPathList = new ArrayList<>();
        for (int i = 0; i < nodeNum; i++) {
            for (int j = 0; j < nodeNum; j++) {
                if (i == j) {
                    continue;
                }
                defaultPathList.add(staticRoutingList.get(i).get(j).toList());
            }
        }
        DefaultPathDict.put(this.flowType, defaultPathList);
//        System.out.println(DefaultPathDict);
        return DefaultPathDict;
    }

    @Override
    public Map<String, List<List<Integer>>> getOptPath(Map<String, List<Double>> demand) {

        resetEdgeQWithNewDemand(demand.get(this.flowType));
        optimize();
        List<List<Path>> nowRouting = getNewRoutingList();
        Map<String, List<List<Integer>>> OptPathDict = new HashMap<>();
        List<List<Integer>> optPathList = new ArrayList<>();

        for (int i = 0; i < nodeNum; i++) {
            for (int j = 0; j < nodeNum; j++) {
                if (i == j) {
                    continue;
                }
                optPathList.add(nowRouting.get(i).get(j).toList());
            }
        }
        OptPathDict.put(this.flowType, optPathList);
//        System.out.println(OptPathDict);
        return OptPathDict;
    }


    public String getFlowType() {
        return flowType;
    }

    public void setFlowType(String flowType) throws Exception {
        String[] s = new String[]{"video", "iot", "voIP", "ar"};
        List<String> flowTypeList = Arrays.asList(s);
        if (flowTypeList.contains(flowType)) {
            this.flowType = flowType;
        } else {
            throw new Exception("wrong flow type,exit");
        }
    }
}