package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Блок мёда, замедляющий скольжение сущностей по боковым граням.
 * <p>При скольжении по стенке блока вертикальная скорость сущности ограничивается
 * константой {@code MAX_SLIDE_VELOCITY}. Эффект применяется только к живым существам,
 * вагонеткам, лодкам и блокам ТНТ.</p>
 */
public class HoneyBlock extends TranslucentBlock {

	public static final MapCodec<HoneyBlock> CODEC = createCodec(HoneyBlock::new);
	private static final double MAX_SLIDE_VELOCITY = 0.13;
	private static final double GRAVITY_CONSTANT = 0.08;
	private static final double SLIDE_VELOCITY_CLAMP = 0.05;
	private static final int TICKS_PER_SECOND = 20;
	private static final VoxelShape SHAPE = Block.createColumnShape(14.0, 0.0, 15.0);

	@Override
	public MapCodec<HoneyBlock> getCodec() {
		return CODEC;
	}

	public HoneyBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	private static boolean hasHoneyBlockEffects(Entity entity) {
		return entity instanceof LivingEntity || entity instanceof AbstractMinecartEntity || entity instanceof TntEntity
				|| entity instanceof AbstractBoatEntity;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
		entity.playSound(SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
		if (!world.isClient()) {
			world.sendEntityStatus(entity, (byte) 54);
		}

		if (entity.handleFallDamage(fallDistance, 0.2F, world.getDamageSources().fall())) {
			entity.playSound(
					this.soundGroup.getFallSound(),
					this.soundGroup.getVolume() * 0.5F,
					this.soundGroup.getPitch() * 0.75F
			);
		}
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
		if (this.isSliding(pos, entity)) {
			this.triggerAdvancement(entity, pos);
			this.updateSlidingVelocity(entity);
			this.addCollisionEffects(world, entity);
		}

		super.onEntityCollision(state, world, pos, entity, handler, bl);
	}

	private static double getOldVelocityY(double d) {
		return d / 0.98F + GRAVITY_CONSTANT;
	}

	private static double getNewVelocityY(double d) {
		return (d - GRAVITY_CONSTANT) * 0.98F;
	}

	private boolean isSliding(BlockPos pos, Entity entity) {
		if (entity.isOnGround()) {
			return false;
		}
		else if (entity.getY() > pos.getY() + 0.9375 - 1.0E-7) {
			return false;
		}
		else if (getOldVelocityY(entity.getVelocity().y) >= -GRAVITY_CONSTANT) {
			return false;
		}
		else {
			double d = Math.abs(pos.getX() + 0.5 - entity.getX());
			double e = Math.abs(pos.getZ() + 0.5 - entity.getZ());
			double f = 0.4375 + entity.getWidth() / 2.0F;
			return d + 1.0E-7 > f || e + 1.0E-7 > f;
		}
	}

	private void triggerAdvancement(Entity entity, BlockPos pos) {
		if (entity instanceof ServerPlayerEntity && entity.getEntityWorld().getTime() % TICKS_PER_SECOND == 0L) {
			Criteria.SLIDE_DOWN_BLOCK.trigger((ServerPlayerEntity) entity, entity.getEntityWorld().getBlockState(pos));
		}
	}

	private void updateSlidingVelocity(Entity entity) {
		Vec3d vec3d = entity.getVelocity();
		if (getOldVelocityY(entity.getVelocity().y) < -MAX_SLIDE_VELOCITY) {
			double d = -SLIDE_VELOCITY_CLAMP / getOldVelocityY(entity.getVelocity().y);
			entity.setVelocity(new Vec3d(vec3d.x * d, getNewVelocityY(-SLIDE_VELOCITY_CLAMP), vec3d.z * d));
		}
		else {
			entity.setVelocity(new Vec3d(vec3d.x, getNewVelocityY(-SLIDE_VELOCITY_CLAMP), vec3d.z));
		}

		entity.onLanding();
	}

	private void addCollisionEffects(World world, Entity entity) {
		if (hasHoneyBlockEffects(entity)) {
			if (world.random.nextInt(5) == 0) {
				entity.playSound(SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
			}

			if (!world.isClient() && world.random.nextInt(5) == 0) {
				world.sendEntityStatus(entity, (byte) 53);
			}
		}
	}

	public static void addRegularParticles(Entity entity) {
		addParticles(entity, 5);
	}

	public static void addRichParticles(Entity entity) {
		addParticles(entity, 10);
	}

	private static void addParticles(Entity entity, int count) {
		if (entity.getEntityWorld().isClient()) {
			BlockState blockState = Blocks.HONEY_BLOCK.getDefaultState();

			for (int i = 0; i < count; i++) {
				entity.getEntityWorld()
				      .addParticleClient(
						      new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState),
						      entity.getX(),
						      entity.getY(),
						      entity.getZ(),
						      0.0,
						      0.0,
						      0.0
				      );
			}
		}
	}
}
