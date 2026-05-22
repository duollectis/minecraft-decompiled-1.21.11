package net.minecraft.entity.mob;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NavigationConditions;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.conversion.EntityConversionContext;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.world.World;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.jspecify.annotations.Nullable;

/**
 * Базовый класс для всех пиглин-подобных мобов. Управляет логикой иммунитета к огню и базовым поведением пиглинов.
 */
public abstract class AbstractPiglinEntity extends HostileEntity {

	protected static final TrackedData<Boolean> IMMUNE_TO_ZOMBIFICATION = DataTracker.registerData(
			AbstractPiglinEntity.class, TrackedDataHandlerRegistry.BOOLEAN
	);
	public static final int TIME_TO_ZOMBIFY = 300;
	private static final int ZOMBIFICATION_NAUSEA_DURATION = 200;
	protected int timeInOverworld;

	public AbstractPiglinEntity(EntityType<? extends AbstractPiglinEntity> entityType, World world) {
		super(entityType, world);
		setCanPickUpLoot(true);
		setCanPathThroughDoors();
		setPathfindingPenalty(PathNodeType.DANGER_FIRE, 16.0F);
		setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, -1.0F);
	}

	private void setCanPathThroughDoors() {
		if (NavigationConditions.hasMobNavigation(this)) {
			getNavigation().setCanOpenDoors(true);
		}
	}

	protected abstract boolean canHunt();

	public void setImmuneToZombification(boolean immuneToZombification) {
		getDataTracker().set(IMMUNE_TO_ZOMBIFICATION, immuneToZombification);
	}

	protected boolean isImmuneToZombification() {
		return getDataTracker().get(IMMUNE_TO_ZOMBIFICATION);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(IMMUNE_TO_ZOMBIFICATION, false);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putBoolean("IsImmuneToZombification", isImmuneToZombification());
		view.putInt("TimeInOverworld", timeInOverworld);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setCanPickUpLoot(view.getBoolean("CanPickUpLoot", true));
		setImmuneToZombification(view.getBoolean("IsImmuneToZombification", false));
		timeInOverworld = view.getInt("TimeInOverworld", 0);
	}

	@Override
	protected void mobTick(ServerWorld world) {
		super.mobTick(world);
		if (shouldZombify()) {
			timeInOverworld++;
		} else {
			timeInOverworld = 0;
		}

		if (timeInOverworld > TIME_TO_ZOMBIFY) {
			playZombificationSound();
			zombify(world);
		}
	}

	@VisibleForTesting
	public void setTimeInOverworld(int timeInOverworld) {
		this.timeInOverworld = timeInOverworld;
	}

	/**
	 * Возвращает {@code true}, если пиглин находится в измерении Верхнего мира
	 * и не имеет иммунитета к зомбификации — то есть должен начать превращаться.
	 */
	public boolean shouldZombify() {
		return !isImmuneToZombification()
				&& !isAiDisabled()
				&& getEntityWorld()
				.getEnvironmentAttributes()
				.getAttributeValue(EnvironmentAttributes.PIGLINS_ZOMBIFY_GAMEPLAY, getEntityPos());
	}

	/**
	 * Превращает пиглина в зомбифицированного пиглина, применяя эффект тошноты
	 * для визуальной индикации процесса трансформации.
	 */
	protected void zombify(ServerWorld world) {
		convertTo(
				EntityType.ZOMBIFIED_PIGLIN,
				EntityConversionContext.create(this, true, true),
				zombifiedPiglin -> zombifiedPiglin.addStatusEffect(new StatusEffectInstance(
						StatusEffects.NAUSEA,
						ZOMBIFICATION_NAUSEA_DURATION,
						0
				))
		);
	}

	public boolean isAdult() {
		return !isBaby();
	}

	public abstract PiglinActivity getActivity();

	@Override
	public @Nullable LivingEntity getTarget() {
		return getTargetInBrain();
	}

	protected boolean isHoldingTool() {
		return getMainHandStack().contains(DataComponentTypes.TOOL);
	}

	@Override
	public void playAmbientSound() {
		if (PiglinBrain.hasIdleActivity(this)) {
			super.playAmbientSound();
		}
	}

	protected abstract void playZombificationSound();
}
