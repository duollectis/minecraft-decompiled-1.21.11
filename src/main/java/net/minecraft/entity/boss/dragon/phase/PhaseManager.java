package net.minecraft.entity.boss.dragon.phase;

import com.mojang.logging.LogUtils;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * {@code PhaseManager}.
 */
public class PhaseManager {

	private static final Logger LOGGER = LogUtils.getLogger();
	private final EnderDragonEntity dragon;
	private final @Nullable Phase[] phases = new Phase[PhaseType.count()];
	private @Nullable Phase current;

	public PhaseManager(EnderDragonEntity dragon) {
		this.dragon = dragon;
		this.setPhase(PhaseType.HOVER);
	}

	public void setPhase(PhaseType<?> type) {
		if (this.current == null || type != this.current.getType()) {
			if (this.current != null) {
				this.current.endPhase();
			}

			this.current = this.create((PhaseType<Phase>) type);
			if (!this.dragon.getEntityWorld().isClient()) {
				this.dragon.getDataTracker().set(EnderDragonEntity.PHASE_TYPE, type.getTypeId());
			}

			LOGGER.debug(
					"Dragon is now in phase {} on the {}",
					type,
					this.dragon.getEntityWorld().isClient() ? "client" : "server"
			);
			this.current.beginPhase();
		}
	}

	public Phase getCurrent() {
		return Objects.requireNonNull(this.current);
	}

	public <T extends Phase> T create(PhaseType<T> type) {
		int i = type.getTypeId();
		Phase phase = this.phases[i];
		if (phase == null) {
			phase = type.create(this.dragon);
			this.phases[i] = phase;
		}

		return (T) phase;
	}
}
