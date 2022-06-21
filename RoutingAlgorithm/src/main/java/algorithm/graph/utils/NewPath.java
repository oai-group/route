package algorithm.graph.utils;

public class NewPath extends Path {
    private int changeTimes = 0;
//    private List<BaseVertex> vertexList = new Vector<BaseVertex>();
//    private double weight = -1;

    public NewPath(Path p, int count) {
        super(p.getVertexList(), p.getWeight());
        this.changeTimes = count;
    }
}
