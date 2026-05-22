package net.minecraft.entity.passive;

import com.mojang.logging.LogUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.world.World;
import org.slf4j.Logger;

/**
 * Базовый класс для прирученных существ, способных сидеть на плече игрока (попугаи).
 * Отслеживает количество тиков с момента создания для определения готовности к посадке.
 */
public abstract class TameableShoulderEntity extends TameableEntity {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int READY_TO_SIT_COOLDOWN = 100;

	private int ticks;

	protected TameableShoulderEntity(EntityType<? extends TameableShoulderEntity> entityType, World world) {
		super(entityType, world);
	}

	/**
	 * Сериализует данные существа в NBT и пытается посадить его на плечо игрока.
	 * После успешной посадки существо удаляется из мира.
	 *
	 * @param player игрок, на плечо которого садится существо
	 * @return {@code true} если посадка прошла успешно
	 */
	public boolean mountOnto(ServerPlayerEntity player) {
		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(getErrorReporterContext(), LOGGER)) {
			NbtWriteView nbtWriteView = NbtWriteView.create(logging, getRegistryManager());
			writeData(nbtWriteView);
			nbtWriteView.putString("id", getSavedEntityId());

			if (player.mountOntoShoulder(nbtWriteView.getNbt())) {
				discard();
				return true;
			}
		}

		return false;
	}

	@Override
	public void tick() {
		ticks++;
		super.tick();
	}

	public boolean isReadyToSitOnPlayer() {
		return ticks > READY_TO_SIT_COOLDOWN;
	}
}
