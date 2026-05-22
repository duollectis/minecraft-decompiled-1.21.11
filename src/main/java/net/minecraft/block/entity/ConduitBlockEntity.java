package net.minecraft.block.entity;

import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LazyEntityReference;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Блок-сущность кондуита. Управляет активацией, атакой монстров и эффектами
 * для игроков в воде. Активируется при наличии достаточного числа призмариновых блоков
 * в радиусе {@code ACTIVATION_FRAME_RADIUS}.
 */
public class ConduitBlockEntity extends BlockEntity {

	private static final int ACTIVATION_CHECK_INTERVAL = 40;
	private static final int AMBIENT_SOUND_INTERVAL = 80;
	private static final float EYE_ROTATION_SPEED = -0.0375F;
	private static final int MIN_BLOCKS_TO_ACTIVATE_PARTIAL = 16;
	private static final int MIN_BLOCKS_TO_ACTIVATE = 42;
	private static final int EFFECT_RANGE_PER_BLOCK = 8;
	private static final int CONDUIT_POWER_DURATION = 260;
	private static final Block[] ACTIVATING_BLOCKS = {
			Blocks.PRISMARINE,
			Blocks.PRISMARINE_BRICKS,
			Blocks.SEA_LANTERN,
			Blocks.DARK_PRISMARINE
	};

	public int ticks;
	private float ticksActive;
	private boolean active;
	private boolean eyeOpen;
	private final List<BlockPos> activatingBlocks = Lists.newArrayList();
	private @Nullable LazyEntityReference<LivingEntity> targetEntity;
	private long nextAmbientSoundTime;

