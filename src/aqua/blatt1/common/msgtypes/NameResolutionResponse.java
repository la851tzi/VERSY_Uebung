package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public class NameResolutionResponse implements Serializable {
    private InetSocketAddress addressTankId;
    private String requestId;
    private InetSocketAddress newFishAddress;

    public NameResolutionResponse(InetSocketAddress addressTankId, String requestId, InetSocketAddress newFishAddress) {
        this.addressTankId = addressTankId;
        this.requestId = requestId;
        this.newFishAddress = newFishAddress;
    }

    public InetSocketAddress getAddressTankId() {
        return addressTankId;
    }

    public String getRequestId() {
        return requestId;
    }

    public InetSocketAddress getNewFishAddress() {
        return newFishAddress;
    }
}
