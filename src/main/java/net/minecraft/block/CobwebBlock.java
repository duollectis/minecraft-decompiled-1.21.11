package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Блок паутины — замедляет движение сущностей при столкновении.
 * Существа с эффектом «Плетение» замедляются значительно меньше.
 */
public class CobwebBlock extends Block {

	public static final MapCodec<CobwebBlock> CODEC = createCodec(CobwebBlock::new);

	@Override
	public MapCodec<CobwebBlock> getCodec() {
		return CODEC;
	}

	public CobwebBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected void onEntityCollision(
			BlockState state,
			World world,
			BlockPos pos,
			Entity entity,
			EntityCollisionHandler handler,
			boolean firstCollision
	) {
		Vec3d slowdown = entity instanceof LivingEntity living && living.hasStatusEffect(StatusEffects.WEAVING)
				? new Vec3d(0.5, 0.25, 0.5)
				: new Vec3d(0.25, 0.05F, 0.25);

		entity.slowMovement(state, slowdown);
	}
}
