package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public class LocationUpdate implements Serializable {
    private String fishId;
    private InetSocketAddress newFishAddress;

    public LocationUpdate(String fishId, InetSocketAddress newFishAddress) {
        this.fishId = fishId;
        this.newFishAddress = newFishAddress;
    }

    public String getFishId() {
        return fishId;
    }

    public InetSocketAddress getNewFishAddress() {
        return newFishAddress;
    }
}
