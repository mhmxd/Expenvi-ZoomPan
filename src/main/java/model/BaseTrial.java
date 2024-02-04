package model;

import enums.Task;

abstract public class BaseTrial {
    public int id;
    public final Task task;
    public int blockNum;
    public int trialNum;
//    public final int level;
    public boolean finished;
    public int retries;

    public BaseTrial(Task task) {
        this.task = task;
//        this.level = level;
        this.finished = false;
        this.retries = 0;
    }

    @Override
    public String toString() {
        return "BaseTrial{" +
                "id=" + id +
                ", task='" + task + '\'' +
                ", blockId=" + blockNum +
                ", trialNum=" + trialNum +
                ", finished=" + finished +
                ", retries=" + retries +
                '}';
    }
}
