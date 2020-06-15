package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class RegisterResponse implements Serializable {
	private final String id;
	private final int durationLease;

	public RegisterResponse(String id, int durationLease) {
		this.id = id;
		this.durationLease = durationLease;
	}

	public String getId() {
		return id;
	}

	public int getDurationLease() {
		return durationLease;
	}

}
