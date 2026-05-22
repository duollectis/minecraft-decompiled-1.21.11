package net.minecraft.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.AutomaticItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.function.Predicate;

/**
 * Сущность падающего блока. Создаётся при обрушении гравитационных блоков (песок, гравий, наковальня).
 * При приземлении пытается разместить блок обратно в мире; если не удаётся — выпадает предметом.
 * Поддерживает урон существам при падении ({@code hurtEntities}) и сохранение данных блочной сущности.
 */
public class FallingBlockEntity extends Entity {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final BlockState DEFAULT_BLOCK_STATE = Blocks.SAND.getDefaultState();
	private static final int DEFAULT_TIME = 0;
	private static final float DEFAULT_FALL_HURT_AMOUNT = 0.0F;
	private static final int DEFAULT_FALL_HURT_MAX = 40;
	private static final boolean DEFAULT_DROP_ITEM = true;
	private static final boolean DEFAULT_DESTROYED_ON_LANDING = false;
	private BlockState blockState = DEFAULT_BLOCK_STATE;
	public int timeFalling = 0;
	public boolean dropItem = true;
	private boolean destroyedOnLanding = false;
	private boolean hurtEntities;
	private int fallHurtMax = DEFAULT_FALL_HURT_MAX;
	private float fallHurtAmount = 0.0F;
	public @Nullable NbtCompound blockEntityData;
	public boolean shouldDupe;
	protected static final TrackedData<BlockPos>
			BLOCK_POS =
			DataTracker.registerData(FallingBlockEntity.class, TrackedDataHandlerRegistry.BLOCK_POS);

	public FallingBlockEntity(EntityType<? extends FallingBlockEntity> entityType, World world) {
		super(entityType, world);
	}

	private FallingBlockEntity(World world, double x, double y, double z, BlockState blockState) {
		this(EntityType.FALLING_BLOCK, world);
		this.blockState = blockState;
		intersectionChecked = true;
		setPosition(x, y, z);
		setVelocity(Vec3d.ZERO);
		lastX = x;
		lastY = y;
		lastZ = z;
		setFallingBlockPos(getBlockPos());
	}

	/**
	 * Создаёт падающий блок из блока в мире: удаляет блок из мира (заменяя его состоянием жидкости),
	 * спавнит сущность по центру блока и возвращает её.
	 *
	 * @param world мир, в котором происходит спавн
	 * @param pos позиция блока, который начинает падать
	 * @param state состояние блока (WATERLOGGED сбрасывается)
	 * @return созданная и заспавненная сущность падающего блока
	 */
	public static FallingBlockEntity spawnFromBlock(World world, BlockPos pos, BlockState state) {
		FallingBlockEntity fallingBlockEntity = new FallingBlockEntity(
				world,
				pos.getX() + 0.5,
				pos.getY(),
				pos.getZ() + 0.5,
				state.contains(Properties.WATERLOGGED) ? state.with(Properties.WATERLOGGED, false) : state
		);
		world.setBlockState(pos, state.getFluidState().getBlockState(), 3);
		world.spawnEntity(fallingBlockEntity);
		return fallingBlockEntity;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public final boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (!this.isAlwaysInvulnerableTo(source)) {
			this.scheduleVelocityUpdate();
		}

		return false;
	}

	public void setFallingBlockPos(BlockPos pos) {
		dataTracker.set(BLOCK_POS, pos);
	}

	public BlockPos getFallingBlockPos() {
		return dataTracker.get(BLOCK_POS);
	}

	@Override
	protected Entity.MoveEffect getMoveEffect() {
		return Entity.MoveEffect.NONE;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(BLOCK_POS, BlockPos.ORIGIN);
	}

	@Override
	public boolean canHit() {
		return !this.isRemoved();
	}

	@Override
	protected double getGravity() {
		return 0.04;
	}

	@Override
	public void tick() {
		if (blockState.isAir()) {
			discard();
			return;
		}

		Block block = blockState.getBlock();
		timeFalling++;
		applyGravity();
		move(MovementType.SELF, getVelocity());
		tickBlockCollision();
		tickPortalTeleportation();

		if (getEntityWorld() instanceof ServerWorld serverWorld && (isAlive() || shouldDupe)) {
			serverTick(serverWorld, block);
		}

		setVelocity(getVelocity().multiply(0.98));
	}

