package algorithm.graph.utils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
public class Demand {
    public int source;
    public int target;
    public double load;
    public double oload;
    public Path p;
    public Path oP;
    public Set<Path> allPathsSet = new HashSet<Path>();

    public Demand(int source, int target, double load) throws IOException, ClassNotFoundException {
        this.source = source;
        this.target = target;
        this.load = load;
    }

    public int hashCode() {
        String code = source + "-" + target;
        return code.hashCode();
    }

    /**
     * set path
     *
     * @param p
     */
    public void setP(Path p) {
        this.p = p;
    }

    public void setOP(Path p) {
        this.oP = p;
    }

    public void setAllPathsSet(List<Path> pathList) {
        this.allPathsSet.clear();
        this.allPathsSet.addAll(pathList);
    }

}
