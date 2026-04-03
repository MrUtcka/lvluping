package org.mrutcka.lvluping.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.network.C2SPossessionInput;
import org.mrutcka.lvluping.network.C2SUseAbility;

public class ClientEvents {

    public static final KeyMapping TALENT_KEY = new KeyMapping(
            "key.lvluping.talents",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            "key.categories.lvluping"
    );

    public static final KeyMapping ABILITY_KEY_1 = new KeyMapping(
            "key.lvluping.ability1",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_X,
            "key.categories.lvluping"
    );

    public static final KeyMapping ABILITY_KEY_2 = new KeyMapping(
            "key.lvluping.ability2",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_C,
            "key.categories.lvluping"
    );

    public static final KeyMapping ABILITY_KEY_3 = new KeyMapping(
            "key.lvluping.ability3",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_V,
            "key.categories.lvluping"
    );

    public static final KeyMapping ABILITY_KEY_4 = new KeyMapping(
            "key.lvluping.ability4",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_B,
            "key.categories.lvluping"
    );

    public static final KeyMapping ABILITY_KEY_5 = new KeyMapping(
            "key.lvluping.ability5",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            "key.categories.lvluping"
    );

    /** Шестой слот: третья способность подкласса ассасина (ослепление / растяжка / разрыв), без пересечения с ультом (G). */
    public static final KeyMapping ABILITY_KEY_6 = new KeyMapping(
            "key.lvluping.ability6",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            "key.categories.lvluping"
    );

