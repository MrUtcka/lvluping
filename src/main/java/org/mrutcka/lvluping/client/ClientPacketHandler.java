package org.mrutcka.lvluping.client;

import net.minecraft.client.Minecraft;
import org.mrutcka.lvluping.data.Race;
import org.mrutcka.lvluping.network.S2CSyncTalents;

public class ClientPacketHandler {
    public static void handleSync(S2CSyncTalents msg) {
        TalentScreen.clientLevel = msg.level();
        TalentScreen.clientStars = msg.stars();
        TalentScreen.clientTalents = msg.talents();
        TalentScreen.clientStats = msg.stats();
        TalentScreen.clientAbilityLevels = msg.abilityLevels();
        TalentScreen.clientRace = Race.getById(msg.raceId());

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ClassSelectScreen && TalentScreen.getChosenTopClassBaseIdClient() != null) {
            mc.execute(() -> {
                if (mc.screen instanceof ClassSelectScreen) {
                    mc.setScreen(new TalentScreen());
                }
            });
        }
    }
}
