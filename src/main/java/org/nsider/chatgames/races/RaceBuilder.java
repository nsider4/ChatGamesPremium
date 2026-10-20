package org.nsider.chatgames.races;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.races.setup.RaceDataLoader;
import org.nsider.chatgames.races.setup.RaceMessageSender;
import org.nsider.chatgames.races.setup.RaceStarter;
import org.nsider.chatgames.races.validator.EntityValidator;
import org.nsider.chatgames.races.validator.MaterialValidator;
import org.nsider.chatgames.utils.management.TimeUtils;

import java.util.HashMap;
import java.util.List;

public class RaceBuilder {
    private final ChatGames plugin;
    private final RaceUtils raceUtils;
    private final MaterialValidator materialValidator;
    private final EntityValidator entityValidator;
    private final RaceStarter raceStarter;
    private final RaceDataLoader raceDataLoader;
    @Getter private final RaceMessageSender raceMessageSender;

    @Getter @Setter private String currentRace = "none";

    @Setter @Getter private HashMap<String, List<Integer>> huntData = new HashMap<>(),
            placeData = new HashMap<>(),
            mineData = new HashMap<>(),
            fishData = new HashMap<>(),
            eatData = new HashMap<>(),
            craftData = new HashMap<>(),
            furnaceData = new HashMap<>();

    private RaceType raceType;
    @Getter @Setter private Race race;

    public RaceBuilder(ChatGames plugin) {
        this.plugin = plugin;
        this.raceUtils = new RaceUtils(plugin);
        this.materialValidator = new MaterialValidator(plugin);
        this.entityValidator = new EntityValidator(plugin);
        this.raceStarter = new RaceStarter(plugin, materialValidator, entityValidator, raceUtils);
        this.raceDataLoader = new RaceDataLoader(plugin);
        this.raceMessageSender = new RaceMessageSender(plugin, raceUtils);
    }

    public void startRace(GameType type) {
        switch (type) {
            case HUNT:
                raceStarter.startEntityTypeRace("hunt", getHuntData());
                break;
            case MINE:
                raceStarter.startMaterialTypeRace("mine", getMineData());
                break;
            case PLACE:
                raceStarter.startMaterialTypeRace("place", getPlaceData());
                break;
            case FISH:
                raceStarter.startMaterialTypeRace("fish", getFishData());
                break;
            case EAT:
                raceStarter.startMaterialTypeRace("eat", getEatData());
                break;
            case CRAFT:
                raceStarter.startMaterialTypeRace("craft", getCraftData());
                break;
            case FURNACE:
                raceStarter.startMaterialTypeRace("furnace", getFurnaceData());
        }
    }

    public void startCustomRace(GameType type, Material material, int value) {
        switch (type) {
            case MINE:
                raceStarter.startRaceWithMaterial(getMineData(), "mine", material, value);
                break;
            case PLACE:
                raceStarter.startRaceWithMaterial(getPlaceData(), "place", material, value);
                break;
            case FISH:
                raceStarter.startRaceWithMaterial(getFishData(), "fish", material, value);
                break;
            case EAT:
                raceStarter.startRaceWithMaterial(getEatData(), "eat", material, value);
                break;
            case CRAFT:
                raceStarter.startRaceWithMaterial(getCraftData(), "craft", material, value);
                break;
            case FURNACE:
                raceStarter.startRaceWithMaterial(getFurnaceData(), "furnace", material, value);
        }
    }

    public void endRace() {
        RaceHandler.removePlayersOnline();
        TimeUtils.stopTimer();
        this.currentRace = "";
        plugin.setInGame(false);
        plugin.getExpirationTask().cancel();
    }
}
