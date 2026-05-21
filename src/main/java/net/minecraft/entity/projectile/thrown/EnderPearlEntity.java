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
 * {@code EnderPearlEntity}.
 */
public class EnderPearlEntity extends ThrownItemEntity {

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
		this.removeFromOwner();
		super.setOwner(owner);
		this.addToOwner();
	}

	private void removeFromOwner() {
		if (this.getOwner() instanceof ServerPlayerEntity serverPlayerEntity) {
			serverPlayerEntity.removeEnderPearl(this);
		}
	}

	private void addToOwner() {
		if (this.getOwner() instanceof ServerPlayerEntity serverPlayerEntity) {
			serverPlayerEntity.addEnderPearl(this);
		}
	}

	@Override
	public @Nullable Entity getOwner() {
		return this.owner != null && this.getEntityWorld() instanceof ServerWorld serverWorld
		       ? this.owner.getEntityByClass(serverWorld, Entity.class)
		       : super.getOwner();
	}

	private static @Nullable Entity getPlayer(ServerWorld world, UUID uuid) {
		Entity entity = world.getEntityAnyDimension(uuid);
		return (Entity) (entity != null ? entity : world.getServer().getPlayerManager().getPlayer(uuid));
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		super.onEntityHit(entityHitResult);
		entityHitResult.getEntity().serverDamage(this.getDamageSources().thrown(this, this.getOwner()), 0.0F);
	}

	@Override
	protected void onCollision(HitResult hitResult) {
		super.onCollision(hitResult);

		for (int i = 0; i < 32; i++) {
			this.getEntityWorld()
			    .addParticleClient(
					    ParticleTypes.PORTAL,
					    this.getX(),
					    this.getY() + this.random.nextDouble() * 2.0,
					    this.getZ(),
					    this.random.nextGaussian(),
					    0.0,
					    this.random.nextGaussian()
			    );
		}

		if (this.getEntityWorld() instanceof ServerWorld serverWorld && !this.isRemoved()) {
			Entity entity = this.getOwner();
			if (entity != null && canTeleportEntityTo(entity, serverWorld)) {
				Vec3d vec3d = this.getLastRenderPos();
				if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
					if (serverPlayerEntity.networkHandler.isConnectionOpen()) {
						if (this.random.nextFloat() < 0.05F && serverWorld.shouldSpawnMonsters()) {
							EndermiteEntity
									endermiteEntity =
									EntityType.ENDERMITE.create(serverWorld, SpawnReason.TRIGGERED);
							if (endermiteEntity != null) {
								endermiteEntity.refreshPositionAndAngles(
										entity.getX(),
										entity.getY(),
										entity.getZ(),
										entity.getYaw(),
										entity.getPitch()
								);
								serverWorld.spawnEntity(endermiteEntity);
							}
						}

						if (this.hasPortalCooldown()) {
							entity.resetPortalCooldown();
						}

						ServerPlayerEntity serverPlayerEntity2 = serverPlayerEntity.teleportTo(
								new TeleportTarget(
										serverWorld,
										vec3d,
										Vec3d.ZERO,
										0.0F,
										0.0F,
										PositionFlag.combine(PositionFlag.ROT, PositionFlag.DELTA),
										TeleportTarget.NO_OP
								)
						);
						if (serverPlayerEntity2 != null) {
							serverPlayerEntity2.onLanding();
							serverPlayerEntity2.clearCurrentExplosion();
							serverPlayerEntity2.damage(
									serverPlayerEntity.getEntityWorld(),
									this.getDamageSources().enderPearl(),
									5.0F
							);
						}

						this.playTeleportSound(serverWorld, vec3d);
					}
				}
				else {
					Entity entity2 = entity.teleportTo(
							new TeleportTarget(
									serverWorld,
									vec3d,
									entity.getVelocity(),
									entity.getYaw(),
									entity.getPitch(),
									TeleportTarget.NO_OP
							)
					);
					if (entity2 != null) {
						entity2.onLanding();
					}

					this.playTeleportSound(serverWorld, vec3d);
				}

				this.discard();
			}
			else {
				this.discard();
			}
		}
	}

	private static boolean canTeleportEntityTo(Entity entity, World world) {
		if (entity.getEntityWorld().getRegistryKey() == world.getRegistryKey()) {
			return !(entity instanceof LivingEntity livingEntity) ? entity.isAlive() : livingEntity.isAlive()
			                                                                           && !livingEntity.isSleeping();
		}
		else {
			return entity.canUsePortals(true);
		}
	}

	@Override
	public void tick() {
		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			int var7 = ChunkSectionPos.getSectionCoordFloored(this.getEntityPos().getX());
			int j = ChunkSectionPos.getSectionCoordFloored(this.getEntityPos().getZ());
			Entity entity = this.owner != null ? getPlayer(serverWorld, this.owner.getUuid()) : null;
			if (entity instanceof ServerPlayerEntity serverPlayerEntity
					&& !entity.isAlive()
					&& !serverPlayerEntity.notInAnyWorld
					&& serverPlayerEntity
					.getEntityWorld()
					.getGameRules()
					.getValue(GameRules.ENDER_PEARLS_VANISH_ON_DEATH)) {
				this.discard();
			}
			else {
				super.tick();
			}

			if (this.isAlive()) {
				BlockPos blockPos = BlockPos.ofFloored(this.getEntityPos());
				if ((
						--this.chunkTicketExpiryTicks <= 0L
								|| var7 != ChunkSectionPos.getSectionCoord(blockPos.getX())
								|| j != ChunkSectionPos.getSectionCoord(blockPos.getZ())
				)
						&& entity instanceof ServerPlayerEntity serverPlayerEntity2) {
					this.chunkTicketExpiryTicks = serverPlayerEntity2.handleThrownEnderPearl(this);
				}
			}
		}
		else {
			super.tick();
		}
	}

	private void playTeleportSound(World world, Vec3d pos) {
		world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_PLAYER_TELEPORT, SoundCategory.PLAYERS);
	}

	@Override
	public @Nullable Entity teleportTo(TeleportTarget teleportTarget) {
		Entity entity = super.teleportTo(teleportTarget);
		if (entity != null) {
			entity.addPortalChunkTicketAt(BlockPos.ofFloored(entity.getEntityPos()));
		}

		return entity;
	}

	@Override
	public boolean canTeleportBetween(World from, World to) {
		return from.getRegistryKey() == World.END && to.getRegistryKey() == World.OVERWORLD
				       && this.getOwner() instanceof ServerPlayerEntity serverPlayerEntity
		       ? super.canTeleportBetween(from, to) && serverPlayerEntity.seenCredits
		       : super.canTeleportBetween(from, to);
	}

	@Override
	protected void onBlockCollision(BlockState state) {
		super.onBlockCollision(state);
		if (state.isOf(Blocks.END_GATEWAY) && this.getOwner() instanceof ServerPlayerEntity serverPlayerEntity) {
			serverPlayerEntity.onBlockCollision(state);
		}
	}

	@Override
	public void onRemove(Entity.RemovalReason reason) {
		if (reason != Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
			this.removeFromOwner();
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
