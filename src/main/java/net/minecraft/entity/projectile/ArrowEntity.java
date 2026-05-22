package net.minecraft.entity.projectile;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Стрела с поддержкой зелий-эффектов.
 * <p>
 * Хранит цвет частиц в синхронизированном поле {@code COLOR}.
 * При попадании применяет все эффекты зелья к цели с учётом масштаба длительности.
 * Если стрела застряла в блоке дольше {@value #MAX_POTION_DURATION_TICKS} тиков —
 * эффекты зелья испаряются и стрела превращается в обычную.
 */
public class ArrowEntity extends PersistentProjectileEntity {

	private static final int MAX_POTION_DURATION_TICKS = 600;
	private static final int NO_POTION_COLOR = -1;
	private static final byte STATUS_CLEAR_POTION = 0;
	private static final int PARTICLE_SPAWN_INTERVAL = 5;

	private static final TrackedData<Integer> COLOR =
		DataTracker.registerData(ArrowEntity.class, TrackedDataHandlerRegistry.INTEGER);

	public ArrowEntity(EntityType<? extends ArrowEntity> entityType, World world) {
		super(entityType, world);
	}

	public ArrowEntity(World world, double x, double y, double z, ItemStack stack, @Nullable ItemStack shotFrom) {
		super(EntityType.ARROW, x, y, z, world, stack, shotFrom);
		initColor();
	}

	public ArrowEntity(World world, LivingEntity owner, ItemStack stack, @Nullable ItemStack shotFrom) {
		super(EntityType.ARROW, owner, world, stack, shotFrom);
		initColor();
	}

	private PotionContentsComponent getPotionContents() {
		return getItemStack().getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
	}

	private float getPotionDurationScale() {
		return getItemStack().getOrDefault(DataComponentTypes.POTION_DURATION_SCALE, 1.0F);
	}

	private void setPotionContents(PotionContentsComponent contents) {
		getItemStack().set(DataComponentTypes.POTION_CONTENTS, contents);
		initColor();
	}

	@Override
	protected void setStack(ItemStack stack) {
		super.setStack(stack);
		initColor();
	}

	/**
	 * Обновляет синхронизированный цвет частиц на основе текущего содержимого зелья.
	 * Если зелье отсутствует — устанавливает {@value #NO_POTION_COLOR}.
	 */
	private void initColor() {
		PotionContentsComponent contents = getPotionContents();
		dataTracker.set(
			COLOR,
			contents.equals(PotionContentsComponent.DEFAULT) ? NO_POTION_COLOR : contents.getColor()
		);
	}

	/**
	 * Добавляет эффект зелья к стреле.
	 *
	 * @param effect добавляемый эффект
	 */
	public void addEffect(StatusEffectInstance effect) {
		setPotionContents(getPotionContents().with(effect));
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(COLOR, NO_POTION_COLOR);
	}

	@Override
	public void tick() {
		super.tick();
		if (getEntityWorld().isClient()) {
			if (isInGround()) {
				if (inGroundTime % PARTICLE_SPAWN_INTERVAL == 0) {
					spawnPotionParticles(1);
				}
			} else {
				spawnPotionParticles(2);
			}
		} else if (isInGround()
			&& inGroundTime != 0
			&& !getPotionContents().equals(PotionContentsComponent.DEFAULT)
			&& inGroundTime >= MAX_POTION_DURATION_TICKS
		) {
			getEntityWorld().sendEntityStatus(this, STATUS_CLEAR_POTION);
			setStack(new ItemStack(Items.ARROW));
		}
	}

	private void spawnPotionParticles(int amount) {
		int color = getColor();
		if (color == NO_POTION_COLOR || amount <= 0) {
			return;
		}

		for (int i = 0; i < amount; i++) {
			getEntityWorld().addParticleClient(
				TintedParticleEffect.create(ParticleTypes.ENTITY_EFFECT, color),
				getParticleX(0.5),
				getRandomBodyY(),
				getParticleZ(0.5),
				0.0,
				0.0,
				0.0
			);
		}
	}

	public int getColor() {
		return dataTracker.get(COLOR);
	}

	@Override
	protected void onHit(LivingEntity target) {
		super.onHit(target);
		Entity effectCause = getEffectCause();
		float durationScale = getPotionDurationScale();
		getPotionContents().forEachEffect(effect -> target.addStatusEffect(effect, effectCause), durationScale);
	}

	@Override
	protected ItemStack getDefaultItemStack() {
		return new ItemStack(Items.ARROW);
	}

	@Override
	public void handleStatus(byte status) {
		if (status == STATUS_CLEAR_POTION) {
			int color = getColor();
			if (color == NO_POTION_COLOR) {
				return;
			}

			float red = (color >> 16 & 0xFF) / 255.0F;
			float green = (color >> 8 & 0xFF) / 255.0F;
			float blue = (color & 0xFF) / 255.0F;

			for (int i = 0; i < 20; i++) {
				getEntityWorld().addParticleClient(
					TintedParticleEffect.create(ParticleTypes.ENTITY_EFFECT, red, green, blue),
					getParticleX(0.5),
					getRandomBodyY(),
					getParticleZ(0.5),
					0.0,
					0.0,
					0.0
				);
			}
		} else {
			super.handleStatus(status);
		}
	}
}
