package net.minecraft.item.consume;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * Эффект потребления, телепортирующий сущность в случайную точку в радиусе {@code diameter / 2}.
 * Совершает до 16 попыток найти валидную позицию телепортации.
 * При успехе сбрасывает текущий взрыв у игрока (если применимо).
 */
public record TeleportRandomlyConsumeEffect(float diameter) implements ConsumeEffect {

	private static final float DEFAULT_DIAMETER = 16.0F;
	public static final MapCodec<TeleportRandomlyConsumeEffect> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(Codecs.POSITIVE_FLOAT
							.optionalFieldOf("diameter", DEFAULT_DIAMETER)
							.forGetter(TeleportRandomlyConsumeEffect::diameter))
					.apply(instance, TeleportRandomlyConsumeEffect::new)
	);
	public static final PacketCodec<RegistryByteBuf, TeleportRandomlyConsumeEffect> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.FLOAT, TeleportRandomlyConsumeEffect::diameter, TeleportRandomlyConsumeEffect::new
	);

	public TeleportRandomlyConsumeEffect() {
		this(DEFAULT_DIAMETER);
	}

	@Override
	public ConsumeEffect.Type<TeleportRandomlyConsumeEffect> getType() {
		return ConsumeEffect.Type.TELEPORT_RANDOMLY;
	}

	@Override
	public boolean onConsume(World world, ItemStack stack, LivingEntity user) {
		boolean teleported = false;

		for (int attempt = 0; attempt < 16; attempt++) {
			double targetX = user.getX() + (user.getRandom().nextDouble() - 0.5) * diameter;
			double targetY = MathHelper.clamp(
					user.getY() + (user.getRandom().nextDouble() - 0.5) * diameter,
					world.getBottomY(),
					world.getBottomY() + ((ServerWorld) world).getLogicalHeight() - 1
			);
			double targetZ = user.getZ() + (user.getRandom().nextDouble() - 0.5) * diameter;

			if (user.hasVehicle()) {
				user.stopRiding();
			}

			Vec3d prevPos = user.getEntityPos();

			if (user.teleport(targetX, targetY, targetZ, true)) {
				world.emitGameEvent(GameEvent.TELEPORT, prevPos, GameEvent.Emitter.of(user));
				playSoundForEntity(world, user);
				user.onLanding();
				teleported = true;
				break;
			}
		}

		if (teleported && user instanceof PlayerEntity playerEntity) {
			playerEntity.clearCurrentExplosion();
		}

		return teleported;
	}

	private static void playSoundForEntity(World world, LivingEntity user) {
		SoundEvent sound = user instanceof FoxEntity
				? SoundEvents.ENTITY_FOX_TELEPORT
				: SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT;
		SoundCategory category = user instanceof FoxEntity
				? SoundCategory.NEUTRAL
				: SoundCategory.PLAYERS;
		world.playSound(null, user.getX(), user.getY(), user.getZ(), sound, category);
	}
}
