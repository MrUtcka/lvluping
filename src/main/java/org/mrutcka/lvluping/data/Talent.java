package org.mrutcka.lvluping.data;

import net.minecraft.resources.ResourceLocation;
import org.mrutcka.lvluping.LvlupingMod;

public enum Talent {
    // 0. КОРЕНЬ
    START("start", "Истоки", "Начало вашего пути", 0, 0, 0, "", new Race[]{}, new Talent[]{}),

    // ======================== ВОИН (ВПРАВО) ========================
    WARRIOR_BASE("warrior_base", "Воин", "Открывает ветку боевых талантов и базовые умения ближнего боя.", 1, 300, 0, "class", new Race[]{}, new Talent[]{START}),

    W_STUN("w_stun", "Оглушающий удар", "Шанс при атаке наложить сильное замедление и ослепление на цель.", 2, 500, 150, "warrior", new Race[]{}, new Talent[]{WARRIOR_BASE}),
    W_PARRY("w_parry", "Парирование", "Активная способность: короткое окно, в течение которого первая полученная атака полностью блокируется.", 2, 700, 300, "warrior", new Race[]{}, new Talent[]{W_STUN}),
    W_IRON_SKIN("w_iron_skin", "Железная кожа", "Активный бафф: на 5 секунд увеличивает защиту Воина, снижая скорость передвижения на 20% и снимая эффекты контроля в момент активации.", 3, 900, 450, "warrior", new Race[]{}, new Talent[]{W_PARRY}),
    W_PROVOCATION("w_provocation", "Провокация", "Боевой клич заставляет ближайших врагов атаковать только вас 3 сек. Урон по всем, кроме вас, снижен.", 3, 1100, 600, "warrior", new Race[]{}, new Talent[]{W_IRON_SKIN}),
    W_ULT_FINAL_COUNTDOWN("w_ult_final_countdown", "Судный молот", "Метка цели. С неба обрушивается Судный молот: огромный урон в радиусе. Чем меньше HP цели в момент призыва — тем выше урон.", 5, 1300, 750, "warrior_ult", new Race[]{}, new Talent[]{W_PROVOCATION}),

    W_SHIELD_STRIKE("w_shield_strike", "Удар щитом", "Активный навык: вход в стойку блока. Если повторно использовать в течение 2 секунд, Воин делает мощный удар щитом, оглушающий ближайшую цель.", 2, 800, 0, "warrior", new Race[]{}, new Talent[]{WARRIOR_BASE}),
    W_SEISMIC("w_seismic", "Сейсмический удар", "Активный навык: удар по земле создаёт волну перед Воином, замедляющую врагов в конусе на 3 секунды.", 2, 1000, -75, "warrior", new Race[]{}, new Talent[]{W_SHIELD_STRIKE}),
    W_HEAVY_STEP("w_heavy_step", "Тяжёлая поступь", "Активный рывок вперёд: Воин игнорирует контроль и отбрасывает врагов на своём пути.", 2, 1200, 75, "warrior", new Race[]{}, new Talent[]{W_SEISMIC}),
    W_UNBREAKABLE("w_unbreakable", "Несокрушимый", "Пассивно: один раз при смертельном ударе вы не погибаете (как тотем), получаете регенерацию. Срабатывает раз в 90 секунд.", 3, 1400, -75, "warrior", new Race[]{}, new Talent[]{W_HEAVY_STEP}),
    W_ULT_BROTHERHOOD("w_ult_brotherhood", "Братство", "Оружие в землю: круговая зона 6 сек. Союзники в зоне получают -30% к входящему урону, враги замедлены на 50%. Нельзя покидать круг.", 5, 1600, 75, "warrior_ult", new Race[]{}, new Talent[]{W_UNBREAKABLE}),
    W_ULT_INVULNERABILITY("w_ult_invulnerability", "Неуязвимость", "6 сек: столп света. Нельзя двигаться и атаковать, но вы неуязвимы и притягиваете атаки на себя, защищая союзников.", 5, 1800, -75, "warrior_ult", new Race[]{}, new Talent[]{W_UNBREAKABLE}),

    W_COMBO("w_combo", "Комбо-удары", "Пассивно накапливает серии ударов. Чем дольше серия без пауз, тем выше урон.", 1, 500, -150, "warrior", new Race[]{}, new Talent[]{WARRIOR_BASE}),
    W_BLOODLUST("w_bloodlust", "Кровавая жажда", "Пассивно: каждая пятая успешная атака восстанавливает Воину 30% от нанесённого урона.", 2, 700, -300, "warrior", new Race[]{}, new Talent[]{W_COMBO}),
    W_SPIN("w_spin", "Круговой удар", "Активный навык: Воин делает круговой взмах оружием, нанося урон всем ближайшим врагам. Если задеты 3+ цели, время перезарядки сокращается вдвое.", 2, 900, -450, "warrior", new Race[]{}, new Talent[]{W_BLOODLUST}),
    W_ARMOR_BREAKER("w_armor_breaker", "Разрушитель доспехов", "Пассивный мощный удар: периодически атака Воина усиливается и накладывает дебафф, снижающий получаемое лечение.", 3, 1100, -600, "warrior", new Race[]{}, new Talent[]{W_SPIN}),
    W_ULT_BERSERK("w_ult_berserk", "Берсерк", "Ярость на 8 сек: атаки наносят чистый урон (игнорируя защиту), блок и уклонение недоступны. Каждая атака восстанавливает 5% здоровья.", 5, 1300, -750, "warrior_ult", new Race[]{}, new Talent[]{W_ARMOR_BREAKER}),

