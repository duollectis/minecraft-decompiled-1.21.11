package net.minecraft.block.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Менеджер уровней предупреждения визжащего скалька для игрока. Отслеживает накопленный уровень угрозы,
 * кулдаун между предупреждениями и автоматически снижает уровень после длительного затишья.
 */
public class SculkShriekerWarningManager {

	public static final Codec<SculkShriekerWarningManager> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			Codecs.NON_NEGATIVE_INT
				.fieldOf("ticks_since_last_warning")
				.orElse(0)
				.forGetter(manager -> manager.ticksSinceLastWarning),
			Codecs.NON_NEGATIVE_INT
				.fieldOf("warning_level")
				.orElse(0)
				.forGetter(manager -> manager.warningLevel),
			Codecs.NON_NEGATIVE_INT
				.fieldOf("cooldown_ticks")
				.orElse(0)
				.forGetter(manager -> manager.cooldownTicks)
		).apply(instance, SculkShriekerWarningManager::new)
	);

	public static final int MAX_WARNING_LEVEL = 4;
	private static final double WARN_RANGE = 16.0;
	private static final int WARN_WARDEN_RANGE = 48;
	private static final int WARN_DECREASE_COOLDOWN = 12000;
	private static final int WARN_INCREASE_COOLDOWN = 200;

	private int ticksSinceLastWarning;
	private int warningLevel;
	private int cooldownTicks;

	public SculkShriekerWarningManager(int ticksSinceLastWarning, int warningLevel, int cooldownTicks) {
		this.ticksSinceLastWarning = ticksSinceLastWarning;
		this.warningLevel = warningLevel;
		this.cooldownTicks = cooldownTicks;
	}

	public SculkShriekerWarningManager() {
		this(0, 0, 0);
	}

	public void tick() {
		if (ticksSinceLastWarning >= WARN_DECREASE_COOLDOWN) {
			decreaseWarningLevel();
			ticksSinceLastWarning = 0;
		} else {
			ticksSinceLastWarning++;
		}

		if (cooldownTicks > 0) {
			cooldownTicks--;
		}
	}

	public void reset() {
		ticksSinceLastWarning = 0;
		warningLevel = 0;
		cooldownTicks = 0;
	}

	/**
	 * Предупреждает всех игроков в радиусе {@code WARN_RANGE} блоков от позиции визжащего скалька.
	 * Возвращает пустой результат, если рядом уже есть Хранитель или кто-то из игроков на кулдауне.
	 */
	public static OptionalInt warnNearbyPlayers(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
		if (isWardenNearby(world, pos)) {
			return OptionalInt.empty();
		}

		List<ServerPlayerEntity> nearbyPlayers = getPlayersInRange(world, pos);

		if (!nearbyPlayers.contains(player)) {
			nearbyPlayers.add(player);
		}

		boolean anyOnCooldown = nearbyPlayers
			.stream()
			.anyMatch(p -> p.getSculkShriekerWarningManager()
				.map(SculkShriekerWarningManager::isInCooldown)
				.orElse(false));

		if (anyOnCooldown) {
			return OptionalInt.empty();
		}

		Optional<SculkShriekerWarningManager> highestManager = nearbyPlayers
			.stream()
			.flatMap(p -> p.getSculkShriekerWarningManager().stream())
			.max(Comparator.comparingInt(SculkShriekerWarningManager::getWarningLevel));

		if (highestManager.isEmpty()) {
			return OptionalInt.empty();
		}

		SculkShriekerWarningManager dominant = highestManager.get();
		dominant.increaseWarningLevel();
		nearbyPlayers.forEach(p -> p.getSculkShriekerWarningManager()
			.ifPresent(manager -> manager.copy(dominant)));

		return OptionalInt.of(dominant.warningLevel);
	}

	private boolean isInCooldown() {
		return cooldownTicks > 0;
	}

	private static boolean isWardenNearby(ServerWorld world, BlockPos pos) {
		Box box = Box.of(Vec3d.ofCenter(pos), WARN_WARDEN_RANGE, WARN_WARDEN_RANGE, WARN_WARDEN_RANGE);
		return !world.getNonSpectatingEntities(WardenEntity.class, box).isEmpty();
	}

	private static List<ServerPlayerEntity> getPlayersInRange(ServerWorld world, BlockPos pos) {
		Vec3d center = Vec3d.ofCenter(pos);
		return world.getPlayers(p -> !p.isSpectator() && p.getEntityPos().isInRange(center, WARN_RANGE) && p.isAlive());
	}

	private void increaseWarningLevel() {
		if (isInCooldown()) {
			return;
		}

		ticksSinceLastWarning = 0;
		cooldownTicks = WARN_INCREASE_COOLDOWN;
		setWarningLevel(getWarningLevel() + 1);
	}

	private void decreaseWarningLevel() {
		setWarningLevel(getWarningLevel() - 1);
	}

	public void setWarningLevel(int warningLevel) {
		this.warningLevel = MathHelper.clamp(warningLevel, 0, MAX_WARNING_LEVEL);
	}

	public int getWarningLevel() {
		return warningLevel;
	}

	private void copy(SculkShriekerWarningManager other) {
		warningLevel = other.warningLevel;
		cooldownTicks = other.cooldownTicks;
		ticksSinceLastWarning = other.ticksSinceLastWarning;
	}
}
