package net.minecraft.entity.boss.dragon.phase;

import com.mojang.logging.LogUtils;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * Управляет жизненным циклом фаз дракона: хранит кэш созданных экземпляров,
 * переключает текущую фазу с вызовом {@link Phase#endPhase()} и {@link Phase#beginPhase()}.
 */
public class PhaseManager {

	private static final Logger LOGGER = LogUtils.getLogger();

	private final EnderDragonEntity dragon;
	private final @Nullable Phase[] phases = new Phase[PhaseType.count()];
	private @Nullable Phase current;

	public PhaseManager(EnderDragonEntity dragon) {
		this.dragon = dragon;
		setPhase(PhaseType.HOVER);
	}

	public void setPhase(PhaseType<?> type) {
		if (current != null && type == current.getType()) {
			return;
		}

		if (current != null) {
			current.endPhase();
		}

		current = create((PhaseType<Phase>) type);

		if (!dragon.getEntityWorld().isClient()) {
			dragon.getDataTracker().set(EnderDragonEntity.PHASE_TYPE, type.getTypeId());
		}

		LOGGER.debug(
				"Dragon is now in phase {} on the {}",
				type,
				dragon.getEntityWorld().isClient() ? "client" : "server"
		);
		current.beginPhase();
	}

	public Phase getCurrent() {
		return Objects.requireNonNull(current);
	}

	public <T extends Phase> T create(PhaseType<T> type) {
		int typeId = type.getTypeId();
		Phase phase = phases[typeId];

		if (phase == null) {
			phase = type.create(dragon);
			phases[typeId] = phase;
		}

		return (T) phase;
	}
}
