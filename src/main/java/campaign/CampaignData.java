package campaign;

import arc.struct.ObjectSet;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.world.Block;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CampaignData {

    public static final Map<String, Float> itemValues;
    static {
        Map<String, Float> aMap = new HashMap<>();
        aMap.put("copper", 0.5f);
        aMap.put("lead", 1f);
        aMap.put("metaglass", 1.5f);
        aMap.put("graphite", 1.5f);
        aMap.put("titanium", 1.5f);
        aMap.put("silicon", 2f);
        aMap.put("thorium", 1.5f);
        aMap.put("plastanium", 3f);
        aMap.put("phase-frabic", 5f);
        aMap.put("surge-alloy", 6f);
        itemValues = Collections.unmodifiableMap(aMap);
    }

    public static final ObjectSet<Block> tech0Banned;
    static {
        tech0Banned = ObjectSet.with(Vars.content.blocks());
        tech0Banned.remove(Blocks.copperWall);
        tech0Banned.remove(Blocks.copperWallLarge);
        tech0Banned.remove(Blocks.mechanicalDrill);
        tech0Banned.remove(Blocks.conveyor);
        tech0Banned.remove(Blocks.junction);
        tech0Banned.remove(Blocks.router);
        tech0Banned.remove(Blocks.duo);
        tech0Banned.remove(Blocks.scatter);
    }

    public static final ObjectSet<Block> tech1Banned;
    static {
        tech1Banned = ObjectSet.with(tech0Banned.asArray());
        tech1Banned.remove(Blocks.overflowGate);
        tech1Banned.remove(Blocks.underflowGate);
        tech1Banned.remove(Blocks.sorter);
        tech1Banned.remove(Blocks.invertedSorter);
        tech1Banned.remove(Blocks.itemBridge);
        tech1Banned.remove(Blocks.distributor);
        tech1Banned.remove(Blocks.hail);
        tech1Banned.remove(Blocks.arc);
        tech1Banned.remove(Blocks.scorch);
        tech1Banned.remove(Blocks.salvo);
        tech1Banned.remove(Blocks.wave);
        tech1Banned.remove(Blocks.waterExtractor);
        tech1Banned.remove(Blocks.pneumaticDrill);
        tech1Banned.remove(Blocks.conduit);
        tech1Banned.remove(Blocks.mechanicalPump);
        tech1Banned.remove(Blocks.liquidRouter);
        tech1Banned.remove(Blocks.liquidJunction);
        tech1Banned.remove(Blocks.bridgeConduit);
        tech1Banned.remove(Blocks.powerNode);
        tech1Banned.remove(Blocks.battery);
        tech1Banned.remove(Blocks.graphitePress);
        tech1Banned.remove(Blocks.kiln);
        tech1Banned.remove(Blocks.incinerator);
        tech1Banned.remove(Blocks.dartPad);
        tech1Banned.remove(Blocks.mender);
        tech1Banned.remove(Blocks.message);
    }

    public static final ObjectSet<Block> tech2Banned;
    static {
        tech2Banned = ObjectSet.with();
        tech2Banned.add(Blocks.swarmer);
        tech2Banned.add(Blocks.cyclone);
        tech2Banned.add(Blocks.spectre);
        tech2Banned.add(Blocks.meltdown);
        tech2Banned.add(Blocks.blastDrill);
        tech2Banned.add(Blocks.armoredConveyor);
        tech2Banned.add(Blocks.phaseConveyor);
        tech2Banned.add(Blocks.massDriver);
        tech2Banned.add(Blocks.platedConduit);
        tech2Banned.add(Blocks.phaseConduit);
        tech2Banned.add(Blocks.surgeTower);
        tech2Banned.add(Blocks.diode);
        tech2Banned.add(Blocks.rtgGenerator);
        tech2Banned.add(Blocks.largeSolarPanel);
        tech2Banned.add(Blocks.impactReactor);
        tech2Banned.add(Blocks.surgeWall);
        tech2Banned.add(Blocks.surgeWallLarge);
        tech2Banned.add(Blocks.plastaniumWall);
        tech2Banned.add(Blocks.plastaniumWallLarge);
        tech2Banned.add(Blocks.phaseWall);
        tech2Banned.add(Blocks.phaseWallLarge);
        tech2Banned.add(Blocks.phaseWeaver);
        tech2Banned.add(Blocks.surgeSmelter);
        tech2Banned.add(Blocks.plastaniumCompressor);
        tech2Banned.add(Blocks.revenantFactory);
        tech2Banned.add(Blocks.omegaPad);
        tech2Banned.add(Blocks.javelinPad);
        tech2Banned.add(Blocks.tridentPad);
        tech2Banned.add(Blocks.glaivePad);
        tech2Banned.add(Blocks.overdriveProjector);
    }
    public static final ObjectSet<Block> tech3Banned;

    static {
        tech3Banned = ObjectSet.with();
    }



}
