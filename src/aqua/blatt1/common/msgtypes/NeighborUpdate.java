package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public final class NeighborUpdate implements Serializable {
    private InetSocketAddress leftNeighbor;
    private InetSocketAddress rightNeighbor;

    public NeighborUpdate(InetSocketAddress leftNeighbor, InetSocketAddress rightNeighbor) {
        this.leftNeighbor = leftNeighbor;
        this.rightNeighbor = rightNeighbor;
    }

    public InetSocketAddress getLeftNeighbor() {
        return this.leftNeighbor;
    }

    public InetSocketAddress getRightNeighbor() {
        return this.rightNeighbor;
    }
}
