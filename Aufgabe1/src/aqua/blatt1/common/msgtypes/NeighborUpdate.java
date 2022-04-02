package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public class NeighborUpdate implements Serializable {
    private final InetSocketAddress leftNeighborAddress;
    private final InetSocketAddress rightNeighborAddress;

    public NeighborUpdate(InetSocketAddress leftNeighborAddress, InetSocketAddress rightNeighborAddress) {
        this.leftNeighborAddress = leftNeighborAddress;
        this.rightNeighborAddress = rightNeighborAddress;
    }

    public InetSocketAddress getLeftNeighborAddress() {
        return leftNeighborAddress;
    }

    public InetSocketAddress getRightNeighborAddress() {
        return rightNeighborAddress;
    }
}
