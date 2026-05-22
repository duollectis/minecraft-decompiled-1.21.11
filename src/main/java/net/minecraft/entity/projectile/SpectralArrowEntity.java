package net.minecraft.entity.projectile;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.EffectParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Спектральная стрела, накладывающая эффект свечения на цель.
 * <p>
 * При попадании применяет {@link StatusEffects#GLOWING} на {@code duration} тиков.
 * В полёте испускает частицы эффекта для визуальной индикации.
 */
public class SpectralArrowEntity extends PersistentProjectileEntity {

	private static final int DEFAULT_DURATION = 200;

	private int duration = DEFAULT_DURATION;

	public SpectralArrowEntity(EntityType<? extends SpectralArrowEntity> entityType, World world) {
		super(entityType, world);
	}

	public SpectralArrowEntity(World world, LivingEntity owner, ItemStack stack, @Nullable ItemStack shotFrom) {
		super(EntityType.SPECTRAL_ARROW, owner, world, stack, shotFrom);
	}

	public SpectralArrowEntity(
		World world,
		double x,
		double y,
		double z,
		ItemStack stack,
		@Nullable ItemStack shotFrom
	) {
		super(EntityType.SPECTRAL_ARROW, x, y, z, world, stack, shotFrom);
	}

	@Override
	public void tick() {
		super.tick();
		if (getEntityWorld().isClient() && !isInGround()) {
			getEntityWorld().addParticleClient(
				EffectParticleEffect.of(ParticleTypes.EFFECT, -1, 1.0F),
				getX(),
				getY(),
				getZ(),
				0.0,
				0.0,
				0.0
			);
		}
	}

	@Override
	protected void onHit(LivingEntity target) {
		super.onHit(target);
		target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, duration, 0), getEffectCause());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		duration = view.getInt("Duration", DEFAULT_DURATION);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putInt("Duration", duration);
	}

	@Override
	protected ItemStack getDefaultItemStack() {
		return new ItemStack(Items.SPECTRAL_ARROW);
	}
}
