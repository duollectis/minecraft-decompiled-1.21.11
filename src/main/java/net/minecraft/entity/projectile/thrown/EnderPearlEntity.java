package net.minecraft.entity.projectile.thrown;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.EndermiteEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Жемчуг Эндера — бросаемый снаряд, телепортирующий владельца к точке попадания.
 * <p>
 * При телепортации игрока с шансом {@link #ENDERMITE_SPAWN_CHANCE} спавнится эндермит.
 * Жемчуг отслеживает чанки через {@link ServerPlayerEntity#handleThrownEnderPearl},
 * чтобы не выгружать чанки по пути полёта. При смерти игрока (если включено правило
 * {@link GameRules#ENDER_PEARLS_VANISH_ON_DEATH}) жемчуг уничтожается.
 */
public class EnderPearlEntity extends ThrownItemEntity {

	/** Урон от телепортации через жемчуг. */
	private static final float TELEPORT_DAMAGE = 5.0F;

	/** Вероятность спавна эндермита при телепортации (1 из N). */
	private static final int ENDERMITE_SPAWN_CHANCE = 20;

	/** Количество портальных частиц при столкновении. */
	private static final int PORTAL_PARTICLE_COUNT = 32;

	/** Вертикальный разброс портальных частиц. */
	private static final double PORTAL_PARTICLE_Y_SPREAD = 2.0;

	private long chunkTicketExpiryTicks = 0L;

	public EnderPearlEntity(EntityType<? extends EnderPearlEntity> entityType, World world) {
		super(entityType, world);
	}

	public EnderPearlEntity(World world, LivingEntity owner, ItemStack stack) {
		super(EntityType.ENDER_PEARL, owner, world, stack);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.ENDER_PEARL;
	}

	@Override
	protected void setOwner(@Nullable LazyEntityReference<Entity> owner) {
		removeFromOwner();
		super.setOwner(owner);
		addToOwner();
	}

	private void removeFromOwner() {
		if (getOwner() instanceof ServerPlayerEntity serverPlayer) {
			serverPlayer.removeEnderPearl(this);
		}
	}

	private void addToOwner() {
		if (getOwner() instanceof ServerPlayerEntity serverPlayer) {
			serverPlayer.addEnderPearl(this);
		}
	}

	@Override
	public @Nullable Entity getOwner() {
		return owner != null && getEntityWorld() instanceof ServerWorld serverWorld
				? owner.getEntityByClass(serverWorld, Entity.class)
				: super.getOwner();
	}

	private static @Nullable Entity getPlayer(ServerWorld world, UUID uuid) {
		Entity entity = world.getEntityAnyDimension(uuid);
		return entity != null ? entity : world.getServer().getPlayerManager().getPlayer(uuid);
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		super.onEntityHit(entityHitResult);
		entityHitResult.getEntity().serverDamage(getDamageSources().thrown(this, getOwner()), 0.0F);
	}

	@Override
	protected void onCollision(HitResult hitResult) {
		super.onCollision(hitResult);
		spawnPortalParticles();

		if (!(getEntityWorld() instanceof ServerWorld serverWorld) || isRemoved()) {
			return;
		}

		Entity owner = getOwner();
		if (owner == null || !canTeleportEntityTo(owner, serverWorld)) {
			discard();
			return;
		}

		Vec3d destination = getLastRenderPos();

		if (owner instanceof ServerPlayerEntity serverPlayer) {
			teleportPlayer(serverPlayer, serverWorld, destination);
		} else {
			teleportEntity(owner, serverWorld, destination);
		}

		discard();
	}

	private void spawnPortalParticles() {
		for (int index = 0; index < PORTAL_PARTICLE_COUNT; index++) {
			getEntityWorld().addParticleClient(
					ParticleTypes.PORTAL,
					getX(),
					getY() + random.nextDouble() * PORTAL_PARTICLE_Y_SPREAD,
					getZ(),
					random.nextGaussian(),
					0.0,
					random.nextGaussian()
			);
		}
	}

	private void teleportPlayer(ServerPlayerEntity player, ServerWorld world, Vec3d destination) {
		if (!player.networkHandler.isConnectionOpen()) {
			return;
		}

		trySpawnEndermite(player, world, destination);

		if (hasPortalCooldown()) {
			player.resetPortalCooldown();
		}

		ServerPlayerEntity teleported = player.teleportTo(new TeleportTarget(
				world,
				destination,
				Vec3d.ZERO,
				0.0F,
				0.0F,
				PositionFlag.combine(PositionFlag.ROT, PositionFlag.DELTA),
				TeleportTarget.NO_OP
		));

		if (teleported != null) {
			teleported.onLanding();
			teleported.clearCurrentExplosion();
			teleported.damage(
					player.getEntityWorld(),
					getDamageSources().enderPearl(),
					TELEPORT_DAMAGE
			);
		}

		playTeleportSound(world, destination);
	}

	private void trySpawnEndermite(ServerPlayerEntity player, ServerWorld world, Vec3d destination) {
		if (random.nextFloat() >= 1.0F / ENDERMITE_SPAWN_CHANCE || !world.shouldSpawnMonsters()) {
			return;
		}

		EndermiteEntity endermite = EntityType.ENDERMITE.create(world, SpawnReason.TRIGGERED);
		if (endermite != null) {
			endermite.refreshPositionAndAngles(
					player.getX(),
					player.getY(),
					player.getZ(),
					player.getYaw(),
					player.getPitch()
			);
			world.spawnEntity(endermite);
		}
	}

	private void teleportEntity(Entity entity, ServerWorld world, Vec3d destination) {
		Entity teleported = entity.teleportTo(new TeleportTarget(
				world,
				destination,
				entity.getVelocity(),
				entity.getYaw(),
				entity.getPitch(),
				TeleportTarget.NO_OP
		));

		if (teleported != null) {
			teleported.onLanding();
		}

		playTeleportSound(world, destination);
	}

	private static boolean canTeleportEntityTo(Entity entity, World world) {
		if (entity.getEntityWorld().getRegistryKey() != world.getRegistryKey()) {
			return entity.canUsePortals(true);
		}

		return entity instanceof LivingEntity living
				? living.isAlive() && !living.isSleeping()
				: entity.isAlive();
	}

	@Override
	public void tick() {
		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			int sectionX = ChunkSectionPos.getSectionCoordFloored(getEntityPos().getX());
			int sectionZ = ChunkSectionPos.getSectionCoordFloored(getEntityPos().getZ());
			Entity ownerEntity = owner != null ? getPlayer(serverWorld, owner.getUuid()) : null;

			if (ownerEntity instanceof ServerPlayerEntity serverPlayer
					&& !ownerEntity.isAlive()
					&& !serverPlayer.notInAnyWorld
					&& serverPlayer.getEntityWorld().getGameRules().getValue(GameRules.ENDER_PEARLS_VANISH_ON_DEATH)) {
				discard();
				return;
			}

			super.tick();

			if (isAlive()) {
				BlockPos currentBlock = BlockPos.ofFloored(getEntityPos());
				boolean sectionChanged = sectionX != ChunkSectionPos.getSectionCoord(currentBlock.getX())
						|| sectionZ != ChunkSectionPos.getSectionCoord(currentBlock.getZ());

				if ((--chunkTicketExpiryTicks <= 0L || sectionChanged)
						&& ownerEntity instanceof ServerPlayerEntity serverPlayer2) {
					chunkTicketExpiryTicks = serverPlayer2.handleThrownEnderPearl(this);
				}
			}
		} else {
			super.tick();
		}
	}

	private void playTeleportSound(World world, Vec3d pos) {
		world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_PLAYER_TELEPORT, SoundCategory.PLAYERS);
	}

	@Override
	public @Nullable Entity teleportTo(TeleportTarget teleportTarget) {
		Entity teleported = super.teleportTo(teleportTarget);
		if (teleported != null) {
			teleported.addPortalChunkTicketAt(BlockPos.ofFloored(teleported.getEntityPos()));
		}

		return teleported;
	}

	@Override
	public boolean canTeleportBetween(World from, World to) {
		boolean isEndToOverworld = from.getRegistryKey() == World.END && to.getRegistryKey() == World.OVERWORLD;
		if (isEndToOverworld && getOwner() instanceof ServerPlayerEntity serverPlayer) {
			return super.canTeleportBetween(from, to) && serverPlayer.seenCredits;
		}

		return super.canTeleportBetween(from, to);
	}

	@Override
	protected void onBlockCollision(BlockState state) {
		super.onBlockCollision(state);
		if (state.isOf(Blocks.END_GATEWAY) && getOwner() instanceof ServerPlayerEntity serverPlayer) {
			serverPlayer.onBlockCollision(state);
		}
	}

	@Override
	public void onRemove(Entity.RemovalReason reason) {
		if (reason != Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
			removeFromOwner();
		}

		super.onRemove(reason);
	}

	@Override
	public void onBubbleColumnSurfaceCollision(boolean drag, BlockPos pos) {
		Entity.applyBubbleColumnSurfaceEffects(this, drag, pos);
	}

	@Override
	public void onBubbleColumnCollision(boolean drag) {
		Entity.applyBubbleColumnEffects(this, drag);
	}
}
