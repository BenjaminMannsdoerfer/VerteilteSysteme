package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class LocationUpdate implements Serializable {
    private final String fishId;
    private final InetSocketAddress address;

    public LocationUpdate(String fishId, InetSocketAddress address) {
        this.address = address;
        this.fishId = fishId;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public String getFishId() {
        return fishId;
    }
}
