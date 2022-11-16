package algorithm.graph;

import algorithm.graph.abstraction.Algorithm;
import algorithm.graph.abstraction.BaseVertex;
import algorithm.graph.utils.Path;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainFunction implements Algorithm {
    public List<oldLS> lsPool = new ArrayList<>();
    String[] s = new String[]{"video", "iot", "voIP", "ar"};
    public List<String> flowTypeList = Arrays.asList(s);
    String preStr = "";
    public int nodeNum = 19;

    public void creatLsPool() {
        for (int i = 0; i < 1; i++) {
            oldLS ls = new oldLS();
            try {
                ls.setFlowType(flowTypeList.get(i));
                ls.createEdgeQ();
            } catch (Exception e) {
                e.printStackTrace();
            }
            lsPool.add(ls);
        }
    }

    @Override
    public void loadFile() throws IOException {
    }

    @Override
    public Map<String, List<List<Integer>>> getDefaultPath() {
        Map<String, List<List<Integer>>> DefaultPathDict = new HashMap<>();
        for (int i = 0; i < 4; i++) {
            String flowType = flowTypeList.get(i);
            DefaultPathDict.put(flowType, lsPool.get(i).getDefaultPath().get(flowType));
        }
//        System.out.println(DefaultPathDict);
        for (String key : DefaultPathDict.keySet()) {
            System.out.print(key);
            System.out.println(DefaultPathDict.get(key));
        }
        return DefaultPathDict;
    }

    @Override
    public Map<String, List<List<Integer>>> getOptPath(Map<String, List<Double>> demand) {

        Map<String, List<List<Integer>>> OptPathDict = new HashMap<>();

        for (int i = 0; i < 1; i++) {
            String flowType = flowTypeList.get(i);
            OptPathDict.put(flowType, lsPool.get(i).getOptPath(demand).get(flowType));
        }
//        System.out.println(OptPathDict);
        for (String key : OptPathDict.keySet()) {
            System.out.print(key);
            System.out.println(OptPathDict.get(key));

        }
        return OptPathDict;
    }

    public void receive() {
        ServerSocketChannel serverSocketChannel = null;
        Selector selector = null;
        ByteBuffer buffer = ByteBuffer.allocate(2048);
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress("0.0.0.0", 1053));
            serverSocketChannel.configureBlocking(false);
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (selector.select() > 0) {
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey next = iterator.next();
                    iterator.remove();
                    if (next.isAcceptable()) {
                        SocketChannel accept = serverSocketChannel.accept();
                        accept.configureBlocking(false);
                        accept.register(selector, SelectionKey.OP_READ);
                    } else if (next.isReadable()) {
                        SocketChannel channel = (SocketChannel) next.channel();
                        int len = 0;
                        String res = "";
                        StringBuilder stringBuilder = new StringBuilder();
                        boolean flag = false;
                        while ((len = channel.read(buffer)) >= 0) {
                            ((Buffer) buffer).flip();
                            byte[] array = buffer.array();
                            ((Buffer) buffer).clear();
                            res = new String(array, 0, len);
                            stringBuilder.append(res);
                            for (byte b : array) {
                                if (b == '*') {
                                    flag = true;
                                    break;
                                }
                            }
                            if (flag) {
                                break;
                            }
                        }
                        ObjectMapper mapper = new ObjectMapper();
                        String getString = stringBuilder.toString();
                        JsonNode map = null;
                        if (getString.charAt(getString.length() - 1) == '*') {
                            System.out.println("成功接收到上传的数据");
                            if (preStr.length() != 0) {
                                System.out.println("---拼接字符串成功----");
                                getString = preStr + getString;
                                preStr = "";
                            }
                            getString = getString.substring(0, getString.length() - 1);
                            if (getString.charAt(getString.length() - 1) == '}') {
                                try {
                                    map = mapper.readTree(getString);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            } else {
                                System.out.println("---json格式不正确---");

                            }
                        } else {
                            System.out.println("---error:上传数据接收不完全---");
                            preStr = getString;
                        }

                        assert map != null;
                        int idx = map.get("topo_idx").asInt();
//                        System.out.println("topo:" + idx);

                        int type = 0;  // 1是静态，2是优化
                        String l = "";
                        List<List<List<Path>>> resRouting = new ArrayList<List<List<Path>>>();
                        long startTime = 0;
                        long endTime   = 0;
                        if (map.get("volumes") != null) {
                            System.out.println("请求优化路由");
                            type = 1;
                            l = map.get("volumes").toString();
                            //解析流量矩阵
                            List readValue = mapper.readValue(l, List.class);
                            List<Double> doubles = new ArrayList<>();
                            for (int i = 0; i < readValue.size(); i++) {
                                doubles.add(Double.parseDouble(readValue.get(i).toString()));
                            }
                            for (int i = 0; i < nodeNum * (nodeNum - 1); i++) {
                                doubles.add(0d);
                            }
                            List<Double> flowMatrix1 = doubles.subList(0, doubles.size() / 3);
                            List<Double> flowMatrix2 = doubles.subList(doubles.size() / 3, doubles.size() * 2 / 3);
                            List<Double> flowMatrix3 = doubles.subList(doubles.size() * 2 / 3, doubles.size());

                            //计算优化路由
                            startTime = System.currentTimeMillis();
                            List<List<Path>> tmp = getOptimizationRouting(1, idx, flowMatrix1);
                            endTime   = System.currentTimeMillis();
                            resRouting.add(tmp);
                            resRouting.add(tmp);
                            resRouting.add(tmp);
//                            getOptimizationRouting(2,idx,flowMatrix2);
//                            getOptimizationRouting(3,idx,flowMatrix3);
                            System.out.println("识别成功");

                        } else {
                            System.out.println("请求静态路由");
                            resRouting.add(lsPool.get(0).getStaticRoutingList());
                            resRouting.add(lsPool.get(0).getStaticRoutingList());
                            resRouting.add(lsPool.get(0).getStaticRoutingList());
                        }
                        sendMassage(resRouting, channel);              //send routing
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        System.out.println(df.format(new Date()) + "---------------------------------------");
                        System.out.println("管控时延 : " + (endTime - startTime) + " ms");
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 释放相关的资源
            if (selector != null) {
                try {
                    selector.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (serverSocketChannel != null) {
                try {
                    serverSocketChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 优化一种流的路由表
     *
     * @param flowType
     * @param topoID
     * @param demandList
     * @return
     * @throws IOException
     */
    public List<List<Path>> getOptimizationRouting(int flowType, int topoID, List<Double> demandList) throws IOException {
        flowType = flowType - 1;
        oldLS ls = lsPool.get(0);
        ls.resetEdgeQWithNewDemandList(demandList);
        ls.optimize();
        List<List<Path>> newRoutingList = new ArrayList<>();
        newRoutingList = ls.getNewRoutingList();//获得路由表
        // 更新ls中的当前路由表
        // ls.changeCurRouting(newRoutingList);
        //更新main当前路由表
//        CurRouting.set(flowType,newRoutingList);

        return newRoutingList;
    }

    public void sendMassage(List<List<List<Path>>> CurRouting, SocketChannel socketChannel) throws IOException {
        List<List<List<Path>>> RoutingList = CurRouting;
        List<List<List<Integer>>> StringList = new ArrayList<>();
//        List<List<List<List<Integer>>>> StringList = new ArrayList<>();
        //转换路由表格式
        for (int i = 0; i < RoutingList.size(); i++) {
            List<List<Integer>> temp2dList = new ArrayList<>();
            for (int j = 0; j < nodeNum; j++) {
                for (int k = 0; k < nodeNum; k++) {
                    if (k == j) {
                        continue;
                    } else {
                        List<BaseVertex> vertexList = RoutingList.get(i).get(j).get(k).getVertexList();
                        List<Integer> integerList = new ArrayList<>();
                        for (int l = 0; l < vertexList.size(); l++) {
                            integerList.add(vertexList.get(l).getId());
                        }
                        temp2dList.add(integerList);
                    }
                }
            }
            StringList.add(temp2dList);

        }

        //序列化发送路由表
        ObjectMapper mapper = new ObjectMapper();
        Map<String, List<List<Integer>>> map = new HashMap<>();
        map.put("res1", StringList.get(0));
        map.put("res2", StringList.get(1));
        map.put("res3", StringList.get(2));

        String resString = mapper.writeValueAsString(map);
//        System.out.println(resString);
        ByteBuffer byteBuffer = ByteBuffer.allocate(512 * 1024);
        byteBuffer.put(resString.getBytes());
        ((Buffer) byteBuffer).flip();
        socketChannel.write(byteBuffer);
        while (byteBuffer.hasRemaining()) {
            socketChannel.write(byteBuffer);
        }
        socketChannel.close();
        System.out.println("---数据已经发送给控制器---");
    }

    public static void main(String[] args) {
        MainFunction mainFunction = new MainFunction();
        mainFunction.creatLsPool();
//        mainFunction.getDefaultPath();
//        Map<String, List<Double>> dDict = new HashMap<>();
//        for (int i = 0; i < 1; i++) {
//            dDict.put(mainFunction.flowTypeList.get(i),mainFunction.lsPool.get(i).demandList);
//        }
//        mainFunction.getOptPath(dDict);
        mainFunction.receive();
    }

}