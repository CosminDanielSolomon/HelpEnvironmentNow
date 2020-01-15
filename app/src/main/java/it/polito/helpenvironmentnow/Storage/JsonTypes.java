package it.polito.helpenvironmentnow.Storage;

public enum JsonTypes {
    CLASSIC("c"),
    MOVEMENT("m");

    private String type;

    JsonTypes(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
