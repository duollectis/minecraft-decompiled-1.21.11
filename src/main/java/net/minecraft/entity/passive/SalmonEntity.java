package net.minecraft.entity.passive;

import io.netty.buffer.ByteBuf;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.function.ValueLists;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.function.IntFunction;

/**
 * Лосось — стайная рыба с тремя размерными вариантами: маленький, нормальный, большой.
 * Вариант влияет на хитбокс и количество дропа.
 */
public class SalmonEntity extends SchoolingFishEntity {

	private static final String TYPE_KEY = "type";
	private static final TrackedData<Integer>
			VARIANT =
			DataTracker.registerData(SalmonEntity.class, TrackedDataHandlerRegistry.INTEGER);

	public SalmonEntity(EntityType<? extends SalmonEntity> entityType, World world) {
		super(entityType, world);
		this.calculateDimensions();
	}

	@Override
	public int getMaxGroupSize() {
		return 5;
	}

	@Override
	public ItemStack getBucketItem() {
		return new ItemStack(Items.SALMON_BUCKET);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_SALMON_AMBIENT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_SALMON_DEATH;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_SALMON_HURT;
	}

	@Override
	protected SoundEvent getFlopSound() {
		return SoundEvents.ENTITY_SALMON_FLOP;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(VARIANT, SalmonEntity.Variant.DEFAULT.getIndex());
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (VARIANT.equals(data)) {
			this.calculateDimensions();
		}
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.put("type", SalmonEntity.Variant.CODEC, this.getVariant());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		this.setVariant(view
				.<SalmonEntity.Variant>read("type", SalmonEntity.Variant.CODEC)
				.orElse(SalmonEntity.Variant.DEFAULT));
	}

	@Override
	public void copyDataToStack(ItemStack stack) {
		Bucketable.copyDataToStack(this, stack);
		stack.copy(DataComponentTypes.SALMON_SIZE, this);
	}

	private void setVariant(SalmonEntity.Variant variant) {
		this.dataTracker.set(VARIANT, variant.index);
	}

	public SalmonEntity.Variant getVariant() {
		return SalmonEntity.Variant.FROM_INDEX.apply(this.dataTracker.get(VARIANT));
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		return type == DataComponentTypes.SALMON_SIZE ? castComponentValue((ComponentType<T>) type, this.getVariant())
		                                              : super.get(type);
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		this.copyComponentFrom(from, DataComponentTypes.SALMON_SIZE);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.SALMON_SIZE) {
			this.setVariant(castComponentValue(DataComponentTypes.SALMON_SIZE, value));
			return true;
		}
		else {
			return super.setApplicableComponent(type, value);
		}
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		Pool.Builder<SalmonEntity.Variant> builder = Pool.builder();
		builder.add(SalmonEntity.Variant.SMALL, 30);
		builder.add(SalmonEntity.Variant.MEDIUM, 50);
		builder.add(SalmonEntity.Variant.LARGE, 15);
		builder.build().getOrEmpty(this.random).ifPresent(this::setVariant);
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	public float getVariantScale() {
		return this.getVariant().scale;
	}

	@Override
	protected EntityDimensions getBaseDimensions(EntityPose pose) {
		return super.getBaseDimensions(pose).scaled(this.getVariantScale());
	}

	/**
 * Размерный вариант лосося: маленький, нормальный, большой.
 */
	public static enum Variant implements StringIdentifiable {
		SMALL("small", 0, 0.5F),
		MEDIUM("medium", 1, 1.0F),
		LARGE("large", 2, 1.5F);

		public static final SalmonEntity.Variant DEFAULT = MEDIUM;
		public static final StringIdentifiable.EnumCodec<SalmonEntity.Variant>
				CODEC =
				StringIdentifiable.createCodec(SalmonEntity.Variant::values);
		static final IntFunction<SalmonEntity.Variant> FROM_INDEX = ValueLists.createIndexToValueFunction(
				SalmonEntity.Variant::getIndex, values(), ValueLists.OutOfBoundsHandling.CLAMP
		);
		public static final PacketCodec<ByteBuf, SalmonEntity.Variant>
				PACKET_CODEC =
				PacketCodecs.indexed(FROM_INDEX, SalmonEntity.Variant::getIndex);
		private final String id;
		final int index;
		final float scale;

		private Variant(final String id, final int index, final float scale) {
			this.id = id;
			this.index = index;
			this.scale = scale;
		}

		@Override
		public String asString() {
			return this.id;
		}

		int getIndex() {
			return this.index;
		}
	}
}
