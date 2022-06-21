package algorithm.graph.utils;

import java.util.List;
import java.util.Vector;


public class BinaryTree {
    Node Root;
    int size;   //当前叶节点数目
    List<Node> nodeList = new Vector<Node>();   //当前总节点数目

    //    List<Node> useNodelist = new Vector<Node>();
    public BinaryTree() {
        this.Root = null;
        this.size = 0;
    }

    public Node right(Node n) {
        int x = n.index * 2 + 2;
        if (x < nodeList.size()) {
            return nodeList.get(x);
        } else {
            return null;
        }
    }

    public Node left(Node n) {
        int x = n.index * 2 + 1;
        if (x < nodeList.size()) {
            return nodeList.get(x);
        } else {
            return null;
        }
//        return nodeList.get(n.index*2+1);
    }

    public Node father(Node n) {
        if (n.index == 0) {
            return null;
        } else {
            return nodeList.get((n.index - 1) / 2);
        }
    }


    /**
     * 更新某节点的上部节点的权值
     *
     * @param n
     */
    public void updateWeight(Node n) {
//        while (n.father != null){
        while (father(n) != null) {
//            n.father.weight = n.father.left.weight + n.father.right.weight;
            Node f = father(n);
            father(n).weight = left(father(n)).weight + right(father(n)).weight;
//            System.out.printf("%f,%f,%f\n",n.father.weight,n.father.left.weight,n.father.right.weight);
            n = father(n);
        }

    }

    /**
     * 插入新节点
     * n为节点类
     *
     * @param n 节点类
     */
    public void insert(Node n) {
        if (this.size == 0) {
            this.Root = n;
            this.Root.index = 0;
//            n.father = null;
            nodeList.add(n);
//            this.size++;
        } else {
            int xindex = (nodeList.size() - 1) / 2;
            Node now = nodeList.get(xindex);
            Node p = new Node(now.weight + n.weight);
            nodeList.set(xindex, p);
            p.index = xindex;
            nodeList.add(now);
            now.index = nodeList.size() - 1;
            nodeList.add(n);
            n.index = nodeList.size() - 1;
            updateWeight(p);
            Root = nodeList.get(0);
        }
        this.size += 1;
//        System.out.println(this.size);
//        System.out.println(nodeList.size());
    }

    /**
     * 移除一个已定位节点n
     *
     * @param n 节点类
     */
    public void remove(Node n) {
        if (this.size == 0) {
            return;
        }
        if (this.size == 1) {
            this.size = 0;
            this.Root = null;
            this.nodeList.remove(0);
            return;
        }
        if (this.size == 2) {
            this.size = 1;
            if (n.index == 1) {
//                Node r = n.father.right;
                this.Root = nodeList.get(2);
                Root.index = 0;
//                r.father = null;
                this.nodeList.remove(0);
                this.nodeList.remove(0);

            } else {
                this.Root = nodeList.get(1);
                Root.index = 0;
                this.nodeList.remove(0);
                this.nodeList.remove(1);
            }
            return;
        }

        Node finalNode = nodeList.get(nodeList.size() - 1);
        nodeList.set(n.index, finalNode);
        int temp;
        temp = finalNode.index;
        finalNode.index = n.index;
        updateWeight(finalNode);
        n.index = temp;
        nodeList.remove(nodeList.size() - 1);
        Node l = nodeList.get(nodeList.size() - 1);
        Node f = father(l);
        l.index = f.index;
        nodeList.set(f.index, l);
        nodeList.remove(nodeList.size() - 1);
        updateWeight(l);
        Root = nodeList.get(0);
        this.size -= 1;
    }

    public Demand chooseOne() {
        if (this.size == 0) {
            return null;
        }
        Double weight = nodeList.get(0).weight;
        double random = Math.random() * weight;
//        System.out.println("random" + random);
        Node now = this.Root;
        while (true) {
            if (left(now) != null) {
                if (random > left(now).weight) {
                    random -= left(now).weight;
                    now = right(now);
                } else {
                    now = left(now);
                }
            }
            if (left(now) == null) {
//                System.out.println(now.weight);
                return now.demand;
            }
        }
    }

    public void printNodeList() {
        System.out.println("now the tree size is " + this.size);
        if (this.size == 0) {
            System.out.println("the tree is empty now");
        }
        for (Node node : this.nodeList) {
            System.out.println(node.weight);
        }
    }

//    public static void main(String[] args) {
//        List<Node> l = new Vector<>();
//        l.add(new Node(1.0));
//        l.add(new Node(1.2));
//        l.add(new Node(1.3));
//        l.add(new Node(1.6));
//        l.add(new Node(1.7));
//        l.add(new Node(0.3));
//        l.add(new Node(0.4));
//        l.add(new Node(0.5));
//        BinaryTree bt = new BinaryTree();
//        for (Node node : l) bt.insert(node);
//        for (int i = 0; i < bt.nodeList.size(); i++) {
//            System.out.println(bt.nodeList.get(i).weight);
//        }
//        for (int i = 0; i < bt.nodeList.size(); i++) {
//
//            System.out.println(bt.nodeList.get(i).index);
//        }
////        for(int i=0;i<l.size();i++){
////            System.out.println(i+"次");
////            bt.remove(l.get(i));
////            bt.printNodeList();
////        }
//        System.out.println(bt.Root.index);
//        bt.chooseOne();
//        bt.chooseOne();
//        bt.chooseOne();
//    }
}