    @EventBusSubscriber(modid = LvlupingMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(TALENT_KEY);
            event.register(ABILITY_KEY_1);
            event.register(ABILITY_KEY_2);
            event.register(ABILITY_KEY_3);
            event.register(ABILITY_KEY_4);
            event.register(ABILITY_KEY_5);
            event.register(ABILITY_KEY_6);
        }
    }

    @EventBusSubscriber(modid = LvlupingMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
    public static class ForgeBusEvents {
        @SubscribeEvent
        public static void onRenderGui(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
            float partialTick = event.getPartialTick().getGameTimeDeltaTicks();
            AbilityOverlay.render(event.getGuiGraphics(), partialTick);
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (TALENT_KEY.consumeClick() && Minecraft.getInstance().screen == null) {
                if (TalentScreen.getChosenTopClassBaseIdClient() == null) Minecraft.getInstance().setScreen(new ClassSelectScreen());
                else Minecraft.getInstance().setScreen(new TalentScreen());
            }
            while (ABILITY_KEY_1.consumeClick()) {
                PacketDistributor.sendToServer(new C2SUseAbility(0));
            }
            while (ABILITY_KEY_2.consumeClick()) {
                PacketDistributor.sendToServer(new C2SUseAbility(1));
            }
            while (ABILITY_KEY_3.consumeClick()) {
                PacketDistributor.sendToServer(new C2SUseAbility(2));
            }
            while (ABILITY_KEY_4.consumeClick()) {
                PacketDistributor.sendToServer(new C2SUseAbility(3));
            }
            while (ABILITY_KEY_5.consumeClick()) {
                PacketDistributor.sendToServer(new C2SUseAbility(4));
            }
            while (ABILITY_KEY_6.consumeClick()) {
                PacketDistributor.sendToServer(new C2SUseAbility(5));
            }

            var player = Minecraft.getInstance().player;
            if (player != null) {
                tickCooldownDisplay(player, "cd_slide");
                tickCooldownDisplay(player, "cd_smoke");
                tickCooldownDisplay(player, "cd_dash");
                tickCooldownDisplay(player, "cd_parry");
                tickCooldownDisplay(player, "cd_buff");
                tickCooldownDisplay(player, "cd_w_seismic");
                tickCooldownDisplay(player, "cd_w_spin");
                tickCooldownDisplay(player, "cd_w_unbreakable");
                tickCooldownDisplay(player, "cd_w_armor_breaker");
                tickCooldownDisplay(player, "cd_w_swordmaster_concentration");
                tickCooldownDisplay(player, "cd_w_swordmaster_steel_body");
                tickCooldownDisplay(player, "cd_w_barbarian_battle_cry");
                tickCooldownDisplay(player, "cd_w_barbarian_bloodletting");
                tickCooldownDisplay(player, "cd_w_barbarian_frenzy");
                tickCooldownDisplay(player, "cd_w_provocation");
                tickCooldownDisplay(player, "cd_w_paladin_blessing");
                tickCooldownDisplay(player, "cd_w_paladin_immolation");
                tickCooldownDisplay(player, "cd_w_ult_paladin_wings");
                tickCooldownDisplay(player, "cd_w_ult_paladin_sacrifice");
                tickCooldownDisplay(player, "cd_w_ult_berserk");
                tickCooldownDisplay(player, "cd_w_ult_final_countdown");
                tickCooldownDisplay(player, "cd_w_ult_invulnerability");
                tickCooldownDisplay(player, "cd_w_ult_swordmaster_hurricane");
                tickCooldownDisplay(player, "cd_w_ult_barbarian_taste_blood");
                tickCooldownDisplay(player, "cd_w_ult_swordmaster_omnislash");
                tickCooldownDisplay(player, "cd_w_ult_swordmaster_blade_wall");
                tickCooldownDisplay(player, "cd_w_ult_swordmaster_perfect_cut");
                tickCooldownDisplay(player, "cd_w_ult_barbarian_feast");
                tickCooldownDisplay(player, "cd_m_fireball");
                tickCooldownDisplay(player, "cd_m_lightning");
                tickCooldownDisplay(player, "cd_m_ice");
                tickCooldownDisplay(player, "cd_m_teleport");
                tickCooldownDisplay(player, "cd_m_summon");
                tickCooldownDisplay(player, "cd_m_sacrifice");
                tickCooldownDisplay(player, "cd_m_command");
                tickCooldownDisplay(player, "cd_m_cleric_heal");
                tickCooldownDisplay(player, "cd_m_cleric_blessing");
                tickCooldownDisplay(player, "cd_m_cleric_light");
                tickCooldownDisplay(player, "cd_m_ult_gate");
                tickCooldownDisplay(player, "cd_m_ult_absorption");
                tickCooldownDisplay(player, "cd_m_ult_totem_form");
                tickCooldownDisplay(player, "cd_m_ult_possession");
                tickCooldownDisplay(player, "cd_m_ult_elemental");
                tickCooldownDisplay(player, "cd_m_stone_skin");
                tickCooldownDisplay(player, "cd_m_magic_barrier");
                tickCooldownDisplay(player, "cd_m_ult_meteor");
                tickCooldownDisplay(player, "cd_m_ult_ice_block");
                tickCooldownDisplay(player, "cd_m_ult_anti_magic");
                tickCooldownDisplay(player, "cd_m_ult_illusions");
                tickCooldownDisplay(player, "cd_m_ult_chaos");
                tickCooldownDisplay(player, "cd_m_ult_light_ray");
                tickCooldownDisplay(player, "cd_m_ult_resurrection");
                tickCooldownDisplay(player, "cd_m_ult_martyr");
                tickCooldownDisplay(player, "cd_m_ult_slow_sphere");
                tickCooldownDisplay(player, "cd_m_ult_divine_protection");

                tickCooldownDisplay(player, "cd_a_hunter_trap");
                tickCooldownDisplay(player, "cd_a_hunter_call_nature");
                tickCooldownDisplay(player, "cd_a_hunter_poison_arrow");
                tickCooldownDisplay(player, "cd_a_hunter_net");
                tickCooldownDisplay(player, "cd_a_hunter_escape");

                tickCooldownDisplay(player, "cd_a_ranger_entangle_arrow");
                tickCooldownDisplay(player, "cd_a_ranger_evasion");
                tickCooldownDisplay(player, "cd_a_ranger_thunder_arrow");
                tickCooldownDisplay(player, "cd_a_ranger_thorn_bush");

                tickCooldownDisplay(player, "cd_a_musketeer_quick_reload");
                tickCooldownDisplay(player, "cd_a_musketeer_incendiary");
                tickCooldownDisplay(player, "cd_a_musketeer_aimed_shot");
                tickCooldownDisplay(player, "cd_a_musketeer_holster");

                tickCooldownDisplay(player, "cd_a_ult_hunter_ult_shot");
                tickCooldownDisplay(player, "cd_a_ult_hunter_pack");
                tickCooldownDisplay(player, "cd_a_ult_hunter_sniper");
                tickCooldownDisplay(player, "cd_a_ult_hunter_track");

                tickCooldownDisplay(player, "cd_a_ult_ranger_wrath");
                tickCooldownDisplay(player, "cd_a_ult_ranger_life_totem");
                tickCooldownDisplay(player, "cd_a_ult_ranger_merge");
                tickCooldownDisplay(player, "cd_a_ult_ranger_roots");

                tickCooldownDisplay(player, "cd_a_ult_musketeer_barrage");
                tickCooldownDisplay(player, "cd_a_ult_musketeer_grenade");
                tickCooldownDisplay(player, "cd_a_ult_musketeer_concussion");
                tickCooldownDisplay(player, "cd_as_rogue_strong_poison");
                tickCooldownDisplay(player, "cd_as_rogue_trip");
                tickCooldownDisplay(player, "cd_as_rogue_blind");
                tickCooldownDisplay(player, "cd_as_wanderer_barricade");
                tickCooldownDisplay(player, "cd_as_wanderer_climb");
                tickCooldownDisplay(player, "cd_as_wanderer_tripwire");
                tickCooldownDisplay(player, "cd_as_assassin_mark");
                tickCooldownDisplay(player, "cd_as_assassin_shuriken");
                tickCooldownDisplay(player, "cd_as_assassin_rupture");
                tickCooldownDisplay(player, "cd_as_assassin_adrenaline");
                tickCooldownDisplay(player, "cd_as_ult_rogue_perfect_kill");
                tickCooldownDisplay(player, "cd_as_ult_rogue_poison_veil");
                tickCooldownDisplay(player, "cd_as_ult_rogue_confusion");
                tickCooldownDisplay(player, "cd_as_ult_rogue_vanish");
                tickCooldownDisplay(player, "cd_as_ult_wanderer_camp");
                tickCooldownDisplay(player, "cd_as_ult_wanderer_dagger_rain");
                tickCooldownDisplay(player, "cd_as_ult_wanderer_thorn_trail");
                tickCooldownDisplay(player, "cd_as_ult_wanderer_ghosts");
                tickCooldownDisplay(player, "cd_as_ult_assassin_blade_dance");
                tickCooldownDisplay(player, "cd_as_ult_assassin_immobilize");
                tickCooldownDisplay(player, "cd_as_ult_assassin_black_mist");
                tickCooldownDisplay(player, "cd_as_ult_assassin_double");

                long until = player.getPersistentData().getLong("lvluping_possession_until");
                if (until > player.level().getGameTime() && player.getPersistentData().hasUUID("lvluping_possession_mob")) {
                    var input = player.input;
                    PacketDistributor.sendToServer(new C2SPossessionInput(
                            input.forwardImpulse,
                            input.leftImpulse,
                            input.jumping,
                            Minecraft.getInstance().options.keyAttack.isDown(),
                            player.getYRot(),
                            player.getXRot()
                    ));
                }
            }
            JudgementHammerClient.tick();
            HunterTrapClient.tick();
            MergeTreeClient.tick();
            ThornBushClient.tick();
            LifeTotemClient.tick();
            RootsTargetClient.tick();
            AssassinBarricadeClient.tick();
            AssassinTripwireClient.tick();
            AssassinCampClient.tick();
        }

        private static void tickCooldownDisplay(net.minecraft.world.entity.player.Player player, String key) {
            int val = player.getPersistentData().getInt(key);
            if (val > 0) {
                player.getPersistentData().putInt(key, val - 1);
            }
        }
    }
}