package enums;

@SuppressWarnings("unused")
public enum Task {
    ZOOM_OUT(1, "ZoomOut"),
    ZOOM_IN(2, "ZoomIn"),
    PAN(3, "Panning");

    private final int id;
    private final String text;

    Task(int id, String text) {
        this.id = id;
        this.text = text;
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }
}
