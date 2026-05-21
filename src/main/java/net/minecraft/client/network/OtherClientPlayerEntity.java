package net.minecraft.client.network;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.util.profiler.ScopedProfiler;

/**
 * Клиентское представление другого игрока в мире.
 * <p>В отличие от {@link ClientPlayerEntity}, этот класс представляет удалённых игроков,
 * чьё движение интерполируется на основе данных от сервера. Физика отключена
 * ({@code noClip = true}), а скорость плавно интерполируется к целевому значению.
 */
@Environment(EnvType.CLIENT)
public class OtherClientPlayerEntity extends AbstractClientPlayerEntity {

	private Vec3d clientVelocity = Vec3d.ZERO;
	private int velocityLerpDivisor;

	/**
	 * Создаёт клиентское представление другого игрока.
	 *
	 * @param world       клиентский мир
	 * @param gameProfile профиль игрока
	 */
	public OtherClientPlayerEntity(ClientWorld world, GameProfile gameProfile) {
		super(world, gameProfile);
		noClip = true;
	}

	@Override
	public boolean shouldRender(double distance) {
		double size = getBoundingBox().getAverageSideLength() * 10.0;

		if (Double.isNaN(size)) {
			size = 1.0;
		}

		size *= 64.0 * getRenderDistanceMultiplier();
		return distance < size * size;
	}

	@Override
	public boolean clientDamage(DamageSource source) {
		return true;
	}

	@Override
	public void tick() {
		super.tick();
		updateLimbs(false);
	}

	@Override
	public void tickMovement() {
		if (isInterpolating()) {
			getInterpolator().tick();
		}

		if (headTrackingIncrements > 0) {
			lerpHeadYaw(headTrackingIncrements, serverHeadYaw);
			headTrackingIncrements--;
		}

		if (velocityLerpDivisor > 0) {
			Vec3d currentVelocity = getVelocity();
			addVelocityInternal(new Vec3d(
					(clientVelocity.x - currentVelocity.x) / velocityLerpDivisor,
					(clientVelocity.y - currentVelocity.y) / velocityLerpDivisor,
					(clientVelocity.z - currentVelocity.z) / velocityLerpDivisor
			));
			velocityLerpDivisor--;
		}

		tickHandSwing();
		tickPlayerMovement();

		try (ScopedProfiler ignored = Profilers.get().scoped("push")) {
			tickCramming();
		}
	}

	@Override
	public void setVelocityClient(Vec3d velocity) {
		clientVelocity = velocity;
		velocityLerpDivisor = getType().getTrackTickInterval() + 1;
	}

	@Override
	protected void updatePose() {
	}

	@Override
	public void onSpawnPacket(EntitySpawnS2CPacket packet) {
		super.onSpawnPacket(packet);
		resetPosition();
	}
}
