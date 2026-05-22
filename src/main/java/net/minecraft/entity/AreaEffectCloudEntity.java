package net.minecraft.entity;

import com.google.common.collect.Maps;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Сущность облака эффектов (зелье-брызги, зелье-туман). Хранит набор эффектов
 * {@link net.minecraft.component.type.PotionContentsComponent} и периодически
 * применяет их к живым существам в радиусе действия. Поддерживает изменение
 * радиуса со временем ({@code radiusGrowth}) и при каждом применении ({@code radiusOnUse}).
 */
public class AreaEffectCloudEntity extends Entity implements Ownable {

	private static final int PARTICLE_INTERVAL = 5;
	private static final TrackedData<Float>
			RADIUS =
			DataTracker.registerData(AreaEffectCloudEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Boolean>
			WAITING =
			DataTracker.registerData(AreaEffectCloudEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<ParticleEffect>
			PARTICLE =
			DataTracker.registerData(AreaEffectCloudEntity.class, TrackedDataHandlerRegistry.PARTICLE);
	private static final float MAX_RADIUS = 32.0F;
	private static final int DEFAULT_DURATION_ON_USE = 0;
	private static final int DEFAULT_RADIUS_ON_USE_INT = 0;
	private static final float DEFAULT_RADIUS_ON_USE = 0.0F;
	private static final float DEFAULT_RADIUS_GROWTH = 0.0F;
	private static final float DEFAULT_POTION_DURATION_SCALE = 1.0F;
	private static final float DEFAULT_RADIUS_SHRINK = 0.5F;
	private static final float DEFAULT_RADIUS = 3.0F;
	public static final float DIAMETER = 6.0F;
	public static final float CLOUD_HEIGHT = 0.5F;
	public static final int DEFAULT_DURATION = -1;
	public static final int DEFAULT_LINGERING_DURATION = 600;
	private static final int DEFAULT_WAIT_TIME = 20;
	private static final int DEFAULT_REAPPLICATION_DELAY = 20;
	private static final TintedParticleEffect
			DEFAULT_PARTICLE_EFFECT =
			TintedParticleEffect.create(ParticleTypes.ENTITY_EFFECT, -1);
	private @Nullable ParticleEffect customParticle;
	private PotionContentsComponent potionContentsComponent = PotionContentsComponent.DEFAULT;
	private float potionDurationScale = 1.0F;
	private final Map<Entity, Integer> affectedEntities = Maps.newHashMap();
	private int duration = -1;
	private int waitTime = 20;
	private int reapplicationDelay = 20;
	private int durationOnUse = 0;
	private float radiusOnUse = 0.0F;
	private float radiusGrowth = 0.0F;
	private @Nullable LazyEntityReference<LivingEntity> owner;

	public AreaEffectCloudEntity(EntityType<? extends AreaEffectCloudEntity> entityType, World world) {
		super(entityType, world);
		this.noClip = true;
	}

	public AreaEffectCloudEntity(World world, double x, double y, double z) {
		this(EntityType.AREA_EFFECT_CLOUD, world);
		this.setPosition(x, y, z);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(RADIUS, 3.0F);
		builder.add(WAITING, false);
		builder.add(PARTICLE, DEFAULT_PARTICLE_EFFECT);
	}

	public void setRadius(float radius) {
		if (!this.getEntityWorld().isClient()) {
			this.getDataTracker().set(RADIUS, MathHelper.clamp(radius, 0.0F, MAX_RADIUS));
		}
	}

	@Override
	public void calculateDimensions() {
		double d = this.getX();
		double e = this.getY();
		double f = this.getZ();
		super.calculateDimensions();
		this.setPosition(d, e, f);
	}

	public float getRadius() {
		return this.getDataTracker().get(RADIUS);
	}

	public void setPotionContents(PotionContentsComponent potionContentsComponent) {
		this.potionContentsComponent = potionContentsComponent;
		this.updateParticle();
	}

	public void setParticleType(@Nullable ParticleEffect customParticle) {
		this.customParticle = customParticle;
		this.updateParticle();
	}

	public void setPotionDurationScale(float potionDurationScale) {
		this.potionDurationScale = potionDurationScale;
	}

	private void updateParticle() {
		if (this.customParticle != null) {
			this.dataTracker.set(PARTICLE, this.customParticle);
		}
		else {
			int color = ColorHelper.fullAlpha(this.potionContentsComponent.getColor());
			this.dataTracker.set(PARTICLE, TintedParticleEffect.create(DEFAULT_PARTICLE_EFFECT.getType(), color));
		}
	}

	public void addEffect(StatusEffectInstance effect) {
		this.setPotionContents(this.potionContentsComponent.with(effect));
	}

	public ParticleEffect getParticleType() {
		return this.getDataTracker().get(PARTICLE);
	}

	protected void setWaiting(boolean waiting) {
		this.getDataTracker().set(WAITING, waiting);
	}

	public boolean isWaiting() {
		return this.getDataTracker().get(WAITING);
	}

	public int getDuration() {
		return this.duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			this.serverTick(serverWorld);
		}
		else {
			this.clientTick();
		}
	}

	private void clientTick() {
		boolean waiting = this.isWaiting();
		float radius = this.getRadius();
		if (waiting && this.random.nextBoolean()) {
			return;
		}

		ParticleEffect particleEffect = this.getParticleType();
		int particleCount;
		float spread;
		if (waiting) {
			particleCount = 2;
			spread = 0.2F;
		}
		else {
			particleCount = MathHelper.ceil((float) Math.PI * radius * radius);
			spread = radius;
		}

		for (int index = 0; index < particleCount; index++) {
			float angle = this.random.nextFloat() * (float) (Math.PI * 2);
			float distance = MathHelper.sqrt(this.random.nextFloat()) * spread;
			double particleX = this.getX() + MathHelper.cos(angle) * distance;
			double particleY = this.getY();
			double particleZ = this.getZ() + MathHelper.sin(angle) * distance;

			if (particleEffect.getType() == ParticleTypes.ENTITY_EFFECT) {
				if (waiting && this.random.nextBoolean()) {
					this.getEntityWorld()
					    .addImportantParticleClient(DEFAULT_PARTICLE_EFFECT, particleX, particleY, particleZ, 0.0, 0.0, 0.0);
				}
				else {
					this.getEntityWorld()
					    .addImportantParticleClient(particleEffect, particleX, particleY, particleZ, 0.0, 0.0, 0.0);
				}
			}
			else if (waiting) {
				this.getEntityWorld()
				    .addImportantParticleClient(particleEffect, particleX, particleY, particleZ, 0.0, 0.0, 0.0);
			}
			else {
				this.getEntityWorld()
				    .addImportantParticleClient(
						    particleEffect,
						    particleX,
						    particleY,
						    particleZ,
						    (0.5 - this.random.nextDouble()) * 0.15,
						    0.01F,
						    (0.5 - this.random.nextDouble()) * 0.15
				    );
			}
		}
	}

	private void serverTick(ServerWorld world) {
		if (this.duration != DEFAULT_DURATION && this.age - this.waitTime >= this.duration) {
			this.discard();
			return;
		}

		boolean wasWaiting = this.isWaiting();
		boolean isWaiting = this.age < this.waitTime;
		if (wasWaiting != isWaiting) {
			this.setWaiting(isWaiting);
		}

		if (isWaiting) {
			return;
		}

		float currentRadius = this.getRadius();
		if (this.radiusGrowth != 0.0F) {
			currentRadius += this.radiusGrowth;
			if (currentRadius < 0.5F) {
				this.discard();
				return;
			}

			this.setRadius(currentRadius);
		}

		if (this.age % PARTICLE_INTERVAL != 0) {
			return;
		}

		this.affectedEntities.entrySet().removeIf(entry -> this.age >= entry.getValue());
		if (!this.potionContentsComponent.hasEffects()) {
			this.affectedEntities.clear();
			return;
		}

		List<StatusEffectInstance> effects = new ArrayList<>();
		this.potionContentsComponent.forEachEffect(effects::add, this.potionDurationScale);
		List<LivingEntity> nearbyEntities = this.getEntityWorld()
		                                        .getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox());

		for (LivingEntity livingEntity : nearbyEntities) {
			if (this.affectedEntities.containsKey(livingEntity)) {
				continue;
			}

			if (!livingEntity.isAffectedBySplashPotions()) {
				continue;
			}

			if (effects.stream().noneMatch(livingEntity::canHaveStatusEffect)) {
				continue;
			}

			double deltaX = livingEntity.getX() - this.getX();
			double deltaZ = livingEntity.getZ() - this.getZ();
			double distanceSq = deltaX * deltaX + deltaZ * deltaZ;
			if (distanceSq > currentRadius * currentRadius) {
				continue;
			}

			this.affectedEntities.put(livingEntity, this.age + this.reapplicationDelay);

			for (StatusEffectInstance effect : effects) {
				if (effect.getEffectType().value().isInstant()) {
					effect.getEffectType()
					      .value()
					      .applyInstantEffect(world, this, this.getOwner(), livingEntity, effect.getAmplifier(), 0.5);
				}
				else {
					livingEntity.addStatusEffect(new StatusEffectInstance(effect), this);
				}
			}

			if (this.radiusOnUse != 0.0F) {
				currentRadius += this.radiusOnUse;
				if (currentRadius < 0.5F) {
					this.discard();
					return;
				}

				this.setRadius(currentRadius);
			}

			if (this.durationOnUse != 0 && this.duration != DEFAULT_DURATION) {
				this.duration += this.durationOnUse;
				if (this.duration <= 0) {
					this.discard();
					return;
				}
			}
		}
	}

