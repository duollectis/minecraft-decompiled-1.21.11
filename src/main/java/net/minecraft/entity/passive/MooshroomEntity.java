package net.minecraft.entity.passive;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SuspiciousStewIngredient;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.SuspiciousStewEffectsComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.conversion.EntityConversionContext;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTables;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.particle.EffectParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.IntFunction;

/**
 * {@code MooshroomEntity}.
 */
public class MooshroomEntity extends AbstractCowEntity implements Shearable {

	private static final TrackedData<Integer>
			VARIANT =
			DataTracker.registerData(MooshroomEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final int MUTATION_CHANCE = 1024;
	private static final String STEW_EFFECTS_NBT_KEY = "stew_effects";
	private @Nullable SuspiciousStewEffectsComponent stewEffects;
	private @Nullable UUID lightningId;

	public MooshroomEntity(EntityType<? extends MooshroomEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	public float getPathfindingFavor(BlockPos pos, WorldView world) {
		return world.getBlockState(pos.down()).isOf(Blocks.MYCELIUM) ? 10.0F : world.getPhototaxisFavor(pos);
	}

	public static boolean canSpawn(
			EntityType<MooshroomEntity> type,
			WorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		return world.getBlockState(pos.down()).isIn(BlockTags.MOOSHROOMS_SPAWNABLE_ON)
				&& isLightLevelValidForNaturalSpawn(world, pos);
	}

	@Override
	public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
		UUID uUID = lightning.getUuid();
		if (!uUID.equals(this.lightningId)) {
			this.setVariant(this.getVariant() == MooshroomEntity.Variant.RED ? MooshroomEntity.Variant.BROWN
			                                                                 : MooshroomEntity.Variant.RED);
			this.lightningId = uUID;
			this.playSound(SoundEvents.ENTITY_MOOSHROOM_CONVERT, 2.0F, 1.0F);
		}
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(VARIANT, MooshroomEntity.Variant.DEFAULT.index);
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack itemStack = player.getStackInHand(hand);
		if (itemStack.isOf(Items.BOWL) && !this.isBaby()) {
			boolean bl = false;
			ItemStack itemStack2;
			if (this.stewEffects != null) {
				bl = true;
				itemStack2 = new ItemStack(Items.SUSPICIOUS_STEW);
				itemStack2.set(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS, this.stewEffects);
				this.stewEffects = null;
			}
			else {
				itemStack2 = new ItemStack(Items.MUSHROOM_STEW);
			}

			ItemStack itemStack3 = ItemUsage.exchangeStack(itemStack, player, itemStack2, false);
			player.setStackInHand(hand, itemStack3);
			SoundEvent soundEvent;
			if (bl) {
				soundEvent = SoundEvents.ENTITY_MOOSHROOM_SUSPICIOUS_MILK;
			}
			else {
				soundEvent = SoundEvents.ENTITY_MOOSHROOM_MILK;
			}

			this.playSound(soundEvent, 1.0F, 1.0F);
			return ActionResult.SUCCESS;
		}
		else if (itemStack.isOf(Items.SHEARS) && this.isShearable()) {
			if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
				this.sheared(serverWorld, SoundCategory.PLAYERS, itemStack);
				this.emitGameEvent(GameEvent.SHEAR, player);
				itemStack.damage(1, player, hand.getEquipmentSlot());
			}

			return ActionResult.SUCCESS;
		}
		else if (this.getVariant() == MooshroomEntity.Variant.BROWN) {
			Optional<SuspiciousStewEffectsComponent> optional = this.getStewEffectFrom(itemStack);
			if (optional.isEmpty()) {
				return super.interactMob(player, hand);
			}
			else {
				if (this.stewEffects != null) {
					for (int i = 0; i < 2; i++) {
						this.getEntityWorld()
						    .addParticleClient(
								    ParticleTypes.SMOKE,
								    this.getX() + this.random.nextDouble() / 2.0,
								    this.getBodyY(0.5),
								    this.getZ() + this.random.nextDouble() / 2.0,
								    0.0,
								    this.random.nextDouble() / 5.0,
								    0.0
						    );
					}
				}
				else {
					itemStack.decrementUnlessCreative(1, player);
					EffectParticleEffect effectParticleEffect = EffectParticleEffect.of(ParticleTypes.EFFECT, -1, 1.0F);

					for (int j = 0; j < 4; j++) {
						this.getEntityWorld()
						    .addParticleClient(
								    effectParticleEffect,
								    this.getX() + this.random.nextDouble() / 2.0,
								    this.getBodyY(0.5),
								    this.getZ() + this.random.nextDouble() / 2.0,
								    0.0,
								    this.random.nextDouble() / 5.0,
								    0.0
						    );
					}

					this.stewEffects = optional.get();
					this.playSound(SoundEvents.ENTITY_MOOSHROOM_EAT, 2.0F, 1.0F);
				}

				return ActionResult.SUCCESS;
			}
		}
		else {
			return super.interactMob(player, hand);
		}
	}

