package apps.smartfwd.src.main.java.constants;

public class Env {
    //todo decide switch number at runtime
    public static int N_SWITCH=0;
    public static final int N_FLOWS=3;
    private Env(){}
    static Env instance;
    static Env getInstance(){
        return instance;
    }

}
