package model;

import enums.Task;

public class ZoomTrial extends BaseTrial {
    public final int startNotch;
    public final int targetNotch;

    public ZoomTrial(Task task, int startNotch, int targetNotch) {
        super(task);

        this.startNotch = startNotch;
        this.targetNotch = targetNotch;
    }

    @Override
    public String toString() {
        return "ZoomTrial{" +
                "id=" + id +
                ", task='" + task + '\'' +
                ", blockNum=" + blockNum +
                ", trialNum=" + trialNum +
                " startNotch=" + startNotch +
                ", targetNotch=" + targetNotch +
                '}';
    }
}
