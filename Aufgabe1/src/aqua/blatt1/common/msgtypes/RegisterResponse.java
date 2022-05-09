package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class RegisterResponse implements Serializable {
	private final String id;
	private final Integer leaseTime;

	public RegisterResponse(String id, Integer leaseTime) {
		this.id = id;
		this.leaseTime = leaseTime;
	}

	public RegisterResponse(String id) {
		this.id = id;
		this.leaseTime = 10000;
	}

	public String getId() {
		return id;
	}

	public Integer getLeaseTime() {
		return leaseTime;
	}

}
