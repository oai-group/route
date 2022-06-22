package apps.smartfwd.src.main.java.task.base;

import java.util.concurrent.Callable;

public interface Task extends Runnable {
    void start();
}
