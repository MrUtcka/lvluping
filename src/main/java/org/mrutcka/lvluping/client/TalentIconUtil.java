package org.mrutcka.lvluping.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.data.Talent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TalentIconUtil {
    public static final ResourceLocation UNKNOWN =
            ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/skill_unknown.png");

    private static final Map<ResourceLocation, Boolean> CACHE = new ConcurrentHashMap<>();

    public static ResourceLocation icon(Talent t) {
        if (t == null) return UNKNOWN;
        ResourceLocation id = t.icon;
        Boolean ok = CACHE.get(id);
        if (ok != null) return ok ? id : UNKNOWN;
        Minecraft mc = Minecraft.getInstance();
        boolean exists = mc != null && mc.getResourceManager().getResource(id).isPresent();
        CACHE.put(id, exists);
        return exists ? id : UNKNOWN;
    }

    public static void clearCache() {
        CACHE.clear();
    }

    private TalentIconUtil() {}
}