	public ConduitBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.CONDUIT, pos, state);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		targetEntity = LazyEntityReference.fromData(view, "Target");
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		LazyEntityReference.writeData(targetEntity, view, "Target");
	}

	@Override
	public BlockEntityUpdateS2CPacket toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return createComponentlessNbt(registries);
	}

	public static void clientTick(World world, BlockPos pos, BlockState state, ConduitBlockEntity blockEntity) {
		blockEntity.ticks++;
		long worldTime = world.getTime();
		List<BlockPos> frames = blockEntity.activatingBlocks;

		if (worldTime % ACTIVATION_CHECK_INTERVAL == 0L) {
			blockEntity.active = updateActivatingBlocks(world, pos, frames);
			openEye(blockEntity, frames);
		}

		LivingEntity target = LazyEntityReference.getLivingEntity(blockEntity.targetEntity, world);
		spawnNautilusParticles(world, pos, frames, target, blockEntity.ticks);

		if (blockEntity.isActive()) {
			blockEntity.ticksActive++;
		}
	}

	public static void serverTick(World world, BlockPos pos, BlockState state, ConduitBlockEntity blockEntity) {
		blockEntity.ticks++;
		long worldTime = world.getTime();
		List<BlockPos> frames = blockEntity.activatingBlocks;

		if (worldTime % ACTIVATION_CHECK_INTERVAL == 0L) {
			boolean nowActive = updateActivatingBlocks(world, pos, frames);
			if (nowActive != blockEntity.active) {
				SoundEvent sound = nowActive
						? SoundEvents.BLOCK_CONDUIT_ACTIVATE
						: SoundEvents.BLOCK_CONDUIT_DEACTIVATE;
				world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
			}

			blockEntity.active = nowActive;
			openEye(blockEntity, frames);

			if (nowActive) {
				givePlayersEffects(world, pos, frames);
				tryAttack((ServerWorld) world, pos, state, blockEntity, frames.size() >= MIN_BLOCKS_TO_ACTIVATE);
			}
		}

		if (blockEntity.isActive()) {
			if (worldTime % AMBIENT_SOUND_INTERVAL == 0L) {
				world.playSound(null, pos, SoundEvents.BLOCK_CONDUIT_AMBIENT, SoundCategory.BLOCKS, 1.0F, 1.0F);
			}

			if (worldTime > blockEntity.nextAmbientSoundTime) {
				blockEntity.nextAmbientSoundTime = worldTime
						+ Entity.MAX_RIDING_COOLDOWN
						+ world.getRandom().nextInt(Entity.FREEZING_DAMAGE_INTERVAL);
				world.playSound(null, pos, SoundEvents.BLOCK_CONDUIT_AMBIENT_SHORT, SoundCategory.BLOCKS, 1.0F, 1.0F);
			}
		}
	}

	private static void openEye(ConduitBlockEntity blockEntity, List<BlockPos> activatingBlocks) {
		blockEntity.setEyeOpen(activatingBlocks.size() >= MIN_BLOCKS_TO_ACTIVATE);
	}

	private static boolean updateActivatingBlocks(World world, BlockPos pos, List<BlockPos> activatingBlocks) {
		activatingBlocks.clear();

		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (!world.isWater(pos.add(dx, dy, dz))) {
						return false;
					}
				}
			}
		}

		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = -2; dy <= 2; dy++) {
				for (int dz = -2; dz <= 2; dz++) {
					int absDx = Math.abs(dx);
					int absDy = Math.abs(dy);
					int absDz = Math.abs(dz);
					boolean isFramePos = (absDx > 1 || absDy > 1 || absDz > 1)
							&& (dx == 0 && (absDy == 2 || absDz == 2)
							|| dy == 0 && (absDx == 2 || absDz == 2)
							|| dz == 0 && (absDx == 2 || absDy == 2));

					if (isFramePos) {
						BlockPos framePos = pos.add(dx, dy, dz);
						BlockState frameState = world.getBlockState(framePos);
						for (Block block : ACTIVATING_BLOCKS) {
							if (frameState.isOf(block)) {
								activatingBlocks.add(framePos);
							}
						}
					}
				}
			}
		}

		return activatingBlocks.size() >= MIN_BLOCKS_TO_ACTIVATE_PARTIAL;
	}

	private static void givePlayersEffects(World world, BlockPos pos, List<BlockPos> activatingBlocks) {
		int frameCount = activatingBlocks.size();
		int effectRange = frameCount / 7 * EFFECT_RANGE_PER_BLOCK;
		int posX = pos.getX();
		int posY = pos.getY();
		int posZ = pos.getZ();

		Box searchBox = new Box(posX, posY, posZ, posX + 1, posY + 1, posZ + 1)
				.expand(effectRange)
				.stretch(0.0, world.getHeight(), 0.0);

		for (PlayerEntity player : world.getNonSpectatingEntities(PlayerEntity.class, searchBox)) {
			if (pos.isWithinDistance(player.getBlockPos(), effectRange) && player.isTouchingWaterOrRain()) {
				player.addStatusEffect(new StatusEffectInstance(
						StatusEffects.CONDUIT_POWER,
						CONDUIT_POWER_DURATION,
						0,
						true,
						true
				));
			}
		}
	}

	private static void tryAttack(
			ServerWorld world,
			BlockPos pos,
			BlockState state,
			ConduitBlockEntity blockEntity,
			boolean canAttack
	) {
		LazyEntityReference<LivingEntity> newTarget = getValidTarget(blockEntity.targetEntity, world, pos, canAttack);
		LivingEntity target = LazyEntityReference.getLivingEntity(newTarget, world);

		if (target != null) {
			world.playSound(
					null,
					target.getX(),
					target.getY(),
					target.getZ(),
					SoundEvents.BLOCK_CONDUIT_ATTACK_TARGET,
					SoundCategory.BLOCKS,
					1.0F,
					1.0F
			);
			target.damage(world, world.getDamageSources().magic(), 4.0F);
		}

		if (!Objects.equals(newTarget, blockEntity.targetEntity)) {
			blockEntity.targetEntity = newTarget;
			world.updateListeners(pos, state, state, 2);
		}
	}

	private static @Nullable LazyEntityReference<LivingEntity> getValidTarget(
			@Nullable LazyEntityReference<LivingEntity> currentTarget,
			ServerWorld world,
			BlockPos pos,
			boolean canAttack
	) {
		if (!canAttack) {
			return null;
		}

		if (currentTarget == null) {
			return findAttackTarget(world, pos);
		}

		LivingEntity entity = LazyEntityReference.getLivingEntity(currentTarget, world);
		return entity != null && entity.isAlive() && pos.isWithinDistance(entity.getBlockPos(), EFFECT_RANGE_PER_BLOCK)
				? currentTarget
				: null;
	}

	private static @Nullable LazyEntityReference<LivingEntity> findAttackTarget(ServerWorld world, BlockPos pos) {
		List<LivingEntity> monsters = world.getEntitiesByClass(
				LivingEntity.class,
				new Box(pos).expand(EFFECT_RANGE_PER_BLOCK),
				entity -> entity instanceof Monster && entity.isTouchingWaterOrRain()
		);
		return monsters.isEmpty() ? null : LazyEntityReference.of(Util.getRandom(monsters, world.random));
	}

	private static void spawnNautilusParticles(
			World world,
			BlockPos pos,
			List<BlockPos> activatingBlocks,
			@Nullable Entity entity,
			int ticks
	) {
		Random random = world.random;
		double eyeOffset = MathHelper.sin((ticks + 35) * 0.1F) / 2.0F + 0.5F;
		eyeOffset = (eyeOffset * eyeOffset + eyeOffset) * 0.3F;
		Vec3d eyePos = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.5 + eyeOffset, pos.getZ() + 0.5);

		for (BlockPos framePos : activatingBlocks) {
			if (random.nextInt(50) == 0) {
				BlockPos relative = framePos.subtract(pos);
				float velX = -0.5F + random.nextFloat() + relative.getX();
				float velY = -2.0F + random.nextFloat() + relative.getY();
				float velZ = -0.5F + random.nextFloat() + relative.getZ();
				world.addParticleClient(ParticleTypes.NAUTILUS, eyePos.x, eyePos.y, eyePos.z, velX, velY, velZ);
			}
		}

		if (entity != null) {
			Vec3d entityEye = new Vec3d(entity.getX(), entity.getEyeY(), entity.getZ());
			float velX = (-0.5F + random.nextFloat()) * (3.0F + entity.getWidth());
			float velY = -1.0F + random.nextFloat() * entity.getHeight();
			float velZ = (-0.5F + random.nextFloat()) * (3.0F + entity.getWidth());
			world.addParticleClient(ParticleTypes.NAUTILUS, entityEye.x, entityEye.y, entityEye.z, velX, velY, velZ);
		}
	}

	public boolean isActive() {
		return active;
	}

	public boolean isEyeOpen() {
		return eyeOpen;
	}

	private void setEyeOpen(boolean eyeOpen) {
		this.eyeOpen = eyeOpen;
	}

	public float getRotation(float tickProgress) {
		return (ticksActive + tickProgress) * EYE_ROTATION_SPEED;
	}
}
