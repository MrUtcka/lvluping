package org.mrutcka.lvluping.client;


public final class ProvocationHintClient {
    private static boolean provocationActive = false;

    public static void setProvocationActive(boolean active) {
        provocationActive = active;
    }

    public static boolean isProvocationActive() {
        return provocationActive;
    }
}
