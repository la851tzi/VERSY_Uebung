package aqua.blatt1.common.msgtypes;

import aqua.blatt1.common.FishModel;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public class LocationRequest implements Serializable {
    private String fish;

    public LocationRequest(String fish) {
        this.fish = fish;
    }

    public String getFish() {
        return fish;
    }
}
