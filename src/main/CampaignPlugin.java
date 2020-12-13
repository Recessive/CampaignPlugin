package main;

import arc.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.GameState;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.blocks.storage.CoreBlock;

import java.util.HashMap;
import java.util.Random;
import java.util.prefs.Preferences;

import static mindustry.Vars.*;
import static mindustry.Vars.netServer;

public class CampaignPlugin extends Plugin {

    private Random rand = new Random(System.currentTimeMillis());

    private Preferences prefs;
    private int launchWave;
    private int techLevel;
    private int wave = 0;
    private boolean finalWave = false;
    private int deadUnits = 0;

    private final Rules rules = new Rules();
    private int currMap;
    private mindustry.maps.Map loadedMap;
    private String mapID;

    private HashMap<String, CustomPlayer> uuidMapping = new HashMap<>();

    private final DBInterface mapDB = new DBInterface("map_data");
    private final DBInterface playerDB = new DBInterface("player_data");

    @Override
    public void init(){

        mapDB.connect("../network-files/campaign_data.db");
        playerDB.connect(mapDB.conn);

        init_rules();

        Events.on(EventType.WaveEvent.class, event ->{

            for(Player player : Groups.player){
                playerDB.safePut(player.uuid(), "xp", (int) playerDB.safeGet(player.uuid(), "xp") + 30*(player.donateLevel + 1));
                player.sendMessage("[accent]+[scarlet]" + 30*(player.donateLevel + 1) + "[accent] xp for surviving");
            }
            wave ++;
            if(wave == launchWave){
                Call.sendMessage("[scarlet]LAUNCH WAVE!\n[accent]Survive this to be able to launch");
                finalWave = true;
                state.rules.waitEnemies = true;
                state.rules.waveSpacing = 5000;
            }else if((launchWave - wave) % 5 == 0){
                Call.sendMessage("[scarlet]" + (launchWave - wave) + "[accent] waves remain");
            }
        });

        Events.on(EventType.PlayerJoinSecondary.class, event ->{
            event.player.sendMessage(motd());
            if(!playerDB.hasRow(event.player.uuid())){
                playerDB.addRow(event.player.uuid());
            }
            playerDB.loadRow(event.player.uuid());

            if(!uuidMapping.containsKey(event.player.uuid())){
                int xp = (int) playerDB.safeGet(event.player.uuid(), "xp");
                uuidMapping.put(event.player.uuid(), new CustomPlayer(event.player, xp));
            }

            event.player.name = StringHandler.determineRank((int) playerDB.safeGet(event.player.uuid(), "xp")) + " " + event.player.name;

        });

        Events.on(EventType.UnitDestroyEvent.class, event ->{
            boolean allDead = true;
            for(Unit unit : Groups.unit){
                if(event.unit == unit){
                    continue;
                }
                if(unit.team() == Team.crux){
                    allDead = false;
                    break;
                }
            }
            if(event.unit.team() == Team.crux && allDead && finalWave) endgame(true);
            if(event.unit.team() == Team.crux){
                deadUnits ++;
                if(deadUnits == 100){
                    for(Player player : Groups.player){
                        playerDB.safePut(player.uuid(), "xp", (int) playerDB.safeGet(player.uuid(), "xp") + 10*(player.donateLevel + 1));
                        player.sendMessage("[accent]+[scarlet]" + 10*(player.donateLevel + 1) + "[accent] xp for killing 100 units");
                    }
                    deadUnits = 0;
                }
            }

        });

        Events.on(EventType.BlockDestroyEvent.class, event ->{
            if(event.tile.block() instanceof CoreBlock && event.tile.team().cores().size == 1){
                endgame(false);
            }

        });

        Events.on(EventType.PlayerLeave.class, event -> {
            savePlayerData(event.player.uuid());
        });

        Events.on(EventType.TapEvent.class, event ->{
            if(event.tile.block() == Blocks.vault && event.tile.team() != Team.purple){
                if(event.tile.build.items.has(Items.thorium, 997)){
                    if(uuidMapping.get(event.player.uuid()).coresLeft < 1){
                        event.player.sendMessage("[accent]You can only place 1 core shard per game!");
                        return;
                    }
                    uuidMapping.get(event.player.uuid()).coresLeft -= 1;
                    event.tile.build.tile.setNet(Blocks.coreShard, event.tile.team(), 0);
                    event.player.sendMessage("[accent]You placed a core shard! " +
                            "(by filling a vault with thorium and tapping/clicking it)");
                }
            }
        });

        Events.on(EventType.CustomEvent.class, event ->{
            if(event.value instanceof String[] && ((String[]) event.value)[0].equals("newName")){
                String[] val = (String[]) event.value;
                Player ply = uuidMapping.get(val[1]).player;
                ply.name = StringHandler.determineRank((int) playerDB.safeGet(val[1],"xp")) + " " + ply.name;
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
            String[] values = state.map.description().replaceAll("\\s+","").split(",");
            launchWave = Integer.parseInt(values[0]);
            techLevel = Integer.parseInt(values[1]);
            switch(techLevel){
                case 0: rules.bannedBlocks = CampaignData.tech0Banned; rules.loadout = CampaignData.tech0loadout; break;
                case 1: rules.bannedBlocks = CampaignData.tech1Banned; rules.loadout = CampaignData.tech1loadout; break;
                case 2: rules.bannedBlocks = CampaignData.tech2Banned; rules.loadout = CampaignData.tech2loadout; break;
                case 3: rules.bannedBlocks = CampaignData.tech3Banned; rules.loadout = CampaignData.tech3loadout; break;
            }
            rules.spawns = state.map.rules().spawns;
            rules.waveSpacing = state.map.rules().waveSpacing;
            /*rules.launchWaveMultiplier = 3;
            rules.bossWaveMultiplier = 3;*/

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

        handler.register("nextmap", "<map>", "End the game and force start the specified map.", args -> {
            endgame(false);
            prefs.putInt("mapchoice", Integer.parseInt(args[0]));
            Log.info("Game ended with next map " + args[0]);

        });

        handler.register("setxp", "<uuid> <xp>", "Set the xp of a player", args -> {
            int newXp;
            try{
                newXp = Integer.parseInt(args[1]);
            }catch(NumberFormatException e){
                Log.info("Invalid xp input '" + args[1] + "'");
                return;
            }

            if(!playerDB.entries.containsKey(args[0])){
                playerDB.loadRow(args[0]);
                playerDB.safePut(args[0],"xp", newXp);
                playerDB.saveRow(args[0]);
            }else{
                playerDB.safePut(args[0],"xp", newXp);
            }
            Log.info("Set uuid " + args[0] + " to have xp of " + args[1]);

        });
    }

    public void registerClientCommands(CommandHandler handler) {

        handler.<Player>register("info", "Display info about the gamemode", (args, player) -> {
            player.sendMessage("[#4d004d]{[purple]AA[#4d004d]}[sky]Campaign [accent] is essentially Survival but" +
                    " there are tech levels and you win by launching at a certain wave.\n\n" +
                    "There are [scarlet]4[accent] tech levels:\n" +
                    "[gold] 0.[accent]Tutorial level, only duos and scatters\n" +
                    "[gold] 1.[accent]Early game, basic power and draug miners\n" +
                    "[gold] 2.[accent]Mid game, Everything except plastanium, phase and surge\n" +
                    "[gold] 3.[accent]End game, all technology available\n");
        });


        handler.<Player>register("xp", "[sky]Show your xp", (args, player) -> {
            int xp = (int) playerDB.safeGet(player.uuid(), "xp");

            String s = "[scarlet]" + xp + "[accent] xp\n";/*nGet [scarlet]" + leftover + "[accent] more xp for 1 additional ";
            if(leftover < 10000){
                s += "[lime]Level 1";
            }else if (leftover < 20000){
                s += "[acid]Level 2";
            } else{
                s += "[green]Level 3";
            }
            s += "[accent] boost!\n";*/

            String nextRank = StringHandler.determineRank(xp+10000);
            player.sendMessage(s + "Reach [scarlet]" + (xp/10000+1)*10000 + "[accent] xp to reach " + nextRank + "[accent] rank.");

        });

        handler.<Player>register("stats", "Display the stats for this map", (args, player) -> {
            player.sendMessage("[gold]All time score record: [scarlet]" + mapDB.safeGet(mapID, "allRecord") +
                    "\n[accent]Monthly score record: [scarlet]" + mapDB.safeGet(mapID, "monthRecord") +
                    "\n[accent]Total times beaten: [scarlet]" + mapDB.safeGet(mapID, "wins") +
                    "\n[accent]Total times failed: [scarlet]" + mapDB.safeGet(mapID, "losses"));
        });

        handler.<Player>register("waves", "Show how many waves remain", (args, player) -> {
            player.sendMessage("[scarlet]" + (launchWave - wave) + "[accent] waves remain");
        });

        handler.<Player>register("score", "Display the teams current score", (args, player) -> {
            player.sendMessage("[gold]Score: [scarlet]" + calculateScore());
        });

        handler.<Player>register("start", "[sky]Start the next wave (donators only)", (args, player) -> {
            if(player.donateLevel < 1){
                player.sendMessage("[accent]Only donators have access to this command");
                return;
            }

            boolean allDead = true;
            for(Unit unit : Groups.unit){
                if(unit.team() == Team.crux){
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
        rules.waitEnemies = false;
        // rules.enemyCheat = true;
        rules.waves = true;
        rules.buildSpeedMultiplier = 1.5f;
        rules.canGameOver = false;
        rules.buildSpeedMultiplier = 2;
    }


    void endgame(boolean win){
        Log.info("Ending the game...");
        if(win){

            for(Player player : Groups.player){
                playerDB.safePut(player.uuid(), "xp", (int) playerDB.safeGet(player.uuid(), "xp") + 500*(player.donateLevel + 1));
                player.sendMessage("[accent]+[scarlet]" + 500*(player.donateLevel + 1) + "[accent] xp for escaping");
            }

            int score = calculateScore();
            int nextMap = currMap;
            while(nextMap == currMap) {
                nextMap = rand.nextInt(maps.customMaps().size - 1);
            }

            prefs.putInt("mapchoice", nextMap);
            String s = "";

            // TODO: ADD A ZERO TO THE XP BONUS' FOR RECORDS AFTER A MONTH

            if(score > (int) mapDB.safeGet(mapID, "allRecord")){
                mapDB.safePut(mapID, "allRecord", score);
                mapDB.safePut(mapID, "monthRecord", score);
                s += "[gold]New all time score record!\n\n";
                for(Player player : Groups.player){
                    playerDB.safePut(player.uuid(), "xp", (int) playerDB.safeGet(player.uuid(), "xp") + 500*(player.donateLevel + 1));
                    player.sendMessage("[gold]+[scarlet]" + 500*(player.donateLevel + 1) + "[gold] xp for setting an all time record!");
                }
            }else if(score > (int) mapDB.safeGet(mapID, "monthRecord")){
                mapDB.safePut(mapID, "monthRecord", score);
                s += "[acid]New monthly score record!\n\n";
                for(Player player : Groups.player){
                    playerDB.safePut(player.uuid(), "xp", (int) playerDB.safeGet(player.uuid(), "xp") + 100*(player.donateLevel + 1));
                    player.sendMessage("[accent]+[scarlet]" + 100*(player.donateLevel + 1) + "[accent] xp for setting a monthly record!");
                }
            }
            s += "[green]Congratulations! You survived.\n[accent]All time score record: [pink]" + mapDB.safeGet(mapID, "allRecord") +
            "\n[accent]Month score record: [scarlet]" + mapDB.safeGet(mapID, "monthRecord");
            s += "\n[accent]Score: [scarlet]" + score;

            mapDB.safePut(mapID, "wins", (int) mapDB.safeGet(mapID, "wins") + 1);


            Call.infoMessage(s);
        }else{
            int nextMap = currMap;
            while(nextMap == currMap) {
                nextMap = rand.nextInt(maps.customMaps().size - 1);
            }

            prefs.putInt("mapchoice", nextMap);
            mapDB.safePut(mapID, "losses", (int) mapDB.safeGet(mapID, "losses") + 1);
            Call.infoMessage("[scarlet]Bad luck! You died.");
        }



        Time.runTask(60f * 20f, () -> {
            mapDB.saveRow(mapID);

            for(Player player : Groups.player) {
                Call.connect(player.con, "aamindustry.play.ai", 6567);
            }


            // I shouldn't need this, all players should be gone since I connected them to hub
            // netServer.kickAll(KickReason.serverRestarting);
            Log.info("Game ended successfully.");
            Time.runTask(60f*2, () -> System.exit(2));
        });
    }

    void savePlayerData(String uuid){
        Log.info("Saving " + uuid + " data...");
        if(playerDB.entries.containsKey(uuid)){
            playerDB.saveRow(uuid);
        }

    }

    int calculateScore(){
        int score = 0;
        for(CoreBlock.CoreBuild core: Team.sharded.cores()){
            for(Item i: content.items()){
                if(CampaignData.itemValues.containsKey(i.name)) score += core.items.get(i) * CampaignData.itemValues.get(i.name);
            }
        }
        return score;
    }

    String motd(){
        String ret = "[accent]Welcome to [#4d004d]{[purple]AA[#4d004d]} [sky]Campaign!\n[accent]Map name: [white]" + loadedMap.name() +
         "\n[accent]Author: [white]" + loadedMap.author() + "\n[accent]Tech level: [scarlet]" + techLevel + "\n\n[gold]All time score record: [scarlet]" + mapDB.safeGet(mapID, "allRecord") +
                "\n[accent]Monthly score record: [scarlet]" + mapDB.safeGet(mapID, "monthRecord") + "\n[accent]Survive until wave [scarlet]" +
                launchWave + "[accent] to launch and win!";
        return ret;

    }
}