	private void serverTick(ServerWorld serverWorld, Block block) {
		BlockPos landingPos = getBlockPos();
		boolean isConcretePowder = blockState.getBlock() instanceof ConcretePowderBlock;
		boolean touchesWater = isConcretePowder && getEntityWorld().getFluidState(landingPos).isIn(FluidTags.WATER);

		if (isConcretePowder && getVelocity().lengthSquared() > 1.0) {
			BlockHitResult waterHit = getEntityWorld().raycast(new RaycastContext(
				new Vec3d(lastX, lastY, lastZ),
				getEntityPos(),
				RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.SOURCE_ONLY,
				this
			));
			if (waterHit.getType() != HitResult.Type.MISS
				&& getEntityWorld().getFluidState(waterHit.getBlockPos()).isIn(FluidTags.WATER)
			) {
				landingPos = waterHit.getBlockPos();
				touchesWater = true;
			}
		}

		if (!isOnGround() && !touchesWater) {
			boolean outOfBounds = landingPos.getY() <= getEntityWorld().getBottomY()
				|| landingPos.getY() > getEntityWorld().getTopYInclusive();
			if ((timeFalling > 100 && outOfBounds) || timeFalling > 600) {
				if (dropItem && serverWorld.getGameRules().getValue(GameRules.ENTITY_DROPS)) {
					dropItem(serverWorld, block);
				}

				discard();
			}

			return;
		}

		BlockState landedOnState = getEntityWorld().getBlockState(landingPos);
		setVelocity(getVelocity().multiply(0.7, -0.5, 0.7));

		if (landedOnState.isOf(Blocks.MOVING_PISTON)) {
			return;
		}

		if (destroyedOnLanding) {
			discard();
			onDestroyedOnLanding(block, landingPos);
			return;
		}

		boolean canReplace = landedOnState.canReplace(new AutomaticItemPlacementContext(
			getEntityWorld(), landingPos, Direction.DOWN, ItemStack.EMPTY, Direction.UP
		));
		boolean fallsThrough = FallingBlock.canFallThrough(getEntityWorld().getBlockState(landingPos.down()))
			&& (!isConcretePowder || !touchesWater);
		boolean canPlace = blockState.canPlaceAt(getEntityWorld(), landingPos) && !fallsThrough;

		if (canReplace && canPlace) {
			placeBlock(serverWorld, block, landingPos, landedOnState);
		}
		else {
			discard();
			if (dropItem && serverWorld.getGameRules().getValue(GameRules.ENTITY_DROPS)) {
				onDestroyedOnLanding(block, landingPos);
				dropItem(serverWorld, block);
			}
		}
	}

	/**
	 * Размещает блок в мире после приземления, синхронизирует состояние с клиентами
	 * и восстанавливает данные блочной сущности из сохранённого NBT.
	 */
	private void placeBlock(ServerWorld serverWorld, Block block, BlockPos pos, BlockState landedOnState) {
		if (blockState.contains(Properties.WATERLOGGED)
			&& getEntityWorld().getFluidState(pos).getFluid() == Fluids.WATER
		) {
			blockState = blockState.with(Properties.WATERLOGGED, true);
		}

		if (!getEntityWorld().setBlockState(pos, blockState, 3)) {
			if (dropItem && serverWorld.getGameRules().getValue(GameRules.ENTITY_DROPS)) {
				discard();
				onDestroyedOnLanding(block, pos);
				dropItem(serverWorld, block);
			}

			return;
		}

		serverWorld.getChunkManager().chunkLoadingManager.sendToOtherNearbyPlayers(
			this, new BlockUpdateS2CPacket(pos, getEntityWorld().getBlockState(pos))
		);
		discard();

		if (block instanceof Falling falling) {
			falling.onLanding(getEntityWorld(), pos, blockState, landedOnState, this);
		}

		if (blockEntityData != null && blockState.hasBlockEntity()) {
			restoreBlockEntityData(serverWorld, pos);
		}
	}

	private void restoreBlockEntityData(ServerWorld serverWorld, BlockPos pos) {
		BlockEntity blockEntity = getEntityWorld().getBlockEntity(pos);
		if (blockEntity == null) {
			return;
		}

		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(blockEntity.getReporterContext(), LOGGER)) {
			DynamicRegistryManager registryManager = getEntityWorld().getRegistryManager();
			NbtWriteView writeView = NbtWriteView.create(logging, registryManager);
			blockEntity.writeDataWithoutId(writeView);
			NbtCompound merged = writeView.getNbt();
			blockEntityData.forEach((key, value) -> merged.put(key, value.copy()));
			blockEntity.read(NbtReadView.create(logging, registryManager, merged));
		}
		catch (Exception error) {
			LOGGER.error("Failed to load block entity from falling block", error);
		}

