package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Блок слизи — отскакивает сущности при падении и замедляет горизонтальное движение.
 * Живые существа отскакивают с полной скоростью, остальные — с коэффициентом 0.8.
 */
public class SlimeBlock extends TranslucentBlock {

	public static final MapCodec<SlimeBlock> CODEC = createCodec(SlimeBlock::new);

	@Override
	public MapCodec<SlimeBlock> getCodec() {
		return CODEC;
	}

	public SlimeBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
		if (!entity.bypassesLandingEffects()) {
			entity.handleFallDamage(fallDistance, 0.0F, world.getDamageSources().fall());
		}
	}

	@Override
	public void onEntityLand(BlockView world, Entity entity) {
		if (entity.bypassesLandingEffects()) {
			super.onEntityLand(world, entity);
			return;
		}

		bounce(entity);
	}

	private void bounce(Entity entity) {
		Vec3d velocity = entity.getVelocity();

		if (velocity.y < 0.0) {
			double bounceFactor = entity instanceof LivingEntity ? 1.0 : 0.8;
			entity.setVelocity(velocity.x, -velocity.y * bounceFactor, velocity.z);
		}
	}

	@Override
	public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
		double d = Math.abs(entity.getVelocity().y);
		if (d < 0.1 && !entity.bypassesSteppingEffects()) {
			double e = 0.4 + d * 0.2;
			entity.setVelocity(entity.getVelocity().multiply(e, 1.0, e));
		}

		super.onSteppedOn(world, pos, state, entity);
	}
}