	@Override
	public void sheared(ServerWorld world, SoundCategory shearedSoundCategory, ItemStack shears) {
		world.playSoundFromEntity(null, this, SoundEvents.ENTITY_MOOSHROOM_SHEAR, shearedSoundCategory, 1.0F, 1.0F);
		this.convertTo(
				EntityType.COW, EntityConversionContext.create(this, false, false), cow -> {
					world.spawnParticles(
							ParticleTypes.EXPLOSION,
							this.getX(),
							this.getBodyY(0.5),
							this.getZ(),
							1,
							0.0,
							0.0,
							0.0,
							0.0
					);
					this.forEachShearedItem(
							world, LootTables.MOOSHROOM_SHEARING, shears, (worldx, stack) -> {
								for (int i = 0; i < stack.getCount(); i++) {
									worldx.spawnEntity(new ItemEntity(
											this.getEntityWorld(),
											this.getX(),
											this.getBodyY(1.0),
											this.getZ(),
											stack.copyWithCount(1)
									));
								}
							}
					);
				}
		);
	}

	@Override
	public boolean isShearable() {
		return this.isAlive() && !this.isBaby();
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.put("Type", MooshroomEntity.Variant.CODEC, this.getVariant());
		view.putNullable("stew_effects", SuspiciousStewEffectsComponent.CODEC, this.stewEffects);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		this.setVariant(view
				.<MooshroomEntity.Variant>read("Type", MooshroomEntity.Variant.CODEC)
				.orElse(MooshroomEntity.Variant.DEFAULT));
		this.stewEffects =
				view
						.<SuspiciousStewEffectsComponent>read("stew_effects", SuspiciousStewEffectsComponent.CODEC)
						.orElse(null);
	}

	private Optional<SuspiciousStewEffectsComponent> getStewEffectFrom(ItemStack flower) {
		SuspiciousStewIngredient suspiciousStewIngredient = SuspiciousStewIngredient.of(flower.getItem());
		return suspiciousStewIngredient != null ? Optional.of(suspiciousStewIngredient.getStewEffects())
		                                        : Optional.empty();
	}

	private void setVariant(MooshroomEntity.Variant variant) {
		this.dataTracker.set(VARIANT, variant.index);
	}

	public MooshroomEntity.Variant getVariant() {
		return MooshroomEntity.Variant.fromIndex(this.dataTracker.get(VARIANT));
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		return type == DataComponentTypes.MOOSHROOM_VARIANT ? castComponentValue(
				(ComponentType<T>) type,
				this.getVariant()
		) : super.get(type);
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		this.copyComponentFrom(from, DataComponentTypes.MOOSHROOM_VARIANT);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.MOOSHROOM_VARIANT) {
			this.setVariant(castComponentValue(DataComponentTypes.MOOSHROOM_VARIANT, value));
			return true;
		}
		else {
			return super.setApplicableComponent(type, value);
		}
	}

	public @Nullable MooshroomEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
		MooshroomEntity mooshroomEntity = EntityType.MOOSHROOM.create(serverWorld, SpawnReason.BREEDING);
		if (mooshroomEntity != null) {
			mooshroomEntity.setVariant(this.chooseBabyVariant((MooshroomEntity) passiveEntity));
		}

		return mooshroomEntity;
	}

	private MooshroomEntity.Variant chooseBabyVariant(MooshroomEntity mooshroom) {
		MooshroomEntity.Variant variant = this.getVariant();
		MooshroomEntity.Variant variant2 = mooshroom.getVariant();
		MooshroomEntity.Variant variant3;
		if (variant == variant2 && this.random.nextInt(1024) == 0) {
			variant3 =
					variant == MooshroomEntity.Variant.BROWN ? MooshroomEntity.Variant.RED
					                                         : MooshroomEntity.Variant.BROWN;
		}
		else {
			variant3 = this.random.nextBoolean() ? variant : variant2;
		}

		return variant3;
	}

	/**
	 * {@code Variant}.
	 */
	public static enum Variant implements StringIdentifiable {
		RED("red", 0, Blocks.RED_MUSHROOM.getDefaultState()),
		BROWN("brown", 1, Blocks.BROWN_MUSHROOM.getDefaultState());

		public static final MooshroomEntity.Variant DEFAULT = RED;
		public static final Codec<MooshroomEntity.Variant>
				CODEC =
				StringIdentifiable.createCodec(MooshroomEntity.Variant::values);
		private static final IntFunction<MooshroomEntity.Variant> INDEX_MAPPER = ValueLists.createIndexToValueFunction(
				MooshroomEntity.Variant::getIndex, values(), ValueLists.OutOfBoundsHandling.CLAMP
		);
		public static final PacketCodec<ByteBuf, MooshroomEntity.Variant>
				PACKET_CODEC =
				PacketCodecs.indexed(INDEX_MAPPER, MooshroomEntity.Variant::getIndex);
		private final String name;
		final int index;
		private final BlockState mushroom;

		private Variant(final String name, final int index, final BlockState mushroom) {
			this.name = name;
			this.index = index;
			this.mushroom = mushroom;
		}

		public BlockState getMushroomState() {
			return this.mushroom;
		}

		@Override
		public String asString() {
			return this.name;
		}

		private int getIndex() {
			return this.index;
		}

		static MooshroomEntity.Variant fromIndex(int index) {
			return INDEX_MAPPER.apply(index);
		}
	}
}
