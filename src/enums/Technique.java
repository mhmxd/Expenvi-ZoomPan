package enums;

@SuppressWarnings("unused")
public enum Technique {
    MOUSE(1, "Mouse"),
    MOOSE(2, "Moose");

    private final int id;
    private final String text;

    Technique(int id, String text) {
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
