package model;

abstract public class BaseTrial {
    public final String task;
    public int blockId;
    public int trialInBlock;
    public final int level;
    public boolean finished;
    public int retries;

    public BaseTrial(String task, int level) {
        this.task = task;
        this.level = level;
        this.finished = false;
        this.retries = 0;
    }
}