		blockEntity.markDirty();
	}

	public void onDestroyedOnLanding(Block block, BlockPos pos) {
		if (block instanceof Falling falling) {
			falling.onDestroyedOnLanding(getEntityWorld(), pos, this);
		}
	}

	@Override
	public boolean handleFallDamage(double fallDistance, float damagePerDistance, DamageSource damageSource) {
		if (!hurtEntities) {
			return false;
		}

		int fallTicks = MathHelper.ceil(fallDistance - 1.0);
		if (fallTicks < 0) {
			return false;
		}

		Predicate<Entity> predicate = EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.and(EntityPredicates.VALID_LIVING_ENTITY);
		DamageSource blockDamageSource = blockState.getBlock() instanceof Falling falling
			? falling.getDamageSource(this)
			: getDamageSources().fallingBlock(this);
		float damage = Math.min(MathHelper.floor(fallTicks * fallHurtAmount), fallHurtMax);
		getEntityWorld()
			.getOtherEntities(this, getBoundingBox(), predicate)
			.forEach(entity -> entity.serverDamage(blockDamageSource, damage));

		boolean isAnvil = blockState.isIn(BlockTags.ANVIL);
		if (isAnvil && damage > 0.0F && random.nextFloat() < 0.05F + fallTicks * 0.05F) {
			BlockState landingState = AnvilBlock.getLandingState(blockState);
			if (landingState == null) {
				destroyedOnLanding = true;
			}
			else {
				blockState = landingState;
			}
		}

		return false;
	}

	@Override
	protected void writeCustomData(WriteView view) {
		view.put("BlockState", BlockState.CODEC, blockState);
		view.putInt("Time", timeFalling);
		view.putBoolean("DropItem", dropItem);
		view.putBoolean("HurtEntities", hurtEntities);
		view.putFloat("FallHurtAmount", fallHurtAmount);
		view.putInt("FallHurtMax", fallHurtMax);
		if (blockEntityData != null) {
			view.put("TileEntityData", NbtCompound.CODEC, blockEntityData);
		}

		view.putBoolean("CancelDrop", destroyedOnLanding);
	}

	@Override
	protected void readCustomData(ReadView view) {
		blockState = view.<BlockState>read("BlockState", BlockState.CODEC).orElse(DEFAULT_BLOCK_STATE);
		timeFalling = view.getInt("Time", DEFAULT_TIME);
		boolean isAnvil = blockState.isIn(BlockTags.ANVIL);
		hurtEntities = view.getBoolean("HurtEntities", isAnvil);
		fallHurtAmount = view.getFloat("FallHurtAmount", DEFAULT_FALL_HURT_AMOUNT);
		fallHurtMax = view.getInt("FallHurtMax", DEFAULT_FALL_HURT_MAX);
		dropItem = view.getBoolean("DropItem", DEFAULT_DROP_ITEM);
		blockEntityData = view.<NbtCompound>read("TileEntityData", NbtCompound.CODEC).orElse(null);
		destroyedOnLanding = view.getBoolean("CancelDrop", DEFAULT_DESTROYED_ON_LANDING);
	}

	public void setHurtEntities(float fallHurtAmount, int fallHurtMax) {
		hurtEntities = true;
		this.fallHurtAmount = fallHurtAmount;
		this.fallHurtMax = fallHurtMax;
	}

	public void setDestroyedOnLanding() {
		destroyedOnLanding = true;
	}

	@Override
	public boolean doesRenderOnFire() {
		return false;
	}

	@Override
	public void populateCrashReport(CrashReportSection section) {
		super.populateCrashReport(section);
		section.add("Immitating BlockState", blockState.toString());
	}

	public BlockState getBlockState() {
		return blockState;
	}

	@Override
	protected Text getDefaultName() {
		return Text.translatable("entity.minecraft.falling_block_type", blockState.getBlock().getName());
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entityTrackerEntry) {
		return new EntitySpawnS2CPacket(this, entityTrackerEntry, Block.getRawIdFromState(getBlockState()));
	}

	@Override
	public void onSpawnPacket(EntitySpawnS2CPacket packet) {
		super.onSpawnPacket(packet);
		blockState = Block.getStateFromRawId(packet.getEntityData());
		intersectionChecked = true;
		setPosition(packet.getX(), packet.getY(), packet.getZ());
		setFallingBlockPos(getBlockPos());
	}

	@Override
	public @Nullable Entity teleportTo(TeleportTarget teleportTarget) {
		RegistryKey<World> targetWorld = teleportTarget.world().getRegistryKey();
		RegistryKey<World> currentWorld = getEntityWorld().getRegistryKey();
		boolean crossesEndBoundary = (currentWorld == World.END || targetWorld == World.END)
			&& currentWorld != targetWorld;
		Entity teleported = super.teleportTo(teleportTarget);
		shouldDupe = teleported != null && crossesEndBoundary;
		return teleported;
	}
}
