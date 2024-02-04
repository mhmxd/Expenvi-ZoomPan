package model;

public class ZoomTrial extends BaseTrial {
    public final int startLevel;
    public final int endLevel;

    public ZoomTrial(String task, int level, int startLevel, int endLevel) {
        super(task, level);

        this.startLevel = startLevel;
        this.endLevel = endLevel;
    }

    @Override
    public String toString() {
        return "ZoomTrial{" +
                "id=" + id +
                ", task='" + task + '\'' +
                ", blockNum=" + blockNum +
                ", trialNum=" + trialNum +
                " startLevel=" + startLevel +
                ", endLevel=" + endLevel +
                '}';
    }
}