	public float getRadiusOnUse() {
		return this.radiusOnUse;
	}

	public void setRadiusOnUse(float radiusOnUse) {
		this.radiusOnUse = radiusOnUse;
	}

	public float getRadiusGrowth() {
		return this.radiusGrowth;
	}

	public void setRadiusGrowth(float radiusGrowth) {
		this.radiusGrowth = radiusGrowth;
	}

	public int getDurationOnUse() {
		return this.durationOnUse;
	}

	public void setDurationOnUse(int durationOnUse) {
		this.durationOnUse = durationOnUse;
	}

	public int getWaitTime() {
		return this.waitTime;
	}

	public void setWaitTime(int waitTime) {
		this.waitTime = waitTime;
	}

	public void setOwner(@Nullable LivingEntity owner) {
		this.owner = LazyEntityReference.of(owner);
	}

	public @Nullable LivingEntity getOwner() {
		return LazyEntityReference.getLivingEntity(this.owner, this.getEntityWorld());
	}

	@Override
	protected void readCustomData(ReadView view) {
		this.age = view.getInt("Age", 0);
		this.duration = view.getInt("Duration", -1);
		this.waitTime = view.getInt("WaitTime", DEFAULT_WAIT_TIME);
		this.reapplicationDelay = view.getInt("ReapplicationDelay", DEFAULT_REAPPLICATION_DELAY);
		this.durationOnUse = view.getInt("DurationOnUse", 0);
		this.radiusOnUse = view.getFloat("RadiusOnUse", 0.0F);
		this.radiusGrowth = view.getFloat("RadiusPerTick", 0.0F);
		this.setRadius(view.getFloat("Radius", 3.0F));
		this.owner = LazyEntityReference.fromData(view, "Owner");
		this.setParticleType(view.<ParticleEffect>read("custom_particle", ParticleTypes.TYPE_CODEC).orElse(null));
		this.setPotionContents(view
				.<PotionContentsComponent>read("potion_contents", PotionContentsComponent.CODEC)
				.orElse(PotionContentsComponent.DEFAULT));
		this.potionDurationScale = view.getFloat("potion_duration_scale", 1.0F);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		view.putInt("Age", this.age);
		view.putInt("Duration", this.duration);
		view.putInt("WaitTime", this.waitTime);
		view.putInt("ReapplicationDelay", this.reapplicationDelay);
		view.putInt("DurationOnUse", this.durationOnUse);
		view.putFloat("RadiusOnUse", this.radiusOnUse);
		view.putFloat("RadiusPerTick", this.radiusGrowth);
		view.putFloat("Radius", this.getRadius());
		view.putNullable("custom_particle", ParticleTypes.TYPE_CODEC, this.customParticle);
		LazyEntityReference.writeData(this.owner, view, "Owner");
		if (!this.potionContentsComponent.equals(PotionContentsComponent.DEFAULT)) {
			view.put("potion_contents", PotionContentsComponent.CODEC, this.potionContentsComponent);
		}

		if (this.potionDurationScale != 1.0F) {
			view.putFloat("potion_duration_scale", this.potionDurationScale);
		}
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		if (RADIUS.equals(data)) {
			this.calculateDimensions();
		}

		super.onTrackedDataSet(data);
	}

