package net.minecraft.entity.mob;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Пиглин-брута — агрессивный вариант пиглина с золотым топором.
 */
public class PiglinBruteEntity extends AbstractPiglinEntity {

	private static final int MAX_HEALTH = 50;
	private static final float MOVEMENT_SPEED = 0.35F;
	private static final int ATTACK_DAMAGE = 7;
	private static final double FOLLOW_RANGE = 12.0;
	protected static final ImmutableList<SensorType<? extends Sensor<? super PiglinBruteEntity>>>
			SENSOR_TYPES =
			ImmutableList.of(
					SensorType.NEAREST_LIVING_ENTITIES,
					SensorType.NEAREST_PLAYERS,
					SensorType.NEAREST_ITEMS,
					SensorType.HURT_BY,
					SensorType.PIGLIN_BRUTE_SPECIFIC_SENSOR
			);
	protected static final ImmutableList<MemoryModuleType<?>> MEMORY_MODULE_TYPES = ImmutableList.of(
			MemoryModuleType.LOOK_TARGET,
			MemoryModuleType.DOORS_TO_CLOSE,
			MemoryModuleType.MOBS,
			MemoryModuleType.VISIBLE_MOBS,
			MemoryModuleType.NEAREST_VISIBLE_PLAYER,
			MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER,
			MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS,
			MemoryModuleType.NEARBY_ADULT_PIGLINS,
			MemoryModuleType.HURT_BY,
			MemoryModuleType.HURT_BY_ENTITY,
			MemoryModuleType.WALK_TARGET,
			MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
			new MemoryModuleType[]{
					MemoryModuleType.ATTACK_TARGET,
					MemoryModuleType.ATTACK_COOLING_DOWN,
					MemoryModuleType.INTERACTION_TARGET,
					MemoryModuleType.PATH,
					MemoryModuleType.ANGRY_AT,
					MemoryModuleType.NEAREST_VISIBLE_NEMESIS,
					MemoryModuleType.HOME
			}
	);

	public PiglinBruteEntity(EntityType<? extends PiglinBruteEntity> entityType, World world) {
		super(entityType, world);
		experiencePoints = 20;
	}

	public static DefaultAttributeContainer.Builder createPiglinBruteAttributes() {
		return HostileEntity.createHostileAttributes()
		                    .add(EntityAttributes.MAX_HEALTH, MAX_HEALTH)
		                    .add(EntityAttributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
		                    .add(EntityAttributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
		                    .add(EntityAttributes.FOLLOW_RANGE, FOLLOW_RANGE);
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		PiglinBruteBrain.setCurrentPosAsHome(this);
		initEquipment(world.getRandom(), difficulty);
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	@Override
	protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
		equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
	}

	@Override
	protected Brain.Profile<PiglinBruteEntity> createBrainProfile() {
		return Brain.createProfile(MEMORY_MODULE_TYPES, SENSOR_TYPES);
	}

	@Override
	protected Brain<?> deserializeBrain(Dynamic<?> dynamic) {
		return PiglinBruteBrain.create(this, createBrainProfile().deserialize(dynamic));
	}

	@Override
	public Brain<PiglinBruteEntity> getBrain() {
		return (Brain<PiglinBruteEntity>) super.getBrain();
	}

	@Override
	public boolean canHunt() {
		return false;
	}

	@Override
	public boolean canGather(ServerWorld world, ItemStack stack) {
		if (!stack.isOf(Items.GOLDEN_AXE)) {
			return false;
		}

		return super.canGather(world, stack);
	}

	@Override
	protected void mobTick(ServerWorld world) {
		Profiler profiler = Profilers.get();
		profiler.push("piglinBruteBrain");
		getBrain().tick(world, this);
		profiler.pop();
		PiglinBruteBrain.tick(this);
		PiglinBruteBrain.playSoundRandomly(this);
		super.mobTick(world);
	}

	@Override
	public PiglinActivity getActivity() {
		return isAttacking() && isHoldingTool()
				? PiglinActivity.ATTACKING_WITH_MELEE_WEAPON
				: PiglinActivity.DEFAULT;
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		boolean damaged = super.damage(world, source, amount);

		if (damaged && source.getAttacker() instanceof LivingEntity attacker) {
			PiglinBruteBrain.tryRevenge(world, this, attacker);
		}

		return damaged;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_PIGLIN_BRUTE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_PIGLIN_BRUTE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_PIGLIN_BRUTE_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.ENTITY_PIGLIN_BRUTE_STEP, 0.15F, 1.0F);
	}

	protected void playAngrySound() {
		playSound(SoundEvents.ENTITY_PIGLIN_BRUTE_ANGRY);
	}

	@Override
	protected void playZombificationSound() {
		playSound(SoundEvents.ENTITY_PIGLIN_BRUTE_CONVERTED_TO_ZOMBIFIED);
	}
}
