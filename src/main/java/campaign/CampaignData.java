package campaign;

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

}
