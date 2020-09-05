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
import mindustry.type.ItemStack;

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

    @Override
    public void init(){

        init_rules();

        Events.on(EventType.WaveEvent.class, event ->{
            wave ++;
            if(wave == launchWave){
                Call.sendMessage("[scarlet]FINAL WAVE!\n[accent]Survive this to progress to the next map");
                finalWave = true;
            }
        });

        Events.on(EventType.UnitDestroyEvent.class, event ->{
            boolean endgame = true;
            for(Unit unit : unitGroup.all()){
                if(unit.getTeam() == Team.crux){
                    endgame = false;
                    break;
                }
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
            int currMap = prefs.getInt("mapchoice",0);
            prefs.putInt("mapchoice", 0); // This is just backup so the server reverts to first map if a map crashes

            mindustry.maps.Map map = maps.customMaps().get(currMap);
            world.loadMap(map);
            String[] values = world.getMap().description().split(",");
            launchWave = Integer.parseInt(values[0]);
            techLevel = Integer.parseInt(values[1]);
            rules.spawns = world.getMap().rules().spawns;

            Log.info("Map loaded.");

            // Create cells objects

            state.rules = rules.copy();
            logic.play();

            netServer.openServer();

        });
    }

    void init_rules(){
        rules.waitForWaveToEnd = false;
        rules.respawnTime = 0;
        rules.enemyCheat = true;
        rules.waves = true;
        rules.waveSpacing = 20 * 60;
        rules.launchWaveMultiplier = 2;
        rules.bossWaveMultiplier = 3;

    }

    void endgame(boolean win){
        if(win){
            Call.onInfoMessage("[green]Congratulations! You survived.");
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
}
