package algorithm.graph.utils;

public class Node {
    public double weight = 0;
    //    Node left;
    //    Node right;
    //    Node father;
    public int index;
    public Demand demand = null;

    public Node(double weight) {
//        this.weight = weight;
        this.weight = Math.pow(weight, 1);

//        this.left = null;
//        this.right = null;
//        this.father = null;
    }

}
