package apps.smartfwd.src.main.java.task.base;

import apps.smartfwd.src.main.java.constants.App;
import apps.smartfwd.src.main.java.task.base.Task;

import java.util.concurrent.ExecutorService;

public abstract class AbstractTask implements Task {
    protected ExecutorService service= App.getInstance().getPool();
    protected Thread worker;
    public void start(){
        if(null==service){
            worker=new Thread(this);
            worker.start();
        }else{
            service.submit(this);
        }
    }
}
