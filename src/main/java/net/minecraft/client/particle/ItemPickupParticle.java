package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
/**
 * {@code ItemPickupParticle}.
 */
public class ItemPickupParticle extends Particle {

	protected static final int TIME_TO_LIVE = 3;
	private final Entity interactingEntity;
	protected int ticksExisted;
	protected final EntityRenderState renderState;
	protected double targetX;
	protected double targetY;
	protected double targetZ;
	protected double lastTargetX;
	protected double lastTargetY;
	protected double lastTargetZ;

	public ItemPickupParticle(ClientWorld world, EntityRenderState renderState, Entity entity, Vec3d velocity) {
		super(world, renderState.x, renderState.y, renderState.z, velocity.x, velocity.y, velocity.z);
		this.interactingEntity = entity;
		this.renderState = renderState;
		this.renderState.outlineColor = 0;
		this.updateTargetPos();
		this.updateLastTargetPos();
	}

	@Override
	public void tick() {
		this.ticksExisted++;
		if (this.ticksExisted == 3) {
			this.markDead();
		}

		this.updateLastTargetPos();
		this.updateTargetPos();
	}

	@Override
	public ParticleTextureSheet textureSheet() {
		return ParticleTextureSheet.ITEM_PICKUP;
	}

	private void updateTargetPos() {
		this.targetX = this.interactingEntity.getX();
		this.targetY = (this.interactingEntity.getY() + this.interactingEntity.getEyeY()) / 2.0;
		this.targetZ = this.interactingEntity.getZ();
	}

	private void updateLastTargetPos() {
		this.lastTargetX = this.targetX;
		this.lastTargetY = this.targetY;
		this.lastTargetZ = this.targetZ;
	}
}
