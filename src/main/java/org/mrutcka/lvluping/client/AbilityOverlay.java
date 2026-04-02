package org.mrutcka.lvluping.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public class AbilityOverlay {
    public static void render(GuiGraphics guiGraphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || mc.options.hideGui || TalentScreen.clientTalents == null) return;

        int x = 10;
        int y = 10;

        if (ProvocationHintClient.isProvocationActive()) {
            int w = mc.getWindow().getGuiScaledWidth();
            int h = mc.getWindow().getGuiScaledHeight();
            String msg = net.minecraft.network.chat.Component.translatable("message.lvluping.hit_provoker").getString();
            int tw = mc.font.width(msg);
            guiGraphics.drawString(mc.font, msg, (w - tw) / 2, h / 2 - 30, 0xFF4444, true);
            guiGraphics.drawString(mc.font, msg, (w - tw) / 2, h / 2 - 30, 0xFFFFFF, false);
        }

        int combo = player.getPersistentData().getInt("lvluping_combo");
        int lastHit = player.getPersistentData().getInt("lvluping_last_hit");

        if (combo > 0 && (player.level().getGameTime() - lastHit < 40)) {
            guiGraphics.drawString(mc.font, "КОМБО: x" + combo, x, y, 0xFFAA00, true);
            y += 12;
        }

        if (TalentScreen.clientTalents.contains("as_slide")) {
            String key = ClientEvents.ABILITY_KEY_3.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_slide", "Подкат [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_smoke")) {
            String key = ClientEvents.ABILITY_KEY_4.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_smoke", "Смок [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_dash")) {
            String key = ClientEvents.ABILITY_KEY_3.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_dash", "Отскок [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_rogue_strong_poison")) {
            String key = ClientEvents.ABILITY_KEY_1.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_as_rogue_strong_poison", "Сильный яд [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_rogue_trip")) {
            String key = ClientEvents.ABILITY_KEY_2.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_as_rogue_trip", "Подсечка [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_rogue_blind")) {
            String key = ClientEvents.ABILITY_KEY_3.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_as_rogue_blind", "Ослепление [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_wanderer_barricade")) {
            String key = ClientEvents.ABILITY_KEY_1.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_as_wanderer_barricade", "Баррикада [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_wanderer_climb")) {
            String key = ClientEvents.ABILITY_KEY_2.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_as_wanderer_climb", "Лазанье [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_wanderer_tripwire")) {
            String key = ClientEvents.ABILITY_KEY_3.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_as_wanderer_tripwire", "Растяжка [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_assassin_mark")) {
            String key = ClientEvents.ABILITY_KEY_1.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_as_assassin_mark", "Метка [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_assassin_shuriken")) {
            String key = ClientEvents.ABILITY_KEY_2.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_as_assassin_shuriken", "Сюрикен [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_assassin_adrenaline")) {
            renderAbility(guiGraphics, mc, player, "cd_as_assassin_adrenaline", "Адреналин (пассив)", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_assassin_rupture")) {
            String key = ClientEvents.ABILITY_KEY_3.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_as_assassin_rupture", "Разрыв [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_hunter_trap")) {
            String key = ClientEvents.ABILITY_KEY_1.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_a_hunter_trap", "Капкан [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_hunter_call_nature")) {
            String key = ClientEvents.ABILITY_KEY_2.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_a_hunter_call_nature", "Зов природы [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_hunter_poison_arrow")) {
            renderAbility(guiGraphics, mc, player, "cd_a_hunter_poison_arrow", "Отравленная стрела", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_hunter_net")) {
            String key = ClientEvents.ABILITY_KEY_4.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_a_hunter_net", "Ловчая сеть [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_hunter_escape")) {
            renderAbility(guiGraphics, mc, player, "cd_a_hunter_escape", "Бегство", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_ranger_entangle_arrow")) {
            String key = ClientEvents.ABILITY_KEY_1.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_a_ranger_entangle_arrow", "Опут. стрела [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_ranger_evasion")) {
            String key = ClientEvents.ABILITY_KEY_2.getTranslatedKeyMessage().getString();
            int evStacks = player.getPersistentData().getInt("lvluping_ranger_evasion_stacks");
            String evLabel = "Ускользание [" + key + "]";
            if (evStacks > 0) {
                evLabel += "  ×" + evStacks;
            }
            renderAbility(guiGraphics, mc, player, "cd_a_ranger_evasion", evLabel, x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_ranger_thunder_arrow")) {
            renderAbility(guiGraphics, mc, player, "cd_a_ranger_thunder_arrow", "Гром. стрела (пассив)", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_ranger_thorn_bush")) {
            String key = ClientEvents.ABILITY_KEY_4.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_a_ranger_thorn_bush", "Колючий куст [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_musketeer_quick_reload")) {
            String key = ClientEvents.ABILITY_KEY_1.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_a_musketeer_quick_reload", "Быстр. перезарядка [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_musketeer_incendiary")) {
            String key = ClientEvents.ABILITY_KEY_2.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_a_musketeer_incendiary", "Зажиг. пуля [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_musketeer_aimed_shot")) {
            String key = ClientEvents.ABILITY_KEY_3.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_a_musketeer_aimed_shot", "Приц. выстрел [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_musketeer_holster")) {
            String key = ClientEvents.ABILITY_KEY_4.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_a_musketeer_holster", "Кобура [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_seismic")) {
            String key = ClientEvents.ABILITY_KEY_3.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_seismic", "Сейсмический удар [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_spin")) {
            String key = ClientEvents.ABILITY_KEY_3.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_spin", "Рассекающий удар [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_swordmaster_concentration")) {
            String key = ClientEvents.ABILITY_KEY_1.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_swordmaster_concentration", "Концентрация [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_swordmaster_steel_body")) {
            String key = ClientEvents.ABILITY_KEY_2.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_swordmaster_steel_body", "Стальное тело [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_barbarian_battle_cry")) {
            String key = ClientEvents.ABILITY_KEY_1.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_barbarian_battle_cry", "Боевой клич [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_barbarian_bloodletting")) {
            String key = ClientEvents.ABILITY_KEY_2.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_barbarian_bloodletting", "Кровопускание [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_barbarian_frenzy")) {
            String key = ClientEvents.ABILITY_KEY_2.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_barbarian_frenzy", "Запредельная ярость [Shift+" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_parry")) {
            String key = ClientEvents.ABILITY_KEY_1.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_parry", "Парирование [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_barrier")) {
            String key = ClientEvents.ABILITY_KEY_1.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_buff", "Усиление [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_fireball")) {
            String key = ClientEvents.ABILITY_KEY_1.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_fireball", "Фаербол [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_lightning")) {
            String key = ClientEvents.ABILITY_KEY_2.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_lightning", "Молния [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_ice_arrow")) {
            String key = ClientEvents.ABILITY_KEY_3.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_ice", "Ледяная стрела [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_cleric_small_heal")) {
            String key = ClientEvents.ABILITY_KEY_1.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_cleric_heal", "Малый отхил [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_cleric_blessing")) {
            String key = ClientEvents.ABILITY_KEY_2.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_cleric_blessing", "Благословение [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_cleric_light")) {
            String key = ClientEvents.ABILITY_KEY_3.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_cleric_light", "Свет [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_teleport")) {
            String key = ClientEvents.ABILITY_KEY_4.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_teleport", "Телепорт [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_summon_servant")) {
            String key = ClientEvents.ABILITY_KEY_1.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_summon", "Призыв слуги [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_summon_guard")) {
            String key = ClientEvents.ABILITY_KEY_1.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_summon", "Призыв стража [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_summon_sacrifice")) {
            String key = ClientEvents.ABILITY_KEY_2.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_sacrifice", "Жертва [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_summon_command")) {
            String key = ClientEvents.ABILITY_KEY_3.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_command", "Команда [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_paladin_blessing")) {
            String key = ClientEvents.ABILITY_KEY_1.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_paladin_blessing", "Благословение [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_paladin_immolation")) {
            String key = ClientEvents.ABILITY_KEY_2.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_paladin_immolation", "Испепеление [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_unbreakable")) {
            renderAbility(guiGraphics, mc, player, "cd_w_unbreakable", "Несокрушимый", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_armor_breaker")) {
            renderAbility(guiGraphics, mc, player, "cd_w_armor_breaker", "Разрез брони", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_provocation")) {
            String key = ClientEvents.ABILITY_KEY_4.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_provocation", "Провокация [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_ult_berserk")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_ult_berserk", "Берсерк [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_ult_swordmaster_hurricane")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_ult_swordmaster_hurricane", "Ураган [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_ult_swordmaster_omnislash")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_ult_swordmaster_omnislash", "Омни-слэш [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_ult_swordmaster_blade_wall")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_ult_swordmaster_blade_wall", "Клинковая стена [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_ult_swordmaster_perfect_cut")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_ult_swordmaster_perfect_cut", "Идеальный разрез [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_ult_barbarian_taste_blood")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_ult_barbarian_taste_blood", "Вкус крови [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_ult_barbarian_feast")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_ult_barbarian_feast", "Пиршество [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_ult_final_countdown")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_ult_final_countdown", "Судный молот [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_ult_invulnerability")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_ult_invulnerability", "Неуязвимость [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_ult_paladin_wings")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_ult_paladin_wings", "Ангельские крылья [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_ult_paladin_sacrifice")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_ult_paladin_sacrifice", "Жертва [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_ult_rogue_perfect_kill")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_as_ult_rogue_perfect_kill", "Идеальное убийство [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_ult_rogue_poison_veil")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_as_ult_rogue_poison_veil", "Ядовитая завеса [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_ult_rogue_confusion")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_as_ult_rogue_confusion", "Замешательство [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_ult_rogue_vanish")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_as_ult_rogue_vanish", "Исчезновение [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_ult_wanderer_camp")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_as_ult_wanderer_camp", "Лагерь [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_ult_wanderer_dagger_rain")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_as_ult_wanderer_dagger_rain", "Град кинжалов [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_ult_wanderer_thorn_trail")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_as_ult_wanderer_thorn_trail", "Колючий след [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_ult_wanderer_ghosts")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_as_ult_wanderer_ghosts", "Призраки [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_ult_assassin_blade_dance")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_as_ult_assassin_blade_dance", "Танец клинков [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_ult_assassin_immobilize")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_as_ult_assassin_immobilize", "Обездвиживание [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_ult_assassin_black_mist")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_as_ult_assassin_black_mist", "Черная дымка [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_ult_assassin_double")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_as_ult_assassin_double", "Двойник [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_ult_hunter_ult_shot")) {
            renderAbility(guiGraphics, mc, player, "cd_a_ult_hunter_ult_shot", "Ульт. выстрел", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_ult_hunter_pack")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_a_ult_hunter_pack", "Стая [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_ult_hunter_sniper")) {
            renderAbility(guiGraphics, mc, player, "cd_a_ult_hunter_sniper", "Снайперский выстрел", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_ult_hunter_track")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_a_ult_hunter_track", "Выследить [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_ult_ranger_wrath")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_a_ult_ranger_wrath", "Гнев природы [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_ult_ranger_life_totem")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_a_ult_ranger_life_totem", "Тотем жизни [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_ult_ranger_merge")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_a_ult_ranger_merge", "Слияние [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_ult_ranger_roots")) {
            renderAbility(guiGraphics, mc, player, "cd_a_ult_ranger_roots", "Корни (след. выстрел)", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_ult_musketeer_barrage")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_a_ult_musketeer_barrage", "Очередь [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_ult_musketeer_grenade")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_a_ult_musketeer_grenade", "Гранатомёт [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_ult_musketeer_concussion")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_a_ult_musketeer_concussion", "Контузия [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_ult_musketeer_execution")) {
            renderAbility(guiGraphics, mc, player, "cd_a_ult_musketeer_execution", "Казнь (пассив)", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_ult_gate")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_ult_gate", "Врата [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_ult_absorption")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_ult_absorption", "Поглощение [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_ult_totem_form")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_ult_totem_form", "Тотемная форма [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_ult_possession")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_ult_possession", "Эволюция [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_ult_elemental")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_ult_elemental", "Элементаль [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_ult_meteor")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_ult_meteor", "Метеорит [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_ult_ice_block")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_ult_ice_block", "Ледяная глыба [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_ult_anti_magic")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_ult_anti_magic", "Анти-магия [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_ult_illusions")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_ult_illusions", "Иллюзии [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_ult_chaos")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_ult_chaos", "Хаос [" + key + "]", x, y);
            y += 12;
        }

        // --- Жрец (Жрец/Cleric) ---
        if (TalentScreen.clientTalents.contains("m_ult_light_ray")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_ult_light_ray", "Луч света [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_ult_resurrection")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_ult_resurrection", "Божественное бессмертие [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_ult_martyr")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_ult_martyr", "Мученик [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_ult_slow_sphere")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_ult_slow_sphere", "Сфера замедления [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_ult_divine_protection")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_ult_divine_protection", "Божественная защита [" + key + "]", x, y);
            y += 12;
        }
    }

    private static void renderPassiveLine(GuiGraphics graphics, Minecraft mc, String label, int x, int y) {
        graphics.pose().pushPose();
        float scale = 0.6f;
        graphics.pose().scale(scale, scale, scale);
        float scaledX = x / scale;
        float scaledY = y / scale;
        graphics.drawString(mc.font, label, (int) scaledX, (int) scaledY, 0x55FF55, true);
        graphics.pose().popPose();
    }

    private static void renderAbility(GuiGraphics graphics, Minecraft mc, Player player, String nbtKey, String label, int x, int y) {
        int currentTicks = player.getPersistentData().getInt(nbtKey);

        graphics.pose().pushPose();

        float scale = 0.6f;
        graphics.pose().scale(scale, scale, scale);

        float scaledX = x / scale;
        float scaledY = y / scale;

        if (currentTicks > 0) {
            float secondsLeft = currentTicks / 20.0f;
            String text = String.format("%s: %.1f сек", label, secondsLeft);
            graphics.drawString(mc.font, text, (int) scaledX, (int) scaledY, 0xFF5555, true);
        } else {
            graphics.drawString(mc.font, label + ": ГОТОВО", (int) scaledX, (int) scaledY, 0x55FF55, true);
        }

        graphics.pose().popPose();
    }
}