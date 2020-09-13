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

import java.util.Random;
import java.util.prefs.Preferences;

import static mindustry.Vars.*;
import static mindustry.Vars.netServer;

public class CampaignPlugin extends Plugin{

    private Random rand = new Random(System.currentTimeMillis());

    private Preferences prefs;
    private int launchWave;
    private int techLevel;
    private int wave = 0;
    private boolean finalWave = false;

    private final Rules rules = new Rules();
    private int currMap;
    private mindustry.maps.Map loadedMap;
    private String mapID;

    private final DBInterface mapDB = new DBInterface("map_data");

    @Override
    public void init(){

        mapDB.connect("data/server_data.db");

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

        Events.on(EventType.PlayerJoinSecondary.class, event ->{
            event.player.sendMessage(motd());
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
        handler.register("campaign", "[map]", "Begin hosting the Campaign gamemode.", args -> {
            if (!Vars.state.is(GameState.State.menu)) {
                Log.err("Stop the server first.");
                return;
            }

            prefs = Preferences.userRoot().node(this.getClass().getName());
            currMap = prefs.getInt("mapchoice",0);
            int i = 0;
            for(mindustry.maps.Map map : maps.customMaps()){
                Log.info(i + ": " + map.name());
                i += 1;
            }

            if(args.length != 0){
                currMap = Integer.parseInt(args[0]);
            }


            mindustry.maps.Map map = maps.customMaps().get(currMap);
            Log.info("Loading map " + map.name());
            world.loadMap(map);
            loadedMap = map;
            String[] values = world.getMap().description().replaceAll("\\s+","").split(",");
            launchWave = Integer.parseInt(values[0]);
            techLevel = Integer.parseInt(values[1]);
            switch(techLevel){
                case 0: rules.bannedBlocks = CampaignData.tech0Banned; rules.loadout = CampaignData.tech0loadout; break;
                case 1: rules.bannedBlocks = CampaignData.tech1Banned; rules.loadout = CampaignData.tech1loadout; break;
                case 2: rules.bannedBlocks = CampaignData.tech2Banned; rules.loadout = CampaignData.tech2loadout; break;
                case 3: rules.bannedBlocks = CampaignData.tech3Banned; rules.loadout = CampaignData.tech3loadout; break;
            }
            rules.spawns = world.getMap().rules().spawns;
            rules.waveSpacing = world.getMap().rules().waveSpacing;
            rules.launchWaveMultiplier = 3;
            rules.bossWaveMultiplier = 3;

            Log.info("Map " + map.name() + " loaded");

            // Create cells objects

            state.rules = rules.copy();
            logic.play();

            netServer.openServer();

            prefs.putInt("mapchoice", currMap);
            mapID = map.file.name().split("_")[0];
            if(!mapDB.hasRow(mapID)){
                mapDB.addRow(mapID);
            }
            mapDB.loadRow(mapID);
        });
    }

    public void registerClientCommands(CommandHandler handler) {

        // Register the re-rank command
        handler.<Player>register("start", "[sky]Start the next wave (donators only)", (args, player) -> {
            if(player.donateLevel < 1){
                player.sendMessage("[accent]Only donators have access to this command");
                return;
            }

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

        handler.<Player>register("stats", "Display the stats for this map", (args, player) -> {
            player.sendMessage("[gold]All time score record: [scarlet]" + mapDB.safeGet(mapID, "allRecord") +
                    "\n[accent]Monthly score record: [scarlet]" + mapDB.safeGet(mapID, "monthRecord") +
                    "\n[accent]Total times beaten: [scarlet]" + mapDB.safeGet(mapID, "wins") +
                    "\n[accent]Total times failed: [scarlet]" + mapDB.safeGet(mapID, "losses"));
        });

        handler.<Player>register("score", "Display the teams current score", (args, player) -> {
            player.sendMessage("[gold]Score: [scarlet]" + calculateScore());
        });

    }


    void init_rules(){
        rules.waitForWaveToEnd = false;
        rules.respawnTime = 5 * 60;
        rules.enemyCheat = true;
        rules.waves = true;
        rules.buildSpeedMultiplier = 1.5f;
        rules.canGameOver = false;
    }

    void endgame(boolean win){
        if(win){

            int score = calculateScore();
            int nextMap = currMap;
            while(nextMap == currMap){
                nextMap = rand.nextInt(maps.customMaps().size-1);
            }
            prefs.putInt("mapchoice", nextMap);
            String s = "";
            if(score > (int) mapDB.safeGet(mapID, "allRecord")){
                mapDB.safePut(mapID, "allRecord", score);
                s += "[gold]New all time score record!\n\n";
            }else
            if(score > (int) mapDB.safeGet(mapID, "monthRecord")){
                mapDB.safePut(mapID, "monthRecord", score);
                s += "[acid]New monthly score record!\n\n";
            }
            s += "[green]Congratulations! You survived.\n[accent]All time score record: [pink]" + mapDB.safeGet(mapID, "allRecord") +
            "\n[accent]Month score record: [scarlet]" + mapDB.safeGet(mapID, "allRecord");
            s += "\n[accent]Score: [scarlet]" + score;

            mapDB.safePut(mapID, "wins", (int) mapDB.safeGet(mapID, "wins") + 1);


            Call.onInfoMessage(s);
        }else{
            mapDB.safePut(mapID, "losses", (int) mapDB.safeGet(mapID, "losses") + 1);
            Call.onInfoMessage("[scarlet]Bad luck! You died.");
        }

        mapDB.saveRow(mapID);

        Time.runTask(60f * 20f, () -> {

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

    String motd(){
        String ret = "[accent]Welcome to [#4d004d]{[purple]AA[#4d004d]} [sky]Campaign!\n[accent]Map name: [white]" + loadedMap.name() +
         "\n[accent]Author: [white]" + loadedMap.author() + "\n\n[gold]All time score record: [scarlet]" + mapDB.safeGet(mapID, "allRecord") +
                "\n[accent]Monthly score record: [scarlet]" + mapDB.safeGet(mapID, "monthRecord") + "\n[accent]Survive until wave [scarlet]" +
                launchWave + "[accent] to launch and win!";
        return ret;

    }
}
