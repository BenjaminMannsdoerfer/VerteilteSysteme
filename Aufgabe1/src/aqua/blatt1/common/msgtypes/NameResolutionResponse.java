package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class NameResolutionResponse implements Serializable {
    private final String requestId;
    private final InetSocketAddress address;

    public NameResolutionResponse(InetSocketAddress address, String requestId) {
        this.address = address;
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }

    public InetSocketAddress getAddress() {
        return address;
    }
}
