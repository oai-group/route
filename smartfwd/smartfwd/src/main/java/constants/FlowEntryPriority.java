package apps.smartfwd.src.main.java.constants;

public class FlowEntryPriority {
    //作为最后一跳的交换机，将packet送给host
    public static final int TABLE0_HANDLE_LAST_HOP=60002;
    public static final int TABLE0_TAG_FLOW=60000;
    // table0 如果遇到已经打上标签的flow
    public static final int TABLE0_HANDLE_TAGGED_FLOW=60001;
    // table0 默认流表优先级,但是比fwd的优先级要高
    public static final int TABLE0_DEFAULT=10000;

    //table1 用于统计流量矩阵
    public static final int TABLE1_MATRIX_COLLECTION=60000;

    //table2 优化路由
    public static final int TABLE2_OPT_ROUTING=60000;
    //table2 默认路由
    public static final int TABLE2_DEFAULT_ROUTING=59999;
    //table2 drop
    public static final int TABLE2_DROP=40000;

    public static final int ONE_ROUTING=60100;
}


