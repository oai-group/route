package algorithm.graph.abstraction;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface Algorithm {
    // load file as needed
    void loadFile() throws IOException;
    //flowType->List
    // e.g. say,we have 3 nodes
    // 0------1---------2

    /**
     * {
     * "video":[
     * [0,1],
     * [0,1,2],
     * [1,0],
     * [1,2],
     * [2,1,0],
     * [2,1]
     * ]
     * "iot":[
     * ...
     * ]
     * }
     *
     * @return
     */
    Map<String, List<List<Integer>>> getDefaultPath();

    /**
     * @param { "video":[
     *          //0->1
     *          100,
     *          //0->2
     *          101,
     *          //1->0
     *          102,
     *          //1->2
     *          103,
     *          //2->0
     *          104,
     *          //2->1
     *          105
     *          ],
     *          iot:[
     *          ...
     *          ]
     *          }
     * @return
     */
    Map<String, List<List<Integer>>> getOptPath(Map<String, List<Double>> demand);
}
