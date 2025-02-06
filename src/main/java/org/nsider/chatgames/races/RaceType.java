package org.nsider.chatgames.races;

import org.nsider.chatgames.races.race.*;

public enum RaceType {
    HUNT(HuntRace.class),
    MINE(MineRace.class),
    PLACE(PlaceRace.class),
    FISH(FishRace.class),
    EAT(EatRace.class),
    CRAFT(CraftRace.class),
    FURNACE(FurnaceRace.class);

    private final Class<? extends Race> raceClass;

    private RaceType(Class<? extends Race> raceClass) {
        this.raceClass = raceClass;
    }

    public Class<?> getRaceClass() {
        return raceClass;
    }
}
