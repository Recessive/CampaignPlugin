package main;

import mindustry.game.Team;
import mindustry.gen.Player;

public class CustomPlayer {

    protected Player player;
    public boolean connected;
    public int boost1Calls = 0;
    public int boost2Calls = 0;
    public int boost3Calls = 0;
    public int coresLeft = 1;


    public CustomPlayer(Player player, int xp){
        this.player = player;
        this.connected = true;
        int sets = xp / 60000;
        int leftover = xp % 60000;
        boost1Calls += sets;
        boost1Calls += sets;
        boost3Calls += sets;
        if(leftover > 10000){
            boost1Calls += 1;
        }
        if(leftover > 30000){
            boost2Calls += 1;
        }
    }



}
