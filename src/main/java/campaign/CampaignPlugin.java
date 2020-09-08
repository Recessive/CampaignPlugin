package campaign;

import arc.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.GameState;
import mindustry.entities.type.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.plugin.Plugin;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.blocks.storage.CoreBlock;

import java.util.prefs.Preferences;

import static mindustry.Vars.*;
import static mindustry.Vars.netServer;

public class CampaignPlugin extends Plugin{

    private Preferences prefs;
    private int launchWave;
    private int techLevel;
    private int wave = 0;
    private boolean finalWave = false;

    private final Rules rules = new Rules();
    private int currMap;

    @Override
    public void init(){

        init_rules();

        Events.on(EventType.WaveEvent.class, event ->{
            wave ++;
            if(wave == launchWave){
                Call.sendMessage("[scarlet]FINAL WAVE!\n[accent]Survive this to progress to the next map");
                finalWave = true;
            }else if((launchWave - wave) % 5 == 0){
                Call.sendMessage("[scarlet]" + (launchWave - wave) + "[accent] waves remain");
            }
        });

        Events.on(EventType.UnitDestroyEvent.class, event ->{
            boolean allDead = true;
            for(Unit unit : unitGroup.all()){
                if(event.unit == unit){
                    continue;
                }
                if(unit.getTeam() == Team.crux){
                    allDead = false;
                    break;
                }
            }
            if(allDead && finalWave) endgame(true);

        });

        Events.on(EventType.BlockDestroyEvent.class, event ->{
            if(event.tile.block() instanceof CoreBlock && event.tile.getTeam().cores().size == 1){
                endgame(false);
            }

        });
    }


    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("campaign", "Begin hosting the Campaign gamemode.", args -> {
            if (!Vars.state.is(GameState.State.menu)) {
                Log.err("Stop the server first.");
                return;
            }

            prefs = Preferences.userRoot().node(this.getClass().getName());
            currMap = prefs.getInt("mapchoice",0);
            prefs.putInt("mapchoice", currMap > 1 ? currMap-1 : 1); // This is just backup so the server reverts to previous map if a map crashes

            mindustry.maps.Map map = maps.customMaps().get(currMap);
            world.loadMap(map);
            String[] values = world.getMap().description().replaceAll("\\s+","").split(",");
            launchWave = Integer.parseInt(values[0]);
            //techLevel = Integer.parseInt(values[1]);
            techLevel = 3;
            switch(techLevel){
                case 0: rules.bannedBlocks = CampaignData.tech0Banned; break;
                case 1: rules.bannedBlocks = CampaignData.tech1Banned; break;
                case 2: rules.bannedBlocks = CampaignData.tech2Banned; break;
                case 3: rules.bannedBlocks = CampaignData.tech3Banned; break;
            }
            rules.spawns = world.getMap().rules().spawns;

            Log.info("Map " + map.name() + " loaded");

            // Create cells objects

            state.rules = rules.copy();
            logic.play();

            netServer.openServer();

            prefs.putInt("mapchoice", currMap);

        });

        handler.register("crash", "<name/uuid>", "Crashes the name/uuid", args ->{
            for(Player player : playerGroup.all()){
                if(player.uuid.equals(args[0]) || Strings.stripColors(player.name).equals(args[0])){
                    player.sendMessage(null);
                    Log.info("Done.");
                    return;
                }
            }
            Log.info("Player not found!");
        });
    }

    public void registerClientCommands(CommandHandler handler) {

        // Register the re-rank command
        handler.<Player>register("start", "[sky]Start the next wave (donators only)", (args, player) -> {
            boolean allDead = true;
            for(Unit unit : unitGroup.all()){
                if(unit.getTeam() == Team.crux){
                    allDead = false;
                    break;
                }
            }
            if(allDead){
                Call.sendMessage(player.name + "[accent] force started the next wave!");
                logic.runWave();
            }else{
                player.sendMessage("[accent]Can not start the next wave until previous wave is cleared!");
            }

        });
    }


    void init_rules(){
        rules.waitForWaveToEnd = false;
        rules.respawnTime = 5 * 60;
        rules.enemyCheat = true;
        rules.waves = true;
        rules.waveSpacing = 60 * 60;
        rules.launchWaveMultiplier = 3;
        rules.bossWaveMultiplier = 3;
        rules.buildSpeedMultiplier = 1.5f;
        rules.canGameOver = false;
    }

    void endgame(boolean win){
        if(win){
            int score = calculateScore();
            prefs.putInt("mapchoice", currMap+1);
            Call.onInfoMessage("[green]Congratulations! You survived.\n[accent]Score: [scarlet]" + score);
        }else{
            Call.onInfoMessage("[scarlet]Bad luck! You died.");
        }

        Time.runTask(60f * 10f, () -> {

            for(Player player : playerGroup.all()) {
                Call.onConnect(player.con, "aamindustry.play.ai", 6567);
            }


            // I shouldn't need this, all players should be gone since I connected them to hub
            // netServer.kickAll(KickReason.serverRestarting);
            Log.info("Game ended successfully.");
            Time.runTask(60f*2, () -> System.exit(2));
        });
    }

    int calculateScore(){
        int score = 0;
        for(CoreBlock.CoreEntity core: Team.sharded.cores()){
            for(Item i: content.items()){
                if(CampaignData.itemValues.containsKey(i.name)) score += core.items.get(i) * CampaignData.itemValues.get(i.name);
            }
        }
        return score;
    }
}
