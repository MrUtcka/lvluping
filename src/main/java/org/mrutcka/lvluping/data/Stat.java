package org.mrutcka.lvluping.data;

public enum Stat {
    STRENGTH("strength", "Сила", -200, 50, 20, "Увеличивает урон"),
    SPEED("speed", "Скорость", 0, 50, 15, "Увеличивает скорость бега"),
    VITALITY("vitality", "Живучесть", 200, 50, 30, "Увеличивает макс. здоровье");

    public final String id, label, description;
    public final int x, y, maxLevel;

    Stat(String id, String label, int x, int y, int maxLevel, String description) {
        this.id = id; this.label = label; this.x = x; this.y = y;
        this.maxLevel = maxLevel; this.description = description;
    }

    public static Stat getById(String id) {
        for (Stat s : values()) if (s.id.equals(id)) return s;
        return null;
    }
}