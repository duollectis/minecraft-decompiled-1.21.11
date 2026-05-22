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
 * Гриб-корова — особый вариант коровы, растущий на мицелии.
 * Поддерживает стрижку (превращается в обычную корову), доение (грибной суп или подозрительное рагу),
 * а также мутацию при ударе молнией (красная ↔ коричневая).
 */
public class MooshroomEntity extends AbstractCowEntity implements Shearable {

	private static final TrackedData<Integer> VARIANT = DataTracker.registerData(
		MooshroomEntity.class,
		TrackedDataHandlerRegistry.INTEGER
	);

	/** Вероятность мутации цвета детёныша: 1 из 1024 при одинаковых родителях. */
	private static final int MUTATION_CHANCE = 1024;
	private static final String STEW_EFFECTS_NBT_KEY = "stew_effects";
	private static final int SMOKE_PARTICLE_COUNT = 2;
	private static final int EFFECT_PARTICLE_COUNT = 4;

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

	/**
	 * При ударе молнией меняет вариант (красная ↔ коричневая).
	 * Один и тот же удар молнии обрабатывается только один раз (защита от дублирования).
	 */
	@Override
	public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
		UUID lightningUuid = lightning.getUuid();
		if (lightningUuid.equals(lightningId)) {
			return;
		}

		setVariant(getVariant() == Variant.RED ? Variant.BROWN : Variant.RED);
		lightningId = lightningUuid;
		playSound(SoundEvents.ENTITY_MOOSHROOM_CONVERT, 2.0F, 1.0F);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(VARIANT, Variant.DEFAULT.index);
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack heldStack = player.getStackInHand(hand);

		if (heldStack.isOf(Items.BOWL) && !isBaby()) {
			return handleBowlInteraction(player, hand, heldStack);
		}

		if (heldStack.isOf(Items.SHEARS) && isShearable()) {
			if (getEntityWorld() instanceof ServerWorld serverWorld) {
				sheared(serverWorld, SoundCategory.PLAYERS, heldStack);
				emitGameEvent(GameEvent.SHEAR, player);
				heldStack.damage(1, player, hand.getEquipmentSlot());
			}

			return ActionResult.SUCCESS;
		}

		if (getVariant() == Variant.BROWN) {
			return handleFlowerInteraction(player, hand, heldStack);
		}

