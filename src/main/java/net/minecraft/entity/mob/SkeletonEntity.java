package net.minecraft.entity.mob;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.conversion.EntityConversionContext;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.world.World;

/**
 * Скелет — дальнобойный моб, стреляющий стрелами.
 * При длительном нахождении в порошковом снегу конвертируется в бродягу (Stray).
 */
public class SkeletonEntity extends AbstractSkeletonEntity {

	private static final int TOTAL_CONVERSION_TIME = 300;
	private static final TrackedData<Boolean>
			CONVERTING =
			DataTracker.registerData(SkeletonEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	public static final String STRAY_CONVERSION_TIME_KEY = "StrayConversionTime";
	private static final int DEFAULT_STRAY_CONVERSION_TIME = -1;
	private int inPowderSnowTime;
	private int conversionTime;

	public SkeletonEntity(EntityType<? extends SkeletonEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(CONVERTING, false);
	}

	public boolean isConverting() {
		return getDataTracker().get(CONVERTING);
	}

	public void setConverting(boolean converting) {
		dataTracker.set(CONVERTING, converting);
	}

	@Override
	public boolean isShaking() {
		return isConverting();
	}

	@Override
	public void tick() {
		if (!getEntityWorld().isClient() && isAlive() && !isAiDisabled()) {
			if (inPowderSnow) {
				if (isConverting()) {
					conversionTime--;

					if (conversionTime < 0) {
						convertToStray();
					}
				} else {
					inPowderSnowTime++;

					if (inPowderSnowTime >= 140) {
						setConversionTime(TOTAL_CONVERSION_TIME);
					}
				}
			} else {
				inPowderSnowTime = -1;
				setConverting(false);
			}
		}

		super.tick();
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putInt("StrayConversionTime", isConverting() ? conversionTime : DEFAULT_STRAY_CONVERSION_TIME);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		int savedTime = view.getInt("StrayConversionTime", DEFAULT_STRAY_CONVERSION_TIME);

		if (savedTime != DEFAULT_STRAY_CONVERSION_TIME) {
			setConversionTime(savedTime);
		} else {
			setConverting(false);
		}
	}

	@VisibleForTesting
	public void setConversionTime(int time) {
		conversionTime = time;
		setConverting(true);
	}

	protected void convertToStray() {
		convertTo(
			EntityType.STRAY,
			EntityConversionContext.create(this, true, true),
			stray -> {
				if (!isSilent()) {
					getEntityWorld().syncWorldEvent(null, 1048, getBlockPos(), 0);
				}
			}
		);
	}

	@Override
	public boolean canFreeze() {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_SKELETON_DEATH;
	}

	@Override
	SoundEvent getStepSound() {
		return SoundEvents.ENTITY_SKELETON_STEP;
	}
}
