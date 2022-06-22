package apps.smartfwd.src.main.java.task.base;

public interface StoppableTask extends Runnable {
    void start();
    void stop();
}
