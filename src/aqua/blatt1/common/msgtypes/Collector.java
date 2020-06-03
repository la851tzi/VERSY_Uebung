package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

public class Collector implements Serializable {

    private int globalFishCounter;

    public Collector(int globalFishCounter) {
        this.globalFishCounter = globalFishCounter;
    }

    public void setGlobalFishCounter(int globalFishCounter) {
        this.globalFishCounter = globalFishCounter;
    }

    public int getGlobalFishCounter() {
        return globalFishCounter;
    }

}
