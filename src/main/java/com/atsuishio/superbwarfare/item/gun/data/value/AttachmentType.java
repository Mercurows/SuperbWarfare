package com.atsuishio.superbwarfare.item.gun.data.value;

public enum AttachmentType {
    SCOPE("Scope"),
    MAGAZINE("Magazine"),
    BARREL("Barrel"),
    STOCK("Stock"),
    GRIP("Grip");

    private final String name;

    AttachmentType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