    // ======================== ЛУЧНИК (ВЛЕВО) ========================
    ARCHER_BASE("archer_base", "Лучник", "Меткость и ловкость", 1, -300, 0, "class", new Race[]{}, new Talent[]{START}),
    A_DASH("a_dash", "Рывок спиной", "Быстрый отскок назад", 2, -500, -150, "archer", new Race[]{}, new Talent[]{ARCHER_BASE}),
    A_DAGGER("a_dagger", "Кинжал", "Ближний бой для стрелка", 2, -550, 0, "archer", new Race[]{}, new Talent[]{ARCHER_BASE}),
    A_POWER("a_power", "Сильный выстрел", "Пробивает броню", 2, -500, 150, "archer", new Race[]{}, new Talent[]{ARCHER_BASE}),

    // ======================== МАГ (ВВЕРХ) ========================
    MAGE_BASE("mage_base", "Маг", "Знание тайного", 1, 0, -300, "class", new Race[]{Race.MECHANID}, new Talent[]{START}),
    M_BARRIER("m_barrier", "Барьер", "Магический щит", 2, 0, -450, "mage", new Race[]{}, new Talent[]{MAGE_BASE}),
    M_BUFF_DEF("m_buff_def", "Усиление Защиты", "Повышает броню магией", 2, 0, -600, "mage", new Race[]{}, new Talent[]{M_BARRIER}),
    M_BUFF_ATK("m_buff_atk", "Усиление Атаки", "Ваши заклинания бьют сильнее", 2, 0, -750, "mage", new Race[]{}, new Talent[]{M_BUFF_DEF}),

    M_SUMMONER_BASE("m_summoner_base", "Призыватель", "Открывает путь Призывателя.", 2, -220, -420, "summoner", new Race[]{}, new Talent[]{MAGE_BASE}),
    M_SUMMON_SERVANT("m_summon_servant", "Призыв слуги", "Призывает скелета-лучника на 30 сек. (присев — зомби-стража).", 2, -320, -560, "summoner", new Race[]{}, new Talent[]{M_SUMMONER_BASE}),
    M_SUMMON_SACRIFICE("m_summon_sacrifice", "Жертва", "Убивает одного слугу, восстанавливая вам 5 HP.", 2, -420, -700, "summoner", new Race[]{}, new Talent[]{M_SUMMON_SERVANT}),
    M_SUMMON_COMMAND("m_summon_command", "Команда", "Приказывает всем слугам атаковать выбранную цель.", 3, -520, -840, "summoner", new Race[]{}, new Talent[]{M_SUMMON_SACRIFICE}),

    M_SPELLCASTER_BASE("m_spellcaster_base", "Заклинатель", "Открывает путь Заклинателя.", 2, 220, -420, "spellcaster", new Race[]{}, new Talent[]{MAGE_BASE}),
    M_FIRE_LIGHTNING("m_fire_lightning", "Фаербол / Молния", "Кидает фаербол. Присев — бьёт молнией по цели.", 2, 320, -560, "spellcaster", new Race[]{}, new Talent[]{M_SPELLCASTER_BASE}),
    M_ICE_ARROW("m_ice_arrow", "Ледяная стрела", "Замедляет цель сильным льдом.", 2, 420, -700, "spellcaster", new Race[]{}, new Talent[]{M_FIRE_LIGHTNING}),
    M_TELEPORT("m_teleport", "Телепорт", "Мгновенно перемещается на небольшое расстояние по направлению взгляда.", 3, 520, -840, "spellcaster", new Race[]{}, new Talent[]{M_ICE_ARROW}),

    // ======================== АССАСИН (ВНИЗ) ========================
    ASSASSIN_BASE("assassin_base", "Ассасин", "Тень и смерть", 1, 0, 300, "class", new Race[]{}, new Talent[]{START}),
    AS_SLIDE("as_slide", "Подкат", "Сближение с целью", 2, -150, 500, "assassin", new Race[]{}, new Talent[]{ASSASSIN_BASE}),
    AS_SMOKE("as_smoke", "Дымовая завеса", "Скрывает ваше положение", 2, 0, 550, "assassin", new Race[]{}, new Talent[]{ASSASSIN_BASE}),
    AS_STEALTH_CRIT("as_crit", "Крит из тени", "Удар в спину из невидимости", 2, 150, 500, "assassin", new Race[]{}, new Talent[]{ASSASSIN_BASE});

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