package org.mrutcka.lvluping.data;

import java.util.Map;

public enum Race {
    MECHANID("mechanid", "Механид",
            Map.of(AttributeStat.HEALTH.id, 4, AttributeStat.DAMAGE.id, 2)),
    IGNIS("ignis", "Игниец",
            Map.of(AttributeStat.DAMAGE.id, 4)),
    DURUM("durum", "Дурум",
            Map.of(AttributeStat.HEALTH.id, 6, AttributeStat.DAMAGE.id, 4)),
    BEAST("beast", "Бестиец",
            Map.of(AttributeStat.SPEED.id, 2, AttributeStat.DAMAGE.id, 3)),
    VENTAR("ventar", "Вентар",
            Map.of(AttributeStat.SPEED.id, 5)),
    NOX("nox", "Нокс",
            Map.of(AttributeStat.SPEED.id, 3, AttributeStat.DAMAGE.id, 1)),
    QUASAR("quasar", "Квазар",
            Map.of(AttributeStat.HEALTH.id, 2, AttributeStat.DAMAGE.id, 2, AttributeStat.SPEED.id, 1)),
    HUMAN("human", "Человек", Map.of());

    public final String id, label;
    public final Map<String, Integer> bonuses;

    Race(String id, String label, Map<String, Integer> bonuses) {
        this.id = id;
        this.label = label;
        this.bonuses = bonuses;
    }

    public static Race getById(String id) {
        for (Race r : values()) if (r.id.equals(id)) return r;
        return HUMAN;
    }
}