package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.EndGatewayBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Блок портала края (End Gateway). Телепортирует сущности и жемчуг эндера
 * к выходному порталу, хранящемуся в {@link EndGatewayBlockEntity}.
 */
public class EndGatewayBlock extends BlockWithEntity implements Portal {

	public static final MapCodec<EndGatewayBlock> CODEC = createCodec(EndGatewayBlock::new);

	@Override
	public MapCodec<EndGatewayBlock> getCodec() {
		return CODEC;
	}

	public EndGatewayBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new EndGatewayBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
			World world,
			BlockState state,
			BlockEntityType<T> type
	) {
		return validateTicker(
				type,
				BlockEntityType.END_GATEWAY,
				world.isClient() ? EndGatewayBlockEntity::clientTick : EndGatewayBlockEntity::serverTick
		);
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (world.getBlockEntity(pos) instanceof EndGatewayBlockEntity gateway) {
			int sideCount = gateway.getDrawnSidesCount();

			for (int i = 0; i < sideCount; i++) {
				double x = pos.getX() + random.nextDouble();
				double y = pos.getY() + random.nextDouble();
				double z = pos.getZ() + random.nextDouble();
				double velX = (random.nextDouble() - 0.5) * 0.5;
				double velY = (random.nextDouble() - 0.5) * 0.5;
				double velZ = (random.nextDouble() - 0.5) * 0.5;
				int side = random.nextInt(2) * 2 - 1;

				if (random.nextBoolean()) {
					z = pos.getZ() + 0.5 + 0.25 * side;
					velZ = random.nextFloat() * 2.0F * side;
				} else {
					x = pos.getX() + 0.5 + 0.25 * side;
					velX = random.nextFloat() * 2.0F * side;
				}

				world.addParticleClient(ParticleTypes.PORTAL, x, y, z, velX, velY, velZ);
			}
		}
	}

	@Override
	protected ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
		return ItemStack.EMPTY;
	}

	@Override
	protected boolean canBucketPlace(BlockState state, Fluid fluid) {
		return false;
	}

	@Override
	protected void onEntityCollision(
			BlockState state,
			World world,
			BlockPos pos,
			Entity entity,
			EntityCollisionHandler handler,
			boolean bl
	) {
		if (entity.canUsePortals(false) == false || world.isClient()) {
			return;
		}

		if (world.getBlockEntity(pos) instanceof EndGatewayBlockEntity gateway
				&& gateway.needsCooldownBeforeTeleporting() == false
		) {
			entity.tryUsePortal(this, pos);
			EndGatewayBlockEntity.startTeleportCooldown(world, pos, state, gateway);
		}
	}

	@Override
	public @Nullable TeleportTarget createTeleportTarget(ServerWorld world, Entity entity, BlockPos pos) {
		BlockEntity blockEntity = world.getBlockEntity(pos);

		if (!(blockEntity instanceof EndGatewayBlockEntity gateway)) {
			return null;
		}

		Vec3d exitPos = gateway.getOrCreateExitPortalPos(world, pos);

		if (exitPos == null) {
			return null;
		}

		return entity instanceof EnderPearlEntity
				? new TeleportTarget(
						world,
						exitPos,
						Vec3d.ZERO,
						0.0F,
						0.0F,
						Set.of(),
						TeleportTarget.ADD_PORTAL_CHUNK_TICKET
				)
				: new TeleportTarget(
						world,
						exitPos,
						Vec3d.ZERO,
						0.0F,
						0.0F,
						PositionFlag.combine(PositionFlag.DELTA, PositionFlag.ROT),
						TeleportTarget.ADD_PORTAL_CHUNK_TICKET
				);
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.INVISIBLE;
	}
}
