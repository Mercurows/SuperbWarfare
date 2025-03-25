package com.atsuishio.superbwarfare.perk;

public class Perk {
    public String descriptionId;
    public Type type;

    public Perk(String descriptionId, Type type) {
        this.descriptionId = descriptionId;
        this.type = type;
    }

    public enum Type {
        AMMO("Ammo"),
        FUNCTIONAL("Functional"),
        DAMAGE("Damage");
        private final String type;

        Type(String type) {
            this.type = type;
        }

        public String getName() {
            return type;
        }
    }
}
