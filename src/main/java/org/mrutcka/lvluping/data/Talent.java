package org.mrutcka.lvluping.data;

import net.minecraft.resources.ResourceLocation;
import org.mrutcka.lvluping.LvlupingMod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public enum Talent {
    // КОРЕНЬ
    START("start", "Истоки", "Начало вашего пути", 0, 0, 0, "", new Race[]{}, new Talent[]{}),

    // ======================== ВОИН ========================
    WARRIOR_BASE("warrior_base", "Воин", "Открывает ветку боевых талантов и базовые умения ближнего боя.", 1, 0, 300, "class", new Race[]{}, new Talent[]{START}),
    W_STUN("w_stun", "Оглушающий удар", "Шанс при атаке наложить оглушение.", 1, -300, -100, "warrior1", new Race[]{}, new Talent[]{WARRIOR_BASE}),
    W_PARRY("w_parry", "Парирование", "Вы можете парировать атаку.", 1, 0, -200, "warrior2", new Race[]{}, new Talent[]{WARRIOR_BASE}),
    W_COMBO("w_combo", "Комбо-удары", "Накапливает серии ударов, увеличивая урон.", 1, 300, -100, "warrior3", new Race[]{}, new Talent[]{WARRIOR_BASE}),
    W_EVO("w_evo", "Эволюция", "Вы на пути к совершеству", 1, 0, -400, "warrior", new Race[]{}, new Talent[]{W_COMBO, W_PARRY, W_STUN}),

    //PALADIN
    W_PALADIN_BASE("w_paladin_base", "Паладин", "Открывает путь Паладина.", 2, -1000, -600, "warrior_subclass", new Race[]{}, new Talent[]{W_EVO}),
    W_PALADIN_SMITE("w_paladin_smite", "Кара", "Удары мечом сильнее и поджигают нежить.", 2, -1300, -600, "paladin1", new Race[]{}, new Talent[]{W_PALADIN_BASE}),
    W_PALADIN_BLESSING("w_paladin_blessing", "Благословение", "Усиливает союзника регенерацией и очищает вас от негативных эффектов.", 2, -1000, -900, "paladin1", new Race[]{}, new Talent[]{W_PALADIN_BASE}),
    W_PALADIN_SHIELD_FAITH("w_paladin_shield_faith", "Щит веры", "Блокирование щитом отражает часть урона атакующему.", 3, -1200, -800, "paladin1", new Race[]{}, new Talent[]{W_PALADIN_BASE}),
    W_PALADIN_AURA("w_paladin_aura", "Аура праведности", "Союзники рядом получают сопротивление урону.", 2, -1100, -1100, "paladin2", new Race[]{}, new Talent[]{W_PALADIN_BLESSING}),
    W_PROVOCATION("w_provocation", "Провокация", "Боевой клич заставляет ближайших врагов атаковать вас. Даёт временные жёлтые сердца (20–40% от макс. здоровья по уровню).", 3, -1400, -1000, "paladin2", new Race[]{}, new Talent[]{W_PALADIN_SHIELD_FAITH, W_PALADIN_BLESSING, W_PALADIN_SMITE}),
    W_PALADIN_HEALING_TOUCH("w_paladin_healing_touch", "Целебное касание", "Получаемое лечение эффективнее.", 2, -1500, -700, "paladin2", new Race[]{}, new Talent[]{W_PALADIN_SMITE}),
    W_PALADIN_IMMOLATION("w_paladin_immolation", "Испепеление", "Удар по земле поджигает врагов вокруг.", 2, -1700, -800, "paladin3", new Race[]{}, new Talent[]{W_PALADIN_HEALING_TOUCH}),
    W_PALADIN_PROVIDENCE("w_paladin_providence", "Божественное провидение", "Шанс снять с себя негативный эффект вскоре после получения.", 2, -1200, -1300, "paladin3", new Race[]{}, new Talent[]{W_PALADIN_AURA}),
    W_UNBREAKABLE("w_unbreakable", "Несокрушимый", "Один раз при смертельном ударе вы не погибаете.", 3, -1600, -1200, "paladin3", new Race[]{}, new Talent[]{W_PROVOCATION}),
    W_ULT_FINAL_COUNTDOWN("w_ult_final_countdown", "Судный молот", "С неба обрушивается судный молот.", 5, -1400, -1500, "paladin_ult", new Race[]{}, new Talent[]{W_PALADIN_PROVIDENCE}),
    W_ULT_PALADIN_WINGS("w_ult_paladin_wings", "Ангельские крылья", "Краткий полёт и ускорение для врыва в бой.", 5, -1900, -1000, "paladin_ult", new Race[]{}, new Talent[]{W_PALADIN_IMMOLATION}),
    W_ULT_PALADIN_SACRIFICE("w_ult_paladin_sacrifice", "Жертва", "Жертвуя здоровьем, полностью исцеляет союзников рядом.", 5, -1600, -1500, "paladin_ult", new Race[]{}, new Talent[]{W_UNBREAKABLE}),
    W_ULT_INVULNERABILITY("w_ult_invulnerability", "Неуязвимость", "Вы становитесь святым. Не получаете урон, в радиусе исцеляете союзников, даёте им щит.", 5, -1900, -1200, "paladin_ult", new Race[]{}, new Talent[]{W_UNBREAKABLE}),

    //BARBARIAN
    W_BARBARIAN_BASE("w_barbarian_base", "Варвар", "Открывает путь Варвара.", 2, 1200, -600, "warrior_subclass", new Race[]{}, new Talent[]{W_EVO}),
    W_BARBARIAN_BATTLE_CRY("w_barbarian_battle_cry", "Боевой клич", "Усиливает себя и ослабляет врагов рядом.", 2, 1200, -900, "barbarian", new Race[]{}, new Talent[]{W_BARBARIAN_BASE}),
    W_BARBARIAN_BLOODLETTING("w_barbarian_bloodletting", "Кровопускание", "Следующий удар накладывает кровотечение.", 2, 1500, -900, "barbarian", new Race[]{}, new Talent[]{W_BARBARIAN_BATTLE_CRY}),
    W_SEISMIC("w_seismic", "Сейсмический удар", "Удар по земле создаёт волну перед воином.", 2, 1050, -900, "barbarian", new Race[]{}, new Talent[]{W_BARBARIAN_BASE}),
    W_BLOODLUST("w_bloodlust", "Кровь за кровь", "Каждая пятая успешная атака восстанавливает здоровье от нанесённого урона.", 2, 1200, -1200, "barbarian", new Race[]{}, new Talent[]{W_BARBARIAN_BASE}),
    W_BARBARIAN_RAGE("w_barbarian_rage", "Ярость", "Чем меньше здоровья, тем выше наносимый урон.", 2, 1500, -1200, "barbarian", new Race[]{}, new Talent[]{W_BLOODLUST}),
    W_BARBARIAN_KILL_FRENZY("w_barbarian_kill_frenzy", "Боевое безумие", "После убийства повышает скорость атаки на короткое время.", 2, 1050, -1200, "barbarian", new Race[]{}, new Talent[]{W_BLOODLUST}),
    W_BARBARIAN_FRENZY("w_barbarian_frenzy", "Запредельная ярость", "Временно повышает урон и скорость атаки, но увеличивает входящий урон.", 2, 1800, -1200, "barbarian", new Race[]{}, new Talent[]{W_BARBARIAN_BLOODLETTING}),
    W_BARBARIAN_BLOODTHIRST("w_barbarian_bloodthirst", "Кровожадность", "При убийстве восстанавливает здоровье.", 2, 1350, -1500, "barbarian", new Race[]{}, new Talent[]{W_BARBARIAN_KILL_FRENZY}),
    W_BARBARIAN_THICK_SKIN("w_barbarian_thick_skin", "Толстая кожа", "Пассивно снижает входящий урон.", 2, 1800, -1500, "barbarian", new Race[]{}, new Talent[]{W_BARBARIAN_RAGE}),
    W_BARBARIAN_INDESTRUCTIBLE_BODY("w_barbarian_indestructible_body", "Непробиваемое тело", "Меньше лечения, но ниже входящий урон.", 3, 2100, -1500, "barbarian", new Race[]{}, new Talent[]{W_BARBARIAN_THICK_SKIN}),
    W_ULT_BERSERK("w_ult_berserk", "Берсерк", "Ярость усиливает бойца и поддерживает натиск.", 5, 1200, -1800, "barbarian_ult", new Race[]{}, new Talent[]{W_BLOODLUST, W_SEISMIC}),
    W_ULT_BARBARIAN_FEAST("w_ult_barbarian_feast", "Пиршество", "Наносит серию ударов, исцеляя себя.", 5, 1500, -1800, "barbarian_ult", new Race[]{}, new Talent[]{W_BARBARIAN_BLOODLETTING, W_BARBARIAN_FRENZY}),
    W_ULT_BARBARIAN_TASTE_BLOOD("w_ult_barbarian_taste_blood", "Вкус крови", "На время превращает урон в мощное самовосстановление.", 5, 1800, -1800, "barbarian_ult", new Race[]{}, new Talent[]{W_BARBARIAN_FRENZY, W_BARBARIAN_INDESTRUCTIBLE_BODY}),

    //SWORDMASTER
    W_SWORDMASTER_BASE("w_swordmaster_base", "Мастер меча", "Открывает путь Мастера Меча.", 2, 0, -600, "warrior_subclass", new Race[]{}, new Talent[]{W_EVO}),
    W_SPIN("w_spin", "Рассекающий удар", "Активный круговой удар по врагам рядом.", 2, 0, -900, "swordmaster", new Race[]{}, new Talent[]{W_SWORDMASTER_BASE}),
    W_SWORDMASTER_CONCENTRATION("w_swordmaster_concentration", "Концентрация", "Кратковременно увеличивает урон ударов.", 2, -300, -900, "swordmaster", new Race[]{}, new Talent[]{W_SWORDMASTER_BASE}),
    W_ARMOR_BREAKER("w_armor_breaker", "Разрез брони", "Пассивный мощный удар, который пробивает защиту цели.", 3, 300, -900, "swordmaster", new Race[]{}, new Talent[]{W_SPIN}),
    W_SWORDMASTER_STEEL_BODY("w_swordmaster_steel_body", "Стальное тело", "Временно снижает входящий урон вдвое.", 2, -600, -1200, "swordmaster", new Race[]{}, new Talent[]{W_SWORDMASTER_CONCENTRATION}),
    W_SWORDMASTER_SHARP_BLADE("w_swordmaster_sharp_blade", "Острое лезвие", "Шанс нанести удвоенный урон.", 2, 0, -1200, "swordmaster", new Race[]{}, new Talent[]{W_SWORDMASTER_CONCENTRATION}),
    W_SWORDMASTER_HARDENED_MASTERY("w_swordmaster_hardened_mastery", "Закалённое мастерство", "Пассивно увеличивает скорость атаки.", 2, -600, -1500, "swordmaster", new Race[]{}, new Talent[]{W_SWORDMASTER_STEEL_BODY}),
    W_SWORDMASTER_HAND_DEXTERITY("w_swordmaster_hand_dexterity", "Ловкость рук", "Пассивно ускоряет атаки.", 2, 0, -1500, "swordmaster", new Race[]{}, new Talent[]{W_SWORDMASTER_SHARP_BLADE}),
    W_SWORDMASTER_BALANCE("w_swordmaster_balance", "Баланс", "Без щита даёт дополнительную скорость атаки.", 2, 300, -1500, "swordmaster", new Race[]{}, new Talent[]{W_SWORDMASTER_HAND_DEXTERITY}),
    W_SWORDMASTER_AGILITY("w_swordmaster_agility", "Повышенная ловкость", "Пассивно увеличивает скорость передвижения.", 2, 600, -1500, "swordmaster", new Race[]{}, new Talent[]{W_SWORDMASTER_BALANCE}),
    W_ULT_SWORDMASTER_OMNISLASH("w_ult_swordmaster_omnislash", "Омни-слэш", "Проводит серию быстрых ударов по цели.", 5, 0, -1800, "swordmaster_ult", new Race[]{}, new Talent[]{W_ARMOR_BREAKER}),
    W_ULT_SWORDMASTER_BLADE_WALL("w_ult_swordmaster_blade_wall", "Клинковая стена", "На время сбивает все летящие в вас снаряды.", 5, -600, -1800, "swordmaster_ult", new Race[]{}, new Talent[]{W_SWORDMASTER_HARDENED_MASTERY}),
    W_ULT_SWORDMASTER_HURRICANE("w_ult_swordmaster_hurricane", "Ураган", "Резко ускоряет атаки и темп боя на короткое время.", 5, 300, -1800, "swordmaster_ult", new Race[]{}, new Talent[]{W_SWORDMASTER_BALANCE, W_ARMOR_BREAKER}),
    W_ULT_SWORDMASTER_PERFECT_CUT("w_ult_swordmaster_perfect_cut", "Идеальный разрез", "Следующий удар наносит дополнительный урон от максимального здоровья цели.", 5, 600, -1800, "swordmaster_ult", new Race[]{}, new Talent[]{W_ARMOR_BREAKER, W_SWORDMASTER_AGILITY}),

    // ======================== ЛУЧНИК ========================
    ARCHER_BASE("archer_base", "Лучник", "Меткость и ловкость", 1, 0, 300, "class", new Race[]{}, new Talent[]{START}),
    A_DASH("a_dash", "Рывок спиной", "Быстрый отскок назад", 2, -300, -300, "archer1", new Race[]{}, new Talent[]{ARCHER_BASE}),
    A_DAGGER("a_dagger", "Кинжал", "Ближний бой для стрелка", 2, 0, -450, "archer2", new Race[]{}, new Talent[]{ARCHER_BASE}),
    A_POWER("a_power", "Сильный выстрел", "Пробивает броню", 2, 300, -300, "archer3", new Race[]{}, new Talent[]{ARCHER_BASE}),
    A_EVO("a_evo", "Эволюция", "Вы на пути к совершеству", 1, 0, -650, "archer", new Race[]{}, new Talent[]{A_DASH, A_DAGGER, A_POWER}),

    //HUNTER
    A_HUNTER_BASE("a_hunter_base", "Охотник", "Открывает путь Охотника.", 2, -1200, -900, "archer_subclass", new Race[]{}, new Talent[]{A_EVO}),
    A_HUNTER_TRAP("a_hunter_trap", "Капкан", "Ставит капкан. Враг, наступивший в него, получает урон и не может двигаться.", 2, -1200, -1200, "hunter", new Race[]{}, new Talent[]{A_HUNTER_BASE}),
    A_HUNTER_CALL_NATURE("a_hunter_call_nature", "Зов природы", "Призывает временного волка.", 2, -1500, -1200, "hunter", new Race[]{}, new Talent[]{A_HUNTER_BASE}),
    A_HUNTER_POISON_ARROW("a_hunter_poison_arrow", "Отравленная стрела", "Следующий выстрел отравляет цель.", 2, -900, -1200, "hunter", new Race[]{}, new Talent[]{A_HUNTER_BASE}),
    A_HUNTER_NET("a_hunter_net", "Ловчая сеть", "Обездвиживает цель и всех рядом.", 2, -1100, -1500, "hunter", new Race[]{}, new Talent[]{A_HUNTER_TRAP, A_HUNTER_POISON_ARROW}),
    A_HUNTER_WOUNDING_SHOT("a_hunter_wounding_shot", "Ранящий выстрел", "Пассивно: выстрелы накладывают кровотечение.", 2, -1400, -1500, "hunter", new Race[]{}, new Talent[]{A_HUNTER_CALL_NATURE}),
    A_HUNTER_ESCAPE("a_hunter_escape", "Бегство", "При получении урона даёт ускорение; есть время восстановления.", 2, -1200, -1800, "hunter", new Race[]{}, new Talent[]{A_HUNTER_TRAP}),
    A_HUNTER_LIGHT_HAND("a_hunter_light_hand", "Лёгкая рука", "Есть шанс не тратить стрелы.", 2, -900, -1800, "hunter", new Race[]{}, new Talent[]{A_HUNTER_POISON_ARROW}),
    A_HUNTER_STEADY_NERVES("a_hunter_steady_nerves", "Крепкие нервы", "При прицеливании получает сопротивление урону.", 2, -1500, -1800, "hunter", new Race[]{}, new Talent[]{A_HUNTER_CALL_NATURE}),
    A_HUNTER_ANATOMY("a_hunter_anatomy", "Знание анатомии", "Выстрелы наносят больше урона.", 3, -1200, -2100, "hunter", new Race[]{}, new Talent[]{A_HUNTER_ESCAPE, A_HUNTER_LIGHT_HAND, A_HUNTER_STEADY_NERVES, A_HUNTER_WOUNDING_SHOT}),
    A_ULT_HUNTER_ULT_SHOT("a_ult_hunter_ult_shot", "Ультимативный выстрел", "Следующий выстрел наносит огромный урон.", 5, -1500, -2400, "hunter_ult", new Race[]{}, new Talent[]{A_HUNTER_ANATOMY}),
    A_ULT_HUNTER_PACK("a_ult_hunter_pack", "Стая", "Призывает 3 волков, которые атакуют цель.", 5, -1200, -2400, "hunter_ult", new Race[]{}, new Talent[]{A_HUNTER_ANATOMY}),
    A_ULT_HUNTER_SNIPER("a_ult_hunter_sniper", "Снайперский выстрел", "Требует стоять на месте для прицеливания, затем наносит огромный урон.", 5, -900, -2400, "hunter_ult", new Race[]{}, new Talent[]{A_HUNTER_ANATOMY}),
    A_ULT_HUNTER_TRACK("a_ult_hunter_track", "Выследить жертву", "Помечает цель и усиливает урон по ней, позволяет видеть сквозь стены.", 5, -1200, -2700, "hunter_ult", new Race[]{}, new Talent[]{A_ULT_HUNTER_ULT_SHOT, A_ULT_HUNTER_PACK, A_ULT_HUNTER_SNIPER}),

    //RANGER
    A_RANGER_BASE("a_ranger_base", "Рейнджер", "Открывает путь Рейнджера.", 2, 0, -900, "archer_subclass", new Race[]{}, new Talent[]{A_EVO}),
    A_RANGER_ENTANGLE_ARROW("a_ranger_entangle_arrow", "Опутывающая стрела", "Следующий выстрел сильно замедляет.", 2, 0, -1200, "ranger", new Race[]{}, new Talent[]{A_RANGER_BASE}),
    A_RANGER_EVASION("a_ranger_evasion", "Ускользание", "При получении урона есть шанс получить стак телепорта.", 2, -300, -1200, "ranger", new Race[]{}, new Talent[]{A_RANGER_BASE}),
    A_RANGER_THUNDER_ARROW("a_ranger_thunder_arrow", "Громовая стрела", "Следующий выстрел вызывает молнию и доп. урон.", 2, 300, -1200, "ranger", new Race[]{}, new Talent[]{A_RANGER_BASE}),
    A_RANGER_THORN_BUSH("a_ranger_thorn_bush", "Колючий куст", "Создаёт область шипов, замедляющую и ранящую врагов.", 2, 100, -1500, "ranger", new Race[]{}, new Talent[]{A_RANGER_ENTANGLE_ARROW, A_RANGER_THUNDER_ARROW}),
    A_RANGER_QUICK_STEP("a_ranger_quick_step", "Быстрый шаг", "Пассивно: если долго не получаете урон, получаете ускорение и усиленный первый удар.", 2, -200, -1500, "ranger", new Race[]{}, new Talent[]{A_RANGER_EVASION}),
    A_RANGER_AGILITY("a_ranger_agility", "Проворство", "Не получает урон от падения.", 2, 0, -1800, "ranger", new Race[]{}, new Talent[]{A_RANGER_ENTANGLE_ARROW}),
    A_RANGER_STURDY_BOW("a_ranger_sturdy_bow", "Крепкий лук", "Лук ломается медленнее.", 2, 300, -1800, "ranger", new Race[]{}, new Talent[]{A_RANGER_THUNDER_ARROW}),
    A_RANGER_NIMBLE_FINGERS("a_ranger_nimble_fingers", "Ловкие пальцы", "Шанс выстрелить две стрелы.", 2, -300, -1800, "ranger", new Race[]{}, new Talent[]{A_RANGER_EVASION}),
    A_RANGER_BOW_MASTERY("a_ranger_bow_mastery", "Мастер лука", "Повышенный урон стрелой в голову.", 3, 0, -2100, "ranger", new Race[]{}, new Talent[]{A_RANGER_AGILITY, A_RANGER_STURDY_BOW, A_RANGER_NIMBLE_FINGERS, A_RANGER_QUICK_STEP}),
    A_ULT_RANGER_WRATH("a_ult_ranger_wrath", "Гнев природы", "Стрела вызывает грозовую бурю в зоне поражения.", 5, -300, -2400, "ranger_ult", new Race[]{}, new Talent[]{A_RANGER_BOW_MASTERY}),
    A_ULT_RANGER_LIFE_TOTEM("a_ult_ranger_life_totem", "Тотем жизни", "Ставит тотем, который лечит союзников рядом.", 5, 0, -2400, "ranger_ult", new Race[]{}, new Talent[]{A_RANGER_THORN_BUSH}),
    A_ULT_RANGER_MERGE("a_ult_ranger_merge", "Слияние", "Превращается в дерево: не получает урон и стоит на месте, лечится.", 5, 300, -2400, "ranger_ult", new Race[]{}, new Talent[]{A_RANGER_BOW_MASTERY}),
    A_ULT_RANGER_ROOTS("a_ult_ranger_roots", "Корни", "Выращивает корни из-под врага, обездвиживая его.", 5, 0, -2700, "ranger_ult", new Race[]{}, new Talent[]{A_ULT_RANGER_WRATH, A_ULT_RANGER_LIFE_TOTEM, A_ULT_RANGER_MERGE}),

    //MUSKETEER
    A_MUSKETEER_BASE("a_musketeer_base", "Мушкетёр", "Открывает путь Мушкетёра.", 2, 1200, -900, "archer_subclass", new Race[]{}, new Talent[]{A_EVO}),
    A_MUSKETEER_QUICK_RELOAD("a_musketeer_quick_reload", "Мгновенный выстрел", "Моментально выпускает снаряд в направлении взгляда.", 2, 1200, -1200, "musketeer", new Race[]{}, new Talent[]{A_MUSKETEER_BASE}),
    A_MUSKETEER_INCENDIARY("a_musketeer_incendiary", "Зажигательная пуля", "Следующий выстрел поджигает цель.", 2, 1500, -1200, "musketeer", new Race[]{}, new Talent[]{A_MUSKETEER_BASE}),
    A_MUSKETEER_AIMED_SHOT("a_musketeer_aimed_shot", "Прицельный выстрел", "Следующий выстрел наносит больше урона.", 2, 900, -1200, "musketeer", new Race[]{}, new Talent[]{A_MUSKETEER_BASE}),
    A_MUSKETEER_HOLSTER("a_musketeer_holster", "Кобура", "Делает двойной выстрел.", 2, 1200, -1500, "musketeer", new Race[]{}, new Talent[]{A_MUSKETEER_QUICK_RELOAD}),
    A_MUSKETEER_SMOKE("a_musketeer_smoke", "Дымовая завеса", "Пассивно: при низком здоровье создаёт дым и помогает выжить.", 2, 1500, -1500, "musketeer", new Race[]{}, new Talent[]{A_MUSKETEER_INCENDIARY}),
    A_MUSKETEER_TRAINED_EYE("a_musketeer_trained_eye", "Натренированный глаз", "Цели с контролем получают больше урона от выстрелов.", 2, 900, -1800, "musketeer", new Race[]{}, new Talent[]{A_MUSKETEER_AIMED_SHOT}),
    A_MUSKETEER_FAST_HAND("a_musketeer_fast_hand", "Быстрая рука", "Пассивно увеличивает урон выстрелов.", 2, 1200, -1800, "musketeer", new Race[]{}, new Talent[]{A_MUSKETEER_QUICK_RELOAD}),
    A_MUSKETEER_STABILITY("a_musketeer_stability", "Устойчивость", "Иммунитет к оглушению и замедлению.", 2, 1500, -1800, "musketeer", new Race[]{}, new Talent[]{A_MUSKETEER_INCENDIARY}),
    A_MUSKETEER_PIERCING_BUCKSHOT("a_musketeer_piercing_buckshot", "Проникающая картечь", "Выстрел проходит сквозь врагов, нанося урон каждому.", 3, 1200, -2100, "musketeer", new Race[]{}, new Talent[]{A_MUSKETEER_TRAINED_EYE, A_MUSKETEER_FAST_HAND, A_MUSKETEER_STABILITY, A_MUSKETEER_SMOKE}),
    A_ULT_MUSKETEER_BARRAGE("a_ult_musketeer_barrage", "Пулемётная очередь", "В течение времени делает серию быстрых выстрелов.", 5, 900, -2400, "musketeer_ult", new Race[]{}, new Talent[]{A_MUSKETEER_PIERCING_BUCKSHOT}),
    A_ULT_MUSKETEER_GRENADE("a_ult_musketeer_grenade", "Гранатомёт", "Следующий выстрел взрывается при попадании.", 5, 1200, -2400, "musketeer_ult", new Race[]{}, new Talent[]{A_MUSKETEER_PIERCING_BUCKSHOT}),
    A_ULT_MUSKETEER_CONCUSSION("a_ult_musketeer_concussion", "Контузия", "Следующий выстрел накладывает слабость и обездвиживает.", 5, 1500, -2400, "musketeer_ult", new Race[]{}, new Talent[]{A_MUSKETEER_PIERCING_BUCKSHOT}),
    A_ULT_MUSKETEER_EXECUTION("a_ult_musketeer_execution", "Казнь", "Пассивно: добивает цели с низким здоровьем.", 5, 1200, -2700, "musketeer_ult", new Race[]{}, new Talent[]{A_ULT_MUSKETEER_BARRAGE, A_ULT_MUSKETEER_GRENADE, A_ULT_MUSKETEER_CONCUSSION}),

    // ======================== МАГ ========================
    MAGE_BASE("mage_base", "Маг", "Знание тайного", 1, 0, -300, "class", new Race[]{Race.MECHANID}, new Talent[]{START}),
    M_BUFF_ATK("m_buff_atk", "Усиление Атаки", "Вы усиливаете свою атаку магией", 2, 0, -450, "mage1", new Race[]{}, new Talent[]{MAGE_BASE}),
    M_BUFF_DEF("m_buff_def", "Усиление Защиты", "Повышает броню магией", 2, 0, -600, "mage2", new Race[]{}, new Talent[]{M_BUFF_ATK}),
    M_BARRIER("m_barrier", "Барьер", "Магический щит", 2, 0, -750, "mage3", new Race[]{}, new Talent[]{M_BUFF_DEF}),
    M_EVO("m_evo", "Эволюция", "Вы на пути к совершеству", 1, 0, -900, "mage", new Race[]{}, new Talent[]{M_BUFF_ATK, M_BUFF_DEF, M_BARRIER}),

    //SUMMONER
    M_SUMMONER_BASE("m_summoner_base", "Призыватель", "Открывает путь Призывателя.", 2, -600, -800, "mage_subclass", new Race[]{}, new Talent[]{M_EVO}),
    M_SUMMON_SERVANT("m_summon_servant", "Призыв слуги", "Призывает скелета-лучника.", 2, -900, -800, "summoner_choice", new Race[]{}, new Talent[]{M_SUMMONER_BASE}),
    M_SUMMON_GUARD("m_summon_guard", "Призыв стража", "Призывает зомби-воина.", 2, -600, -1100, "summoner_choice", new Race[]{}, new Talent[]{M_SUMMONER_BASE}),
    M_SUMMON_COMMAND("m_summon_command", "Команда", "Приказывает всем слугам атаковать выбранную цель.", 1, -750, -950, "summoner_add", new Race[]{}, new Talent[]{M_SUMMONER_BASE}),
    M_SUMMON_SACRIFICE("m_summon_sacrifice", "Жертва", "Убивает одного слугу, восстанавливая вам хп.", 2, -900, -1100, "summoner_add", new Race[]{}, new Talent[]{M_SUMMON_COMMAND}),
    M_SUMMON_ENDURANCE("m_summon_endurance", "Выносливость слуг", "Ваши призывы наносят больше урона.", 2, -1200, -800, "summoner", new Race[]{}, new Talent[]{M_SUMMON_SERVANT}),
    M_SUMMON_DISCIPLINE("m_summon_discipline", "Дисциплина слуг", "Ваши призывы получают больше брони.", 2, -600, -1400, "summoner", new Race[]{}, new Talent[]{M_SUMMON_GUARD}),
    M_SUMMON_EFFICIENCY("m_summon_efficiency", "Экономия маны", "Дополнительно снижает расход маны.", 2, -1200, -1400, "summoner", new Race[]{}, new Talent[]{M_SUMMON_GUARD, M_SUMMON_SERVANT}),
    M_ULT_GATE("m_ult_gate", "Врата", "Призывает сильных слуг.", 5, -900, -1700, "summoner_ult", new Race[]{}, new Talent[]{M_SUMMON_DISCIPLINE}),
    M_ULT_ABSORPTION("m_ult_absorption", "Поглощение", "Убивает ваших слуг и восстанавливает хп и маны за каждого.", 5, -600, -1700, "summoner_ult", new Race[]{}, new Talent[]{M_SUMMON_DISCIPLINE}),
    M_ULT_TOTEM_FORM("m_ult_totem_form", "Тотемная форма", "Превращает слуг в неподвижные тотемы-стрелялки.", 5, -1500, -1100, "summoner_ult", new Race[]{}, new Talent[]{M_SUMMON_ENDURANCE}),
    M_ULT_POSSESSION("m_ult_possession", "Эволюция", "Эволюционирует ваших слуг/стражей, усиливая их урон, здоровье.", 5, -1500, -1700, "summoner_ult", new Race[]{}, new Talent[]{M_SUMMON_EFFICIENCY}),
    M_ULT_ELEMENTAL("m_ult_elemental", "Элементаль", "Призывает элементаля.", 5, -1500, -800, "summoner_ult", new Race[]{}, new Talent[]{M_SUMMON_ENDURANCE}),

    //CLERIC
    M_CLERIC_BASE("m_cleric_base", "Жрец", "Открывает путь Жреца.", 2, 600, -800, "mage_subclass", new Race[]{}, new Talent[]{M_EVO}),
    M_CLERIC_SMALL_HEAL("m_cleric_small_heal", "Малый отхил", "Лечит союзника небольшим количеством хп.", 2, 800, -1000, "cleric_active_heal", new Race[]{}, new Talent[]{M_CLERIC_BASE}),
    M_CLERIC_BLESSING("m_cleric_blessing", "Благословение", "Дает союзнику усиление урона и очищает от 1 негативного эффекта.", 2, 1000, -1000, "cleric_active", new Race[]{}, new Talent[]{M_CLERIC_SMALL_HEAL}),
    M_CLERIC_LIGHT("m_cleric_light", "Свет", "Создает светящуюся сферу, которая лечит союзников и вредит нежити рядом.", 3, 800, -1200, "cleric_active", new Race[]{}, new Talent[]{M_CLERIC_SMALL_HEAL}),
    M_CLERIC_HEALING_AMP("m_cleric_healing_amp", "Усиление лечения", "Все исцеление повышено.", 2, 900, -1400, "cleric_passive1", new Race[]{}, new Talent[]{M_CLERIC_LIGHT}),
    M_CLERIC_PRAYER("m_cleric_prayer", "Аура", "Пока союзник стоит рядом, он получает небольшое лечение каждую секунду.", 2, 1200, -1100, "cleric_passive1", new Race[]{}, new Talent[]{M_CLERIC_BLESSING}),
    M_CLERIC_READ_PRAYER("m_cleric_read_prayer", "Молитва о здоровье", "Стоя на месте, начинает восстанавливать хп.", 2, 1000, -1600, "cleric_passive2", new Race[]{}, new Talent[]{M_CLERIC_HEALING_AMP}),
    M_CLERIC_PURITY("m_cleric_purity", "Чистота", "Негативные эффекты длятся меньше.", 2, 1400, -1300, "cleric_passive2", new Race[]{}, new Talent[]{M_CLERIC_PRAYER}),
    M_CLERIC_MANA_PRAYER("m_cleric_mana_prayer", "Молитва о мане", "Стоя на месте, начинает восстанавливать ману.", 2, 1250, -1500, "cleric_passive2", new Race[]{}, new Talent[]{M_CLERIC_PURITY, M_CLERIC_READ_PRAYER}),
    C_ULT_LIGHT_RAY("m_ult_light_ray", "Луч света", "Призывает столп света, который лечит союзников и вредит врагам.", 5, 1600, -1300, "cleric_ult", new Race[]{}, new Talent[]{M_CLERIC_PURITY}),
    C_ULT_RESURRECTION("m_ult_resurrection", "Божественное бессмертие", "На вас и всех союзников в радиусе накладывается неуязвость.", 5, 1000, -1900, "cleric_ult", new Race[]{}, new Talent[]{M_CLERIC_READ_PRAYER}),
    C_ULT_MARTYR("m_ult_martyr", "Мученик", "На 4 сек становится бессмертным, но не может лечиться. Дает доп. лечение союзникам", 5, 1300, -1900, "cleric_ult", new Race[]{}, new Talent[]{M_CLERIC_READ_PRAYER}),
    C_ULT_SLOW_SPHERE("m_ult_slow_sphere", "Сфера замедления", "Создает сферу, которая замедляет врагов и ускоряет союзников.", 5, 1600, -1500, "cleric_ult", new Race[]{}, new Talent[]{M_CLERIC_PURITY}),
    C_ULT_DIVINE_PROTECTION("m_ult_divine_protection", "Божественная защита", "Все союзники в радиусе получают щит.", 5, 1450, -1700, "cleric_ult", new Race[]{}, new Talent[]{M_CLERIC_MANA_PRAYER}),

    //SPELLCASTER
    M_SPELLCASTER_BASE("m_spellcaster_base", "Заклинатель", "Открывает путь Заклинателя.", 2, 0, -1500, "mage_subclass", new Race[]{}, new Talent[]{M_EVO}),
    M_FIREBALL("m_fireball", "Фаербол", "Кидает огненный шар.", 2, -300, -1600, "spell1", new Race[]{}, new Talent[]{M_SPELLCASTER_BASE}),
    M_ICE_ARROW("m_ice_arrow", "Ледяная стрела", "Замедляет цель сильным льдом.", 2, 300, -1600, "spell1", new Race[]{}, new Talent[]{M_SPELLCASTER_BASE}),
    M_LIGHTNING("m_lightning", "Молния", "Бьет молнией по цели.", 2, 0, -1700, "spell2", new Race[]{}, new Talent[]{M_FIREBALL, M_ICE_ARROW}),
    M_TELEPORT("m_teleport", "Телепорт", "Мгновенно перемещается на небольшое расстояние.", 3, -450, -1900, "spell3", new Race[]{}, new Talent[]{M_LIGHTNING}),
    M_STONE_SKIN("m_stone_skin", "Каменная кожа", "При получении урона дает сопротивление, но замедляет.", 2, 450, -1900, "spell3", new Race[]{}, new Talent[]{M_LIGHTNING}),
    M_RECHARGE("m_recharge", "Подпитка", "Ускоряет перезарядку навыков.", 2, 0, -2000, "spell3", new Race[]{}, new Talent[]{M_LIGHTNING}),
    M_MANA_FLOW("m_mana_flow", "Поток маны", "Увеличивает скорость восстонавления маны.", 2, 0, -2200, "spell4", new Race[]{}, new Talent[]{M_RECHARGE}),
    M_MAGIC_BARRIER("m_magic_barrier", "Магический барьер", "Поглащает один удар.", 2, -600, -2100, "spell4", new Race[]{}, new Talent[]{M_TELEPORT}),
    M_SOFT_LANDING("m_soft_landing", "Мягкое приземление", "Не получает урон от падения.", 2, 600, -2100, "spell4", new Race[]{}, new Talent[]{M_STONE_SKIN}),
    M_ULT_METEOR("m_ult_meteor", "Метеорит", "Призывает метеорит в область, нанося огромный урон.", 5, -800, -2300, "spellcaster_ult", new Race[]{}, new Talent[]{M_MAGIC_BARRIER}),
    M_ULT_ICE_BLOCK("m_ult_ice_block", "Ледяная глыба", "Замораживает цель в льду на 5 сек.", 5, -600, -2400, "spellcaster_ult", new Race[]{}, new Talent[]{M_MAGIC_BARRIER}),
    M_ULT_ANTI_MAGIC("m_ult_anti_magic", "Анти-магия", "Создает сферу, отражающую магию и снаряды на 5 сек.", 5, 800, -2300, "spellcaster_ult", new Race[]{}, new Talent[]{M_SOFT_LANDING}),
    M_ULT_ILLUSIONS("m_ult_illusions", "Иллюзии", "Создает копии мага, которые применяют магию по врагам.", 5, 0, -2400, "spellcaster_ult", new Race[]{}, new Talent[]{M_MANA_FLOW}),
    M_ULT_CHAOS("m_ult_chaos", "Хаос", "Выпускает волну огня, молнии и льда, накладывая эффекты.", 5, 600, -2400, "spellcaster_ult", new Race[]{}, new Talent[]{M_SOFT_LANDING}),

    // ======================== АССАСИН ========================
    ASSASSIN_BASE("assassin_base", "Ассасин", "Тень и смерть", 1, 0, -200, "class", new Race[]{}, new Talent[]{START}),
    AS_SLIDE("as_slide", "Подкат", "Сближение с целью", 1, -300, -300, "assassin1", new Race[]{}, new Talent[]{ASSASSIN_BASE}),
    AS_SMOKE("as_smoke", "Дымовая завеса", "Скрывает ваше положение", 1, 0, -450, "assassin2", new Race[]{}, new Talent[]{ASSASSIN_BASE}),
    AS_STEALTH_CRIT("as_crit", "Крит из тени", "Удар в спину из невидимости", 1, 300, -300, "assassin3", new Race[]{}, new Talent[]{ASSASSIN_BASE}),
    AS_EVO("as_evo", "Эволюция ассасина", "Открывает специализацию и ветку развития.", 1, 0, -650, "assassin", new Race[]{}, new Talent[]{AS_SLIDE, AS_SMOKE, AS_STEALTH_CRIT}),

    //ROGUE
    AS_ROGUE_BASE("as_rogue_base", "Вор", "Открывает путь Вора.", 1, -1000, -1050, "assassin_subclass", new Race[]{}, new Talent[]{AS_EVO}),
    AS_ROGUE_STRONG_POISON("as_rogue_strong_poison", "Сильный яд", "Следующая атака накладывает сильное отравление.", 2, -1300, -1300, "assassin_subclass", new Race[]{}, new Talent[]{AS_ROGUE_BASE}),
    AS_ROGUE_TRIP("as_rogue_trip", "Подсечка", "Сбивает цель с ног и оглушает.", 2, -1000, -1400, "assassin_subclass", new Race[]{}, new Talent[]{AS_ROGUE_BASE}),
    AS_ROGUE_BLIND("as_rogue_blind", "Ослепление", "Бросает песок в глаза врагу.", 2, -700, -1300, "assassin_subclass", new Race[]{}, new Talent[]{AS_ROGUE_BASE}),
    AS_ROGUE_POISON_IMMUNE("as_rogue_poison_immune", "Невосприимчивость к яду", "Яд не действует на вас.", 2, -1450, -1550, "assassin_subclass", new Race[]{}, new Talent[]{AS_ROGUE_STRONG_POISON}),
    AS_ROGUE_SLEIGHT("as_rogue_sleight", "Ловкач", "Предметы и зелья используются быстрее.", 2, -1150, -1650, "assassin_subclass", new Race[]{}, new Talent[]{AS_ROGUE_TRIP}),
    AS_ROGUE_NIGHT_EYE("as_rogue_night_eye", "Кошачье зрение", "Постоянное ночное зрение.", 2, -850, -1650, "assassin_subclass", new Race[]{}, new Talent[]{AS_ROGUE_BLIND}),
    AS_ROGUE_TRAINED("as_rogue_trained", "Натренированный", "Повышает скорость атаки.", 2, -1300, -1850, "assassin_subclass", new Race[]{}, new Talent[]{AS_ROGUE_POISON_IMMUNE}),
    AS_ROGUE_EDGE("as_rogue_edge", "На острие", "-15% макс. здоровья, но +50% урона.", 2, -1000, -1950, "assassin_subclass", new Race[]{}, new Talent[]{AS_ROGUE_SLEIGHT, AS_ROGUE_NIGHT_EYE}),
    AS_ROGUE_TIME_THIEF("as_rogue_time_thief", "Вор времени", "Смок сокращает КД навыков.", 2, -700, -1850, "assassin_subclass", new Race[]{}, new Talent[]{AS_ROGUE_NIGHT_EYE}),
    AS_ULT_ROGUE_PERFECT_KILL("as_ult_rogue_perfect_kill", "Идеальное убийство", "Уходит в невидимость, следующая атака усиливается.", 5, -1400, -2200, "assassin_subclass", new Race[]{}, new Talent[]{AS_ROGUE_TRAINED}),
    AS_ULT_ROGUE_POISON_VEIL("as_ult_rogue_poison_veil", "Ядовитая завеса", "Ядовитый смок наносит урон в зоне.", 5, -1100, -2300, "assassin_subclass", new Race[]{}, new Talent[]{AS_ROGUE_EDGE}),
    AS_ULT_ROGUE_CONFUSION("as_ult_rogue_confusion", "Замешательство", "Цель ненадолго теряет контроль.", 5, -800, -2300, "assassin_subclass", new Race[]{}, new Talent[]{AS_ROGUE_TIME_THIEF}),
    AS_ULT_ROGUE_VANISH("as_ult_rogue_vanish", "Исчезновение", "Телепорт и уход в невидимость.", 5, -500, -2200, "assassin_subclass", new Race[]{}, new Talent[]{AS_ROGUE_TIME_THIEF}),

    //WANDERER
    AS_WANDERER_BASE("as_wanderer_base", "Скиталец", "Открывает путь Скитальца", 1, 0, -1050, "assassin_subclass", new Race[]{}, new Talent[]{AS_EVO}),
    AS_WANDERER_BARRICADE("as_wanderer_barricade", "Баррикада", "Ставит деревянную преграду.", 2, -300, -1300, "assassin_subclass", new Race[]{}, new Talent[]{AS_WANDERER_BASE}),
    AS_WANDERER_CLIMB("as_wanderer_climb", "Лазанье", "Позволяет быстро карабкаться по стенам.", 2, 0, -1400, "assassin_subclass", new Race[]{}, new Talent[]{AS_WANDERER_BASE}),
    AS_WANDERER_TRIPWIRE("as_wanderer_tripwire", "Ловушка-растяжка", "Ставит растяжку, которая срабатывает по врагу.", 2, 300, -1300, "assassin_subclass", new Race[]{}, new Talent[]{AS_WANDERER_BASE}),
    AS_WANDERER_ENDURANCE("as_wanderer_endurance", "Выносливость", "Повышает скорость бега.", 2, -450, -1550, "assassin_subclass", new Race[]{}, new Talent[]{AS_WANDERER_BARRICADE}),
    AS_WANDERER_NO_SLOW("as_wanderer_no_slow", "Без задержек", "Замедление почти не действует.", 2, -150, -1650, "assassin_subclass", new Race[]{}, new Talent[]{AS_WANDERER_CLIMB}),
    AS_WANDERER_SHADOW_WRAP("as_wanderer_shadow_wrap", "Окутанный тенью", "Невидимость длится дольше.", 2, 150, -1650, "assassin_subclass", new Race[]{}, new Talent[]{AS_WANDERER_TRIPWIRE}),
    AS_WANDERER_FASTEST("as_wanderer_fastest", "Быстрее всех", "В невидимости получаете сильный бонус скорости.", 2, -300, -1850, "assassin_subclass", new Race[]{}, new Talent[]{AS_WANDERER_ENDURANCE}),
    AS_WANDERER_DOUBLE_DODGE("as_wanderer_double_dodge", "Двойное уклонение", "Подкат можно использовать дважды.", 2, 0, -1950, "assassin_subclass", new Race[]{}, new Talent[]{AS_WANDERER_NO_SLOW}),
    AS_WANDERER_KNIFE_EDGE("as_wanderer_knife_edge", "На острие ножа", "Крит из тени наносит больше урона.", 2, 300, -1850, "assassin_subclass", new Race[]{}, new Talent[]{AS_WANDERER_SHADOW_WRAP}),
    AS_ULT_WANDERER_CAMP("as_ult_wanderer_camp", "Лагерь", "Ставит палатку лечения.", 5, -400, -2200, "assassin_subclass", new Race[]{}, new Talent[]{AS_WANDERER_FASTEST}),
    AS_ULT_WANDERER_DAGGER_RAIN("as_ult_wanderer_dagger_rain", "Град кинжалов", "Серия быстрых бросков кинжалов.", 5, -100, -2300, "assassin_subclass", new Race[]{}, new Talent[]{AS_WANDERER_DOUBLE_DODGE}),
    AS_ULT_WANDERER_THORN_TRAIL("as_ult_wanderer_thorn_trail", "Колючий след", "Оставляет за собой опасный след.", 5, 200, -2300, "assassin_subclass", new Race[]{}, new Talent[]{AS_WANDERER_KNIFE_EDGE}),
    AS_ULT_WANDERER_GHOSTS("as_ult_wanderer_ghosts", "Призраки", "Призывает отвлекающих призраков.", 5, 500, -2200, "assassin_subclass", new Race[]{}, new Talent[]{AS_WANDERER_KNIFE_EDGE}),

    //ASSASSIN
    AS_ASSASSIN_BASE("as_assassin_base", "Убийца", "Открывает путь Убийцы", 1, 1000, -1050, "assassin_subclass", new Race[]{}, new Talent[]{AS_EVO}),
    AS_ASSASSIN_MARK("as_assassin_mark", "Метка", "Помечает цель и раскрывает ее.", 2, 700, -1300, "assassin_subclass", new Race[]{}, new Talent[]{AS_ASSASSIN_BASE}),
    AS_ASSASSIN_SHURIKEN("as_assassin_shuriken", "Сюрикен", "Кидает сюрикен и накладывает кровотечение.", 2, 1000, -1400, "assassin_subclass", new Race[]{}, new Talent[]{AS_ASSASSIN_BASE}),
    AS_ASSASSIN_RUPTURE("as_assassin_rupture", "Разрыв", "Взрывает кровотечение мгновенным уроном.", 2, 1300, -1300, "assassin_subclass", new Race[]{}, new Talent[]{AS_ASSASSIN_BASE}),
    AS_ASSASSIN_THROAT("as_assassin_throat", "Удар в горло", "Удары в спину накладывают кровотечение.", 2, 550, -1550, "assassin_subclass", new Race[]{}, new Talent[]{AS_ASSASSIN_MARK}),
    AS_ASSASSIN_SILENT_STEP("as_assassin_silent_step", "Бесшумный шаг", "При приседе двигается почти как обычно.", 2, 850, -1650, "assassin_subclass", new Race[]{}, new Talent[]{AS_ASSASSIN_SHURIKEN}),
    AS_ASSASSIN_BLOODLETTER("as_assassin_bloodletter", "Кровопускание", "Урон по целям с кровотечением повышен.", 2, 1150, -1650, "assassin_subclass", new Race[]{}, new Talent[]{AS_ASSASSIN_SHURIKEN}),
    AS_ASSASSIN_ADRENALINE("as_assassin_adrenaline", "Адреналин", "При низком хп дает всплеск скорости.", 2, 700, -1850, "assassin_subclass", new Race[]{}, new Talent[]{AS_ASSASSIN_THROAT}),
    AS_ASSASSIN_SHARP_BLADES("as_assassin_sharp_blades", "Острые лезвия", "Кровотечение длится дольше.", 2, 1000, -1950, "assassin_subclass", new Race[]{}, new Talent[]{AS_ASSASSIN_SILENT_STEP}),
    AS_ASSASSIN_DARK_STYLE("as_assassin_dark_style", "Тёмный стиль", "Урон повышается при ударе в спину.", 2, 1300, -1850, "assassin_subclass", new Race[]{}, new Talent[]{AS_ASSASSIN_BLOODLETTER}),
    AS_ULT_ASSASSIN_BLADE_DANCE("as_ult_assassin_blade_dance", "Танец клинков", "Серия быстрых ударов с кровотечением.", 5, 600, -2200, "assassin_subclass", new Race[]{}, new Talent[]{AS_ASSASSIN_ADRENALINE}),
    AS_ULT_ASSASSIN_IMMOBILIZE("as_ult_assassin_immobilize", "Обездвиживание", "Крюк притягивает и фиксирует цель.", 5, 900, -2300, "assassin_subclass", new Race[]{}, new Talent[]{AS_ASSASSIN_SHARP_BLADES}),
    AS_ULT_ASSASSIN_BLACK_MIST("as_ult_assassin_black_mist", "Черная дымка", "Сфера ослабляет врагов и усиливает убийцу.", 5, 1200, -2300, "assassin_subclass", new Race[]{}, new Talent[]{AS_ASSASSIN_DARK_STYLE}),
    AS_ULT_ASSASSIN_DOUBLE("as_ult_assassin_double", "Двойник", "Оставляет тень и уходит в невидимость.", 5, 1500, -2200, "assassin_subclass", new Race[]{}, new Talent[]{AS_ASSASSIN_DARK_STYLE});

    public final String id, label, description, branch;
    public final int cost, x, y;
    public final Talent[] parents;
    public final Race[] forbiddenRaces;
    public final ResourceLocation icon;

    Talent(String id, String label, String description, int cost, int x, int y, String branch, Race[] forbiddenRaces, Talent[] parents) {
        this(id, label, description, cost, x, y, branch, forbiddenRaces, parents, null);
    }

    Talent(String id, String label, String description, int cost, int x, int y, String branch, Race[] forbiddenRaces, Talent[] parents, String iconFileId) {
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

    /** Узлы «Эволюция»: нужны все базовые навыки класса (все записи в {@link #parents}), а не любой один. */
    public boolean requiresAllParents() {
        return this == W_EVO || this == A_EVO || this == M_EVO || this == AS_EVO;
    }

    /** Условие покупки по цепочке родителей (для эволюции — все родители должны быть изучены). */
    public boolean parentsSatisfiedForPurchase(Set<String> owned) {
        if (parents.length == 0) return true;
        if (requiresAllParents()) {
            return Arrays.stream(parents).allMatch(p -> owned.contains(p.id));
        }
        for (Talent p : parents) {
            if ("start".equals(p.id)) return true;
            if (owned.contains(p.id)) return true;
        }
        return false;
    }

    /** Предок в дереве родителей (цепочка parents). */
    public static boolean isAncestorOf(Talent potentialAncestor, Talent target) {
        for (Talent parent : target.parents) {
            if (parent == potentialAncestor || isAncestorOf(potentialAncestor, parent)) return true;
        }
        return false;
    }

    /** Один и тот же «столбец» дерева: предок/потомок по parents. */
    public static boolean isSameHierarchy(Talent a, Talent b) {
        return isAncestorOf(a, b) || isAncestorOf(b, a);
    }

    /**
     * Другие таланты с тем же {@link #branch}, с которыми нельзя совмещать по правилам {@code isBranchBlocked}
     * (не в одной иерархии parents).
     */
    public static List<Talent> mutuallyExclusivePeers(Talent t) {
        if (t.branch.isEmpty()) return List.of();
        ArrayList<Talent> out = new ArrayList<>();
        for (Talent o : values()) {
            if (o == t || o.branch.isEmpty()) continue;
            if (!o.branch.equals(t.branch)) continue;
            if (isSameHierarchy(t, o)) continue;
            out.add(o);
        }
        out.sort(Comparator.comparing(x -> x.label));
        return List.copyOf(out);
    }
}