		return super.interactMob(player, hand);
	}

	private ActionResult handleBowlInteraction(PlayerEntity player, Hand hand, ItemStack bowl) {
		boolean hasSuspiciousEffects = stewEffects != null;
		ItemStack stewStack;
		if (hasSuspiciousEffects) {
			stewStack = new ItemStack(Items.SUSPICIOUS_STEW);
			stewStack.set(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS, stewEffects);
			stewEffects = null;
		} else {
			stewStack = new ItemStack(Items.MUSHROOM_STEW);
		}

		player.setStackInHand(hand, ItemUsage.exchangeStack(bowl, player, stewStack, false));
		SoundEvent milkSound = hasSuspiciousEffects
			? SoundEvents.ENTITY_MOOSHROOM_SUSPICIOUS_MILK
			: SoundEvents.ENTITY_MOOSHROOM_MILK;
		playSound(milkSound, 1.0F, 1.0F);
		return ActionResult.SUCCESS;
	}

	private ActionResult handleFlowerInteraction(PlayerEntity player, Hand hand, ItemStack flower) {
		Optional<SuspiciousStewEffectsComponent> effects = getStewEffectFrom(flower);
		if (effects.isEmpty()) {
			return super.interactMob(player, hand);
		}

		if (stewEffects != null) {
			for (int count = 0; count < SMOKE_PARTICLE_COUNT; count++) {
				getEntityWorld().addParticleClient(
					ParticleTypes.SMOKE,
					getX() + random.nextDouble() / 2.0,
					getBodyY(0.5),
					getZ() + random.nextDouble() / 2.0,
					0.0,
					random.nextDouble() / 5.0,
					0.0
				);
			}
		} else {
			flower.decrementUnlessCreative(1, player);
			EffectParticleEffect effectParticle = EffectParticleEffect.of(ParticleTypes.EFFECT, -1, 1.0F);
			for (int count = 0; count < EFFECT_PARTICLE_COUNT; count++) {
				getEntityWorld().addParticleClient(
					effectParticle,
					getX() + random.nextDouble() / 2.0,
					getBodyY(0.5),
					getZ() + random.nextDouble() / 2.0,
					0.0,
					random.nextDouble() / 5.0,
					0.0
				);
			}

			stewEffects = effects.get();
			playSound(SoundEvents.ENTITY_MOOSHROOM_EAT, 2.0F, 1.0F);
		}

		return ActionResult.SUCCESS;
	}

	@Override
	public void sheared(ServerWorld world, SoundCategory shearedSoundCategory, ItemStack shears) {
		world.playSoundFromEntity(null, this, SoundEvents.ENTITY_MOOSHROOM_SHEAR, shearedSoundCategory, 1.0F, 1.0F);
		convertTo(EntityType.COW, EntityConversionContext.create(this, false, false), cow -> {
			world.spawnParticles(
				ParticleTypes.EXPLOSION,
				getX(),
				getBodyY(0.5),
				getZ(),
				1,
				0.0,
				0.0,
				0.0,
				0.0
			);
			forEachShearedItem(world, LootTables.MOOSHROOM_SHEARING, shears, (dropWorld, stack) -> {
				for (int count = 0; count < stack.getCount(); count++) {
					dropWorld.spawnEntity(new ItemEntity(
						getEntityWorld(),
						getX(),
						getBodyY(1.0),
						getZ(),
						stack.copyWithCount(1)
					));
				}
			});
		});
	}

	@Override
	public boolean isShearable() {
		return isAlive() && !isBaby();
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.put("Type", Variant.CODEC, getVariant());
		view.putNullable(STEW_EFFECTS_NBT_KEY, SuspiciousStewEffectsComponent.CODEC, stewEffects);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setVariant(view.<Variant>read("Type", Variant.CODEC).orElse(Variant.DEFAULT));
		stewEffects = view.<SuspiciousStewEffectsComponent>read(STEW_EFFECTS_NBT_KEY, SuspiciousStewEffectsComponent.CODEC)
			.orElse(null);
	}

	private Optional<SuspiciousStewEffectsComponent> getStewEffectFrom(ItemStack flower) {
		SuspiciousStewIngredient ingredient = SuspiciousStewIngredient.of(flower.getItem());
		return ingredient == null ? Optional.empty() : Optional.of(ingredient.getStewEffects());
	}

	private void setVariant(Variant variant) {
		dataTracker.set(VARIANT, variant.index);
	}

	public Variant getVariant() {
		return Variant.fromIndex(dataTracker.get(VARIANT));
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		return type == DataComponentTypes.MOOSHROOM_VARIANT
			? castComponentValue((ComponentType<T>) type, getVariant())
			: super.get(type);
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		copyComponentFrom(from, DataComponentTypes.MOOSHROOM_VARIANT);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.MOOSHROOM_VARIANT) {
			setVariant(castComponentValue(DataComponentTypes.MOOSHROOM_VARIANT, value));
			return true;
		}

		return super.setApplicableComponent(type, value);
	}

	/**
	 * Создаёт детёныша при размножении. С вероятностью 1/1024 при одинаковых родителях
	 * происходит мутация цвета.
	 */
	@Override
	public @Nullable MooshroomEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
		MooshroomEntity baby = EntityType.MOOSHROOM.create(serverWorld, SpawnReason.BREEDING);
		if (baby != null) {
			baby.setVariant(chooseBabyVariant((MooshroomEntity) passiveEntity));
		}

		return baby;
	}

	private Variant chooseBabyVariant(MooshroomEntity otherParent) {
		Variant myVariant = getVariant();
		Variant otherVariant = otherParent.getVariant();
		if (myVariant == otherVariant && random.nextInt(MUTATION_CHANCE) == 0) {
			return myVariant == Variant.BROWN ? Variant.RED : Variant.BROWN;
		}

		return random.nextBoolean() ? myVariant : otherVariant;
	}

	/** Вариант гриб-коровы: красная (обычная) или коричневая (редкая). */
	public enum Variant implements StringIdentifiable {
		RED("red", 0, Blocks.RED_MUSHROOM.getDefaultState()),
		BROWN("brown", 1, Blocks.BROWN_MUSHROOM.getDefaultState());

		public static final Variant DEFAULT = RED;
		public static final Codec<Variant> CODEC = StringIdentifiable.createCodec(Variant::values);
		private static final IntFunction<Variant> INDEX_MAPPER = ValueLists.createIndexToValueFunction(
			Variant::getIndex,
			values(),
			ValueLists.OutOfBoundsHandling.CLAMP
		);
		public static final PacketCodec<ByteBuf, Variant> PACKET_CODEC = PacketCodecs.indexed(
			INDEX_MAPPER,
			Variant::getIndex
		);

		private final String name;
		final int index;
		private final BlockState mushroom;

		Variant(String name, int index, BlockState mushroom) {
			this.name = name;
			this.index = index;
			this.mushroom = mushroom;
		}

		public BlockState getMushroomState() {
			return mushroom;
		}

		@Override
		public String asString() {
			return name;
		}

		private int getIndex() {
			return index;
		}

		static Variant fromIndex(int index) {
			return INDEX_MAPPER.apply(index);
		}
	}
}
