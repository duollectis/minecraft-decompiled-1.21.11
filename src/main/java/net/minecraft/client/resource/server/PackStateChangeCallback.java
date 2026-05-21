package net.minecraft.client.resource.server;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.UUID;

@Environment(EnvType.CLIENT)
/**
 * {@code PackStateChangeCallback}.
 */
public interface PackStateChangeCallback {

	void onStateChanged(UUID id, PackStateChangeCallback.State state);

	void onFinish(UUID id, PackStateChangeCallback.FinishState state);

	@Environment(EnvType.CLIENT)
	/**
	 * {@code FinishState}.
	 */
	public static enum FinishState {
		DECLINED,
		APPLIED,
		DISCARDED,
		DOWNLOAD_FAILED,
		ACTIVATION_FAILED;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code State}.
	 */
	public static enum State {
		ACCEPTED,
		DOWNLOADED;
	}
}
