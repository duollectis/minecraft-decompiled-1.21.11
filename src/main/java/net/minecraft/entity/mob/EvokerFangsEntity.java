package net.minecraft.entity.mob;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Клыки вуки — снаряд призывателя, наносящий урон.
 */
public class EvokerFangsEntity extends Entity implements Ownable {

	public static final int ANIMATION_DURATION = 20;
	public static final int ANIMATION_END_OFFSET = 2;
	public static final int PARTICLE_SPAWN_TICKS_LEFT = 14;
	private int warmup;
	private boolean startedAttack;
	private int ticksLeft = 22;
	private boolean playingAnimation;
	private @Nullable LazyEntityReference<LivingEntity> owner;

	public EvokerFangsEntity(EntityType<? extends EvokerFangsEntity> entityType, World world) {
		super(entityType, world);
	}

	public EvokerFangsEntity(World world, double x, double y, double z, float yaw, int warmup, LivingEntity owner) {
		this(EntityType.EVOKER_FANGS, world);
		this.warmup = warmup;
		this.setOwner(owner);
		this.setYaw(yaw * (180.0F / (float) Math.PI));
		this.setPosition(x, y, z);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
	}

	public void setOwner(@Nullable LivingEntity owner) {
		this.owner = LazyEntityReference.of(owner);
	}

	public @Nullable LivingEntity getOwner() {
		return LazyEntityReference.getLivingEntity(this.owner, this.getEntityWorld());
	}

	@Override
	protected void readCustomData(ReadView view) {
		this.warmup = view.getInt("Warmup", 0);
		this.owner = LazyEntityReference.fromData(view, "Owner");
	}

	@Override
	protected void writeCustomData(WriteView view) {
		view.putInt("Warmup", this.warmup);
		LazyEntityReference.writeData(this.owner, view, "Owner");
	}

	@Override
	public void tick() {
		super.tick();

		if (getEntityWorld().isClient()) {
			if (playingAnimation) {
				ticksLeft--;

				if (ticksLeft == PARTICLE_SPAWN_TICKS_LEFT) {
					for (int count = 0; count < 12; count++) {
						double particleX = getX() + (random.nextDouble() * 2.0 - 1.0) * getWidth() * 0.5;
						double particleY = getY() + 0.05 + random.nextDouble();
						double particleZ = getZ() + (random.nextDouble() * 2.0 - 1.0) * getWidth() * 0.5;
						double velX = (random.nextDouble() * 2.0 - 1.0) * 0.3;
						double velY = 0.3 + random.nextDouble() * 0.3;
						double velZ = (random.nextDouble() * 2.0 - 1.0) * 0.3;
						getEntityWorld().addParticleClient(ParticleTypes.CRIT, particleX, particleY + 1.0, particleZ, velX, velY, velZ);
					}
				}
			}

			return;
		}

		if (--warmup < 0) {
			if (warmup == -8) {
				for (LivingEntity livingEntity : getEntityWorld()
						.getNonSpectatingEntities(LivingEntity.class, getBoundingBox().expand(0.2, 0.0, 0.2))) {
					damage(livingEntity);
				}
			}

			if (!startedAttack) {
				getEntityWorld().sendEntityStatus(this, (byte) 4);
				startedAttack = true;
			}

			if (--ticksLeft < 0) {
				discard();
			}
		}
	}

	private void damage(LivingEntity target) {
		LivingEntity livingEntity = this.getOwner();
		if (target.isAlive() && !target.isInvulnerable() && target != livingEntity) {
			if (livingEntity == null) {
				target.serverDamage(this.getDamageSources().magic(), 6.0F);
			}
			else {
				if (livingEntity.isTeammate(target)) {
					return;
				}

				DamageSource damageSource = this.getDamageSources().indirectMagic(this, livingEntity);
				if (this.getEntityWorld() instanceof ServerWorld serverWorld && target.damage(
						serverWorld,
						damageSource,
						6.0F
				)) {
					EnchantmentHelper.onTargetDamaged(serverWorld, target, damageSource);
				}
			}
		}
	}

	@Override
	public void handleStatus(byte status) {
		super.handleStatus(status);
		if (status == 4) {
			this.playingAnimation = true;
			if (!this.isSilent()) {
				this.getEntityWorld()
				    .playSoundClient(
						    this.getX(),
						    this.getY(),
						    this.getZ(),
						    SoundEvents.ENTITY_EVOKER_FANGS_ATTACK,
						    this.getSoundCategory(),
						    1.0F,
						    this.random.nextFloat() * 0.2F + 0.85F,
						    false
				    );
			}
		}
	}

	public float getAnimationProgress(float tickProgress) {
		if (!playingAnimation) {
			return 0.0F;
		}

		int remaining = ticksLeft - 2;
		return remaining <= 0 ? 1.0F : 1.0F - (remaining - tickProgress) / 20.0F;
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		return false;
	}
}