	@Override
	public PistonBehavior getPistonBehavior() {
		return PistonBehavior.IGNORE;
	}

	@Override
	public EntityDimensions getDimensions(EntityPose pose) {
		return EntityDimensions.changing(this.getRadius() * 2.0F, 0.5F);
	}

	@Override
	public final boolean damage(ServerWorld world, DamageSource source, float amount) {
		return false;
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		if (type == DataComponentTypes.POTION_CONTENTS) {
			return castComponentValue((ComponentType<T>) type, this.potionContentsComponent);
		}
		else {
			return type == DataComponentTypes.POTION_DURATION_SCALE ? castComponentValue(
					(ComponentType<T>) type,
					this.potionDurationScale
			) : super.get(type);
		}
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		this.copyComponentFrom(from, DataComponentTypes.POTION_CONTENTS);
		this.copyComponentFrom(from, DataComponentTypes.POTION_DURATION_SCALE);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.POTION_CONTENTS) {
			this.setPotionContents(castComponentValue(DataComponentTypes.POTION_CONTENTS, value));
			return true;
		}
		else if (type == DataComponentTypes.POTION_DURATION_SCALE) {
			this.setPotionDurationScale(castComponentValue(DataComponentTypes.POTION_DURATION_SCALE, value));
			return true;
		}
		else {
			return super.setApplicableComponent(type, value);
		}
	}
}
