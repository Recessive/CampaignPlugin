package campaign;

import mindustry.entities.type.Player;
import mindustry.game.Team;

public class CustomPlayer {

    protected Player player;
    protected int playTime;
    public boolean connected;
    public int donateLevel = 0;
    public int eventCalls = 0;


    public CustomPlayer(Player player, int playTime){
        this.player = player;
        this.playTime = playTime;
        this.connected = true;
    }



}
