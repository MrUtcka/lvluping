package org.mrutcka.lvluping.client;

import org.mrutcka.lvluping.data.Race;
import org.mrutcka.lvluping.network.S2CSyncTalents;

public class ClientPacketHandler {
    public static void handleSync(S2CSyncTalents msg) {
        TalentScreen.clientLevel = msg.level();
        TalentScreen.clientStars = msg.stars();
        TalentScreen.clientTalents = msg.talents();
        TalentScreen.clientStats = msg.stats();
        TalentScreen.clientRace = Race.getById(msg.raceId());
    }
}