package campaign;

public class StringHandler {
    public static String determineRank(int xp){
        switch(xp / 50000){
            case 0: return "[accent]<[white]\uF8AE1[accent]>";
            case 1: return "[accent]<[white]\uF8AE2[accent]>";
            case 2: return "[accent]<[white]\uF8AE3[accent]>";
            case 3: return "[accent]<[white]\uF8AC1[accent]>";
            case 4: return "[accent]<[white]\uF8AC2[accent]>";
            case 5: return "[accent]<[white]\uF8AC3[accent]>";
            case 6: return "[accent]<[white]\uF8A81[accent]>";
            case 7: return "[accent]<[white]\uF8A82[accent]>";
            case 8: return "[accent]<[white]\uF8A83[accent]>";
            case 9: return "[accent]<[lime]\uF8A6[white]1[accent]>";
            case 10: return "[accent]<[lime]\uF8A6[white]2[accent]>";
            case 11: return "[accent]<[lime]\uF8A6[white]3[accent]>";
            case 12: return "[accent]<[yellow]\uF8A4[white]1[accent]>";
            case 13: return "[accent]<[yellow]\uF8A4[white]2[accent]>";
            case 14: return "[accent]<[yellow]\uF8A4[white]3[accent]>";
        }
        return "[accent]<[#00FFFF]Survivalist[accent]>[white]";
    }
}
