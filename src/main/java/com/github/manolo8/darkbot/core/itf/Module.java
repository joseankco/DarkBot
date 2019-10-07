package com.github.manolo8.darkbot.core.itf;

import com.github.manolo8.darkbot.Main;

public interface Module extends Installable, Tickable, RefreshHandler {

    void install(Main main);

    boolean canRefresh();

    void tick();

    default String status() {
        return null;
    }

    default String stoppedStatus() {
        return null;
    }

}
