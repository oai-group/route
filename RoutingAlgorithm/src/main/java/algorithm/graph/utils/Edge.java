package algorithm.graph.utils;

import java.util.HashMap;
import java.util.Map;

public class Edge implements Comparable<Edge> {
    public double load = 0;
    public double we = 0d;
    public double c = 1;
    public int source;
    public int target;

    public BinaryTree bt = new BinaryTree();
    public Map<Demand, Node> demandNodeMap = new HashMap<>();

    //    Queue<Pair> pairs = new PriorityQueue<Pair>();
    public Edge(int source, int target, double load) {
        this.source = source;
        this.target = target;
        this.load = load;
    }

    public String toString() {
        return source + "," + target + ":" + load;
    }


//    @Override
//    public int compare(Edge o1, Edge o2) {
//        if(o1.load - o2.load < 0){
//            return 1;
//        }
//        else if (o1.load - o2.load == 0){
//            return 0;
//        }
//        else
//            return -1;
//    }

    public int compareTo(Edge e) {
        if (this.load - e.load < 0) {
            return 1;
        } else if (this.load - e.load == 0) {
            return 0;
        } else return -1;
    }

    public void addDemand(Demand d) {
        if (!demandNodeMap.containsKey(d)) {
//            Node n = new Node(d.load);
            Node n = new Node(Math.pow(d.load, 2));
            n.demand = d;
            this.demandNodeMap.put(d, n);
            this.bt.insert(n);
            this.load += d.load / this.c;
        }
    }

    public void removeDemand(Demand d) {
        if (demandNodeMap.containsKey(d)) {
            Node n = demandNodeMap.get(d);
            this.demandNodeMap.remove(d);
            bt.remove(n);
            this.load -= d.load / this.c;
        }

    }

    public Demand chooseDemand() {
        Demand d = this.bt.chooseOne();
        return d;
    }

    public void reset() {
        this.load = 0;
        this.bt = new BinaryTree();
        this.demandNodeMap.clear();
    }

}
