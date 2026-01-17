package org.mrutcka.lvluping.data;

import net.minecraft.resources.ResourceLocation;
import org.mrutcka.lvluping.LvlupingMod;

public enum Talent {
    // 0. КОРЕНЬ
    START("start", "Истоки", "Начало вашего пути", 0, 0, 0, "", new Race[]{}, new Talent[]{}),

    // ======================== ВОИН (ВПРАВО) ========================
    WARRIOR_BASE("warrior_base", "Воин", "Путь силы", 1, 300, 0, "class", new Race[]{}, new Talent[]{START}),
    W_PARRY("w_parry", "Парирование", "Блокирует часть урона", 2, 500, -150, "warrior", new Race[]{}, new Talent[]{WARRIOR_BASE}),
    W_COMBO("w_combo", "Комбо", "Серия быстрых атак", 2, 550, 0, "warrior", new Race[]{}, new Talent[]{WARRIOR_BASE}),
    W_STUN("w_stun", "Оглушение", "Шанс остановить цель", 2, 500, 150, "warrior", new Race[]{}, new Talent[]{WARRIOR_BASE}),
    W_MASTERY("w_mastery", "Мастерство Клинка", "Требуется для эволюции", 3, 750, 0, "", new Race[]{}, new Talent[]{W_PARRY, W_COMBO, W_STUN}),
    W_EVO_PALADIN("w_evo_paladin", "Паладин", "Святая защита", 5, 950, -150, "warrior_evo", new Race[]{}, new Talent[]{W_MASTERY}),
    W_EVO_BERSERK("w_evo_berserk", "Берсерк", "Безумная ярость", 5, 1000, 0, "warrior_evo", new Race[]{}, new Talent[]{W_MASTERY}),
    W_EVO_WARLORD("w_evo_warlord", "Полководец", "Аура для союзников", 5, 950, 150, "warrior_evo", new Race[]{}, new Talent[]{W_MASTERY}),

    // ======================== ЛУЧНИК (ВЛЕВО) ========================
    ARCHER_BASE("archer_base", "Лучник", "Меткость и ловкость", 1, -300, 0, "class", new Race[]{}, new Talent[]{START}),
    A_DASH("a_dash", "Рывок спиной", "Быстрый отскок назад", 2, -500, -150, "archer", new Race[]{}, new Talent[]{ARCHER_BASE}),
    A_DAGGER("a_dagger", "Кинжал", "Ближний бой для стрелка", 2, -550, 0, "archer", new Race[]{}, new Talent[]{ARCHER_BASE}),
    A_POWER("a_power", "Сильный выстрел", "Пробивает броню", 2, -500, 150, "archer", new Race[]{}, new Talent[]{ARCHER_BASE}),
    A_MASTERY("a_mastery", "Мастерство Охоты", "Подготовка к эволюции", 3, -750, 0, "", new Race[]{}, new Talent[]{A_DASH, A_DAGGER, A_POWER}),
    A_EVO_SNIPER("a_evo_sniper", "Снайпер", "Огромный урон издалека", 5, -950, -150, "archer_evo", new Race[]{}, new Talent[]{A_MASTERY}),
    A_EVO_HUNTER("a_evo_hunter", "Зверолов", "Призыв питомца", 5, -1000, 0, "archer_evo", new Race[]{}, new Talent[]{A_MASTERY}),
    A_EVO_CROSSBOW("a_evo_crossbow", "Арбалетчик", "Скорострельный бой", 5, -950, 150, "archer_evo", new Race[]{}, new Talent[]{A_MASTERY}),

    // ======================== МАГ (ВВЕРХ) ========================
    MAGE_BASE("mage_base", "Маг", "Знание тайного", 1, 0, -300, "class", new Race[]{Race.MECHANID}, new Talent[]{START}),
    M_BARRIER("m_barrier", "Барьер", "Магический щит", 2, 0, -450, "mage", new Race[]{}, new Talent[]{MAGE_BASE}),
    M_BUFF_DEF("m_buff_def", "Усиление Защиты", "Повышает броню магией", 2, 0, -600, "mage", new Race[]{}, new Talent[]{M_BARRIER}),
    M_BUFF_ATK("m_buff_atk", "Усиление Атаки", "Ваши заклинания бьют сильнее", 2, 0, -750, "mage", new Race[]{}, new Talent[]{M_BUFF_DEF}),
    M_EVO_ARCHMAGE("m_evo_archmage", "Архимаг", "Магия стихий", 5, -150, -950, "mage_evo", new Race[]{}, new Talent[]{M_BUFF_ATK}),
    M_EVO_NECRO("m_evo_necro", "Некромант", "Власть над смертью", 5, 0, -1000, "mage_evo", new Race[]{}, new Talent[]{M_BUFF_ATK}),
    M_EVO_CHRONO("m_evo_chrono", "Хрономант", "Управление временем", 5, 150, -950, "mage_evo", new Race[]{}, new Talent[]{M_BUFF_ATK}),

    // ======================== АССАСИН (ВНИЗ) ========================
    ASSASSIN_BASE("assassin_base", "Ассасин", "Тень и смерть", 1, 0, 300, "class", new Race[]{}, new Talent[]{START}),
    AS_SLIDE("as_slide", "Подкат", "Сближение с целью", 2, -150, 500, "assassin", new Race[]{}, new Talent[]{ASSASSIN_BASE}),
    AS_SMOKE("as_smoke", "Дымовая завеса", "Скрывает ваше положение", 2, 0, 550, "assassin", new Race[]{}, new Talent[]{ASSASSIN_BASE}),
    AS_STEALTH_CRIT("as_crit", "Крит из тени", "Удар в спину из невидимости", 2, 150, 500, "assassin", new Race[]{}, new Talent[]{ASSASSIN_BASE}),
    AS_MASTERY("as_mastery", "Мастерство Тени", "Шаг в бездну", 3, 0, 750, "", new Race[]{}, new Talent[]{AS_SLIDE, AS_SMOKE, AS_STEALTH_CRIT}),
    AS_EVO_NINJA("as_evo_ninja", "Ниндзя", "Акробатика и кунаи", 5, -150, 950, "assassin_evo", new Race[]{}, new Talent[]{AS_MASTERY}),
    AS_EVO_REAPER("as_evo_reaper", "Жнец", "Мастер кос", 5, 0, 1000, "assassin_evo", new Race[]{}, new Talent[]{AS_MASTERY}),
    AS_EVO_POISONER("as_evo_poisoner", "Отравитель", "Смертельные яды", 5, 150, 950, "assassin_evo", new Race[]{}, new Talent[]{AS_MASTERY});

    public final String id, label, description, branch;
    public final int cost, x, y;
    public final Talent[] parents;
    public final Race[] forbiddenRaces;
    public final ResourceLocation icon;

    Talent(String id, String label, String description, int cost, int x, int y, String branch, Race[] forbiddenRaces, Talent[] parents) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.cost = cost;
        this.x = x;
        this.y = y;
        this.branch = branch;
        this.forbiddenRaces = forbiddenRaces != null ? forbiddenRaces : new Race[0];
        this.parents = parents != null ? parents : new Talent[0];
        this.icon = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/talents/" + id + ".png");
    }

    public static Talent getById(String id) {
        for (Talent t : values()) if (t.id.equals(id)) return t;
        return null;
    }
}