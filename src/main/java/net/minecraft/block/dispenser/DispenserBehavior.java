package net.minecraft.block.dispenser;

import com.mojang.logging.LogUtils;
import net.minecraft.block.*;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.entity.passive.ArmadilloEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potions;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.rule.GameRules;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Функциональный интерфейс поведения диспенсера при выбросе предмета.
 * Метод {@link #registerDefaults()} регистрирует все стандартные поведения
 * для ванильных предметов (снаряды, вёдра, яйца спавна, лодки и т.д.).
 */
public interface DispenserBehavior {

	Logger LOGGER = LogUtils.getLogger();

	/** Урон инструменту «щётка» при чистке армадилло через диспенсер. */
	int BRUSH_SCUTE_DAMAGE = 16;

	/** Количество частиц воды при превращении блока в грязь через диспенсер. */
	int MUD_PARTICLE_COUNT = 5;

	/** Код мирового события «нанесение воска» (wax on). */
	int WAX_ON_WORLD_EVENT = 3003;

	DispenserBehavior NOOP = (pointer, stack) -> stack;

	ItemStack dispense(BlockPointer pointer, ItemStack stack);

	static void registerDefaults() {
		DispenserBlock.registerProjectileBehavior(Items.ARROW);
		DispenserBlock.registerProjectileBehavior(Items.TIPPED_ARROW);
		DispenserBlock.registerProjectileBehavior(Items.SPECTRAL_ARROW);
		DispenserBlock.registerProjectileBehavior(Items.EGG);
		DispenserBlock.registerProjectileBehavior(Items.BLUE_EGG);
		DispenserBlock.registerProjectileBehavior(Items.BROWN_EGG);
		DispenserBlock.registerProjectileBehavior(Items.SNOWBALL);
		DispenserBlock.registerProjectileBehavior(Items.EXPERIENCE_BOTTLE);
		DispenserBlock.registerProjectileBehavior(Items.SPLASH_POTION);
		DispenserBlock.registerProjectileBehavior(Items.LINGERING_POTION);
		DispenserBlock.registerProjectileBehavior(Items.FIREWORK_ROCKET);
		DispenserBlock.registerProjectileBehavior(Items.FIRE_CHARGE);
		DispenserBlock.registerProjectileBehavior(Items.WIND_CHARGE);
		ItemDispenserBehavior spawnEggBehavior = new ItemDispenserBehavior() {
			@Override
			public ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
				Direction direction = pointer.state().get(DispenserBlock.FACING);
				EntityType<?> entityType = ((SpawnEggItem) stack.getItem()).getEntityType(stack);
				if (entityType == null) {
					return stack;
				}

				try {
					entityType.spawnFromItemStack(
							pointer.world(),
							stack,
							null,
							pointer.pos().offset(direction),
							SpawnReason.DISPENSER,
							direction != Direction.UP,
							false
					);
				}
				catch (Exception exception) {
					LOGGER.error("Error while dispensing spawn egg from dispenser at {}", pointer.pos(), exception);
					return ItemStack.EMPTY;
				}

				stack.decrement(1);
				pointer.world().emitGameEvent(null, GameEvent.ENTITY_PLACE, pointer.pos());
				return stack;
			}
		};

		for (SpawnEggItem spawnEggItem : SpawnEggItem.getAll()) {
			DispenserBlock.registerBehavior(spawnEggItem, spawnEggBehavior);
		}

		DispenserBlock.registerBehavior(
				Items.ARMOR_STAND,
				new ItemDispenserBehavior() {
					@Override
					public ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
						Direction direction = pointer.state().get(DispenserBlock.FACING);
						BlockPos blockPos = pointer.pos().offset(direction);
						ServerWorld serverWorld = pointer.world();
						Consumer<ArmorStandEntity> consumer = EntityType.copier(
								armorStand -> armorStand.setYaw(direction.getPositiveHorizontalDegrees()),
								serverWorld,
								stack,
								null
						);
						ArmorStandEntity
								armorStandEntity =
								EntityType.ARMOR_STAND.spawn(
										serverWorld,
										consumer,
										blockPos,
										SpawnReason.DISPENSER,
										false,
										false
								);
						if (armorStandEntity != null) {
							stack.decrement(1);
						}

						return stack;
					}
				}
		);
		DispenserBlock.registerBehavior(
				Items.CHEST,
				new FallibleItemDispenserBehavior() {
					@Override
					public ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
						BlockPos blockPos = pointer.pos().offset(pointer.state().get(DispenserBlock.FACING));

						for (AbstractDonkeyEntity abstractDonkeyEntity : pointer.world()
						                                                        .getEntitiesByClass(
								                                                        AbstractDonkeyEntity.class,
								                                                        new Box(blockPos),
								                                                        donkey -> donkey.isAlive()
										                                                        && !donkey.hasChest()
						                                                        )) {
							if (abstractDonkeyEntity.isTame()) {
								StackReference stackReference = abstractDonkeyEntity.getStackReference(499);
								if (stackReference != null && stackReference.set(stack)) {
									stack.decrement(1);
									this.setSuccess(true);
									return stack;
								}
							}
						}

						return super.dispenseSilently(pointer, stack);
					}
				}
		);
		DispenserBlock.registerBehavior(Items.OAK_BOAT, new BoatDispenserBehavior(EntityType.OAK_BOAT));
		DispenserBlock.registerBehavior(Items.SPRUCE_BOAT, new BoatDispenserBehavior(EntityType.SPRUCE_BOAT));
		DispenserBlock.registerBehavior(Items.BIRCH_BOAT, new BoatDispenserBehavior(EntityType.BIRCH_BOAT));
		DispenserBlock.registerBehavior(Items.JUNGLE_BOAT, new BoatDispenserBehavior(EntityType.JUNGLE_BOAT));
		DispenserBlock.registerBehavior(Items.DARK_OAK_BOAT, new BoatDispenserBehavior(EntityType.DARK_OAK_BOAT));
		DispenserBlock.registerBehavior(Items.ACACIA_BOAT, new BoatDispenserBehavior(EntityType.ACACIA_BOAT));
		DispenserBlock.registerBehavior(Items.CHERRY_BOAT, new BoatDispenserBehavior(EntityType.CHERRY_BOAT));
		DispenserBlock.registerBehavior(Items.MANGROVE_BOAT, new BoatDispenserBehavior(EntityType.MANGROVE_BOAT));
		DispenserBlock.registerBehavior(Items.PALE_OAK_BOAT, new BoatDispenserBehavior(EntityType.PALE_OAK_BOAT));
		DispenserBlock.registerBehavior(Items.BAMBOO_RAFT, new BoatDispenserBehavior(EntityType.BAMBOO_RAFT));
		DispenserBlock.registerBehavior(Items.OAK_CHEST_BOAT, new BoatDispenserBehavior(EntityType.OAK_CHEST_BOAT));
		DispenserBlock.registerBehavior(
				Items.SPRUCE_CHEST_BOAT,
				new BoatDispenserBehavior(EntityType.SPRUCE_CHEST_BOAT)
		);
		DispenserBlock.registerBehavior(Items.BIRCH_CHEST_BOAT, new BoatDispenserBehavior(EntityType.BIRCH_CHEST_BOAT));
		DispenserBlock.registerBehavior(
				Items.JUNGLE_CHEST_BOAT,
				new BoatDispenserBehavior(EntityType.JUNGLE_CHEST_BOAT)
		);
		DispenserBlock.registerBehavior(
				Items.DARK_OAK_CHEST_BOAT,
				new BoatDispenserBehavior(EntityType.DARK_OAK_CHEST_BOAT)
		);
		DispenserBlock.registerBehavior(
				Items.ACACIA_CHEST_BOAT,
				new BoatDispenserBehavior(EntityType.ACACIA_CHEST_BOAT)
		);
		DispenserBlock.registerBehavior(
				Items.CHERRY_CHEST_BOAT,
				new BoatDispenserBehavior(EntityType.CHERRY_CHEST_BOAT)
		);
		DispenserBlock.registerBehavior(
				Items.MANGROVE_CHEST_BOAT,
				new BoatDispenserBehavior(EntityType.MANGROVE_CHEST_BOAT)
		);
		DispenserBlock.registerBehavior(
				Items.PALE_OAK_CHEST_BOAT,
				new BoatDispenserBehavior(EntityType.PALE_OAK_CHEST_BOAT)
		);
		DispenserBlock.registerBehavior(
				Items.BAMBOO_CHEST_RAFT,
				new BoatDispenserBehavior(EntityType.BAMBOO_CHEST_RAFT)
		);
		DispenserBehavior bucketFluidBehavior = new ItemDispenserBehavior() {
			private final ItemDispenserBehavior fallback = new ItemDispenserBehavior();

			@Override
			public ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
				FluidModificationItem fluidItem = (FluidModificationItem) stack.getItem();
				BlockPos targetPos = pointer.pos().offset(pointer.state().get(DispenserBlock.FACING));
				World world = pointer.world();
				if (fluidItem.placeFluid(null, world, targetPos, null)) {
					fluidItem.onEmptied(null, world, stack, targetPos);
					return this.decrementStackWithRemainder(pointer, stack, new ItemStack(Items.BUCKET));
				}

				return this.fallback.dispense(pointer, stack);
			}
		};
		DispenserBlock.registerBehavior(Items.LAVA_BUCKET, bucketFluidBehavior);
		DispenserBlock.registerBehavior(Items.WATER_BUCKET, bucketFluidBehavior);
		DispenserBlock.registerBehavior(Items.POWDER_SNOW_BUCKET, bucketFluidBehavior);
		DispenserBlock.registerBehavior(Items.SALMON_BUCKET, bucketFluidBehavior);
		DispenserBlock.registerBehavior(Items.COD_BUCKET, bucketFluidBehavior);
		DispenserBlock.registerBehavior(Items.PUFFERFISH_BUCKET, bucketFluidBehavior);
		DispenserBlock.registerBehavior(Items.TROPICAL_FISH_BUCKET, bucketFluidBehavior);
		DispenserBlock.registerBehavior(Items.AXOLOTL_BUCKET, bucketFluidBehavior);
		DispenserBlock.registerBehavior(Items.TADPOLE_BUCKET, bucketFluidBehavior);
		DispenserBlock.registerBehavior(
				Items.BUCKET, new ItemDispenserBehavior() {
					@Override
					public ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
						WorldAccess worldAccess = pointer.world();
						BlockPos targetPos = pointer.pos().offset(pointer.state().get(DispenserBlock.FACING));
						BlockState targetState = worldAccess.getBlockState(targetPos);

						if (targetState.getBlock() instanceof FluidDrainable fluidDrainable) {
							ItemStack drained = fluidDrainable.tryDrainFluid(null, worldAccess, targetPos, targetState);
							if (drained.isEmpty()) {
								return super.dispenseSilently(pointer, stack);
							}

							worldAccess.emitGameEvent(null, GameEvent.FLUID_PICKUP, targetPos);
							return this.decrementStackWithRemainder(pointer, stack, new ItemStack(drained.getItem()));
						}

						return super.dispenseSilently(pointer, stack);
					}
				}
		);
		DispenserBlock.registerBehavior(
				Items.FLINT_AND_STEEL, new FallibleItemDispenserBehavior() {
					@Override
					protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
						ServerWorld serverWorld = pointer.world();
						this.setSuccess(true);
						Direction direction = pointer.state().get(DispenserBlock.FACING);
						BlockPos blockPos = pointer.pos().offset(direction);
						BlockState blockState = serverWorld.getBlockState(blockPos);
						if (AbstractFireBlock.canPlaceAt(serverWorld, blockPos, direction)) {
							serverWorld.setBlockState(blockPos, AbstractFireBlock.getState(serverWorld, blockPos));
							serverWorld.emitGameEvent(null, GameEvent.BLOCK_PLACE, blockPos);
						}
						else if (CampfireBlock.canBeLit(blockState) || CandleBlock.canBeLit(blockState)
								|| CandleCakeBlock.canBeLit(blockState)) {
							serverWorld.setBlockState(blockPos, blockState.with(Properties.LIT, true));
							serverWorld.emitGameEvent(null, GameEvent.BLOCK_CHANGE, blockPos);
						}
						else if (blockState.getBlock() instanceof TntBlock) {
							if (TntBlock.primeTnt(serverWorld, blockPos)) {
								serverWorld.removeBlock(blockPos, false);
							}
							else {
								this.setSuccess(false);
							}
						}
						else {
							this.setSuccess(false);
						}

						if (this.isSuccess()) {
							stack.damage(1, serverWorld, null, item -> {});
						}

						return stack;
					}
				}
		);
		DispenserBlock.registerBehavior(
				Items.BONE_MEAL, new FallibleItemDispenserBehavior() {
					@Override
					protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
						this.setSuccess(true);
						World world = pointer.world();
						BlockPos blockPos = pointer.pos().offset(pointer.state().get(DispenserBlock.FACING));
						if (!BoneMealItem.useOnFertilizable(stack, world, blockPos) && !BoneMealItem.useOnGround(
								stack,
								world,
								blockPos,
								null
						)) {
							this.setSuccess(false);
						}
						else if (!world.isClient()) {
							world.syncWorldEvent(1505, blockPos, 15);
						}

						return stack;
					}
				}
		);
		DispenserBlock.registerBehavior(
				Blocks.TNT,
				new FallibleItemDispenserBehavior() {
					@Override
					protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
						ServerWorld serverWorld = pointer.world();
						if (serverWorld.getGameRules().getValue(GameRules.TNT_EXPLODES) == false) {
							this.setSuccess(false);
							return stack;
						}

						BlockPos spawnPos = pointer.pos().offset(pointer.state().get(DispenserBlock.FACING));
						TntEntity tntEntity = new TntEntity(
								serverWorld,
								spawnPos.getX() + 0.5,
								spawnPos.getY(),
								spawnPos.getZ() + 0.5,
								null
						);
						serverWorld.spawnEntity(tntEntity);
						serverWorld.playSound(
								null,
								tntEntity.getX(),
								tntEntity.getY(),
								tntEntity.getZ(),
								SoundEvents.ENTITY_TNT_PRIMED,
								SoundCategory.BLOCKS,
								1.0F,
								1.0F
						);
						serverWorld.emitGameEvent(null, GameEvent.ENTITY_PLACE, spawnPos);
						stack.decrement(1);
						this.setSuccess(true);
						return stack;
					}
				}
		);
		DispenserBlock.registerBehavior(
				Items.WITHER_SKELETON_SKULL,
				new FallibleItemDispenserBehavior() {
					@Override
					protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
						World world = pointer.world();
						Direction direction = pointer.state().get(DispenserBlock.FACING);
						BlockPos targetPos = pointer.pos().offset(direction);

						if (world.isAir(targetPos) && WitherSkullBlock.canDispense(world, targetPos, stack)) {
							world.setBlockState(
									targetPos,
									Blocks.WITHER_SKELETON_SKULL
											.getDefaultState()
											.with(SkullBlock.ROTATION, RotationPropertyHelper.fromDirection(direction)),
									Block.NOTIFY_ALL
							);
							world.emitGameEvent(null, GameEvent.BLOCK_PLACE, targetPos);
							BlockEntity blockEntity = world.getBlockEntity(targetPos);
							if (blockEntity instanceof SkullBlockEntity skullEntity) {
								WitherSkullBlock.onPlaced(world, targetPos, skullEntity);
							}

							stack.decrement(1);
							this.setSuccess(true);
							return stack;
						}

						this.setSuccess(EquippableDispenserBehavior.tryEquip(pointer, stack));
						return stack;
					}
				}
		);
		DispenserBlock.registerBehavior(
				Blocks.CARVED_PUMPKIN, new FallibleItemDispenserBehavior() {
					@Override
					protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
						World world = pointer.world();
						BlockPos targetPos = pointer.pos().offset(pointer.state().get(DispenserBlock.FACING));
						CarvedPumpkinBlock carvedPumpkinBlock = (CarvedPumpkinBlock) Blocks.CARVED_PUMPKIN;

						if (world.isAir(targetPos) && carvedPumpkinBlock.canDispense(world, targetPos)) {
							if (world.isClient() == false) {
								world.setBlockState(targetPos, carvedPumpkinBlock.getDefaultState(), Block.NOTIFY_ALL);
								world.emitGameEvent(null, GameEvent.BLOCK_PLACE, targetPos);
							}

							stack.decrement(1);
							this.setSuccess(true);
							return stack;
						}

						this.setSuccess(EquippableDispenserBehavior.tryEquip(pointer, stack));
						return stack;
					}
				}
		);
		DispenserBlock.registerBehavior(Blocks.SHULKER_BOX.asItem(), new BlockPlacementDispenserBehavior());

		for (DyeColor dyeColor : DyeColor.values()) {
			DispenserBlock.registerBehavior(
					ShulkerBoxBlock.get(dyeColor).asItem(),
					new BlockPlacementDispenserBehavior()
			);
		}

		DispenserBlock.registerBehavior(
				Items.GLASS_BOTTLE.asItem(),
				new FallibleItemDispenserBehavior() {
					private ItemStack pickUpFluid(BlockPointer pointer, ItemStack inputStack, ItemStack outputStack) {
						pointer.world().emitGameEvent(null, GameEvent.FLUID_PICKUP, pointer.pos());
						return this.decrementStackWithRemainder(pointer, inputStack, outputStack);
					}

					@Override
						public ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
							this.setSuccess(false);
							ServerWorld serverWorld = pointer.world();
							BlockPos targetPos = pointer.pos().offset(pointer.state().get(DispenserBlock.FACING));
							BlockState targetState = serverWorld.getBlockState(targetPos);
	
							if (targetState.isIn(
									BlockTags.BEEHIVES,
									state -> state.contains(BeehiveBlock.HONEY_LEVEL)
											&& state.getBlock() instanceof BeehiveBlock
							)
									&& targetState.get(BeehiveBlock.HONEY_LEVEL) >= BeehiveBlock.FULL_HONEY_LEVEL) {
								((BeehiveBlock) targetState.getBlock()).takeHoney(
										serverWorld,
										targetState,
										targetPos,
										null,
										BeehiveBlockEntity.BeeState.BEE_RELEASED
								);
								this.setSuccess(true);
								return this.pickUpFluid(pointer, stack, new ItemStack(Items.HONEY_BOTTLE));
							}
	
							if (serverWorld.getFluidState(targetPos).isIn(FluidTags.WATER)) {
								this.setSuccess(true);
								return this.pickUpFluid(
										pointer,
										stack,
										PotionContentsComponent.createStack(Items.POTION, Potions.WATER)
								);
							}
	
							return super.dispenseSilently(pointer, stack);
						}
				}
		);
		DispenserBlock.registerBehavior(
				Items.GLOWSTONE, new FallibleItemDispenserBehavior() {
					@Override
					public ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
						Direction direction = pointer.state().get(DispenserBlock.FACING);
						BlockPos targetPos = pointer.pos().offset(direction);
						World world = pointer.world();
						BlockState targetState = world.getBlockState(targetPos);
						this.setSuccess(true);

						if (targetState.isOf(Blocks.RESPAWN_ANCHOR) == false) {
							return super.dispenseSilently(pointer, stack);
						}

						if (targetState.get(RespawnAnchorBlock.CHARGES) == RespawnAnchorBlock.MAX_CHARGES) {
							this.setSuccess(false);
							return stack;
						}

						RespawnAnchorBlock.charge(null, world, targetPos, targetState);
						stack.decrement(1);
						return stack;
					}
				}
		);
		DispenserBlock.registerBehavior(Items.SHEARS.asItem(), new ShearsDispenserBehavior());
		DispenserBlock.registerBehavior(
				Items.BRUSH.asItem(), new FallibleItemDispenserBehavior() {
					@Override
					protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
						ServerWorld serverWorld = pointer.world();
						BlockPos targetPos = pointer.pos().offset(pointer.state().get(DispenserBlock.FACING));
						List<ArmadilloEntity> armadillos = serverWorld.getEntitiesByClass(
								ArmadilloEntity.class,
								new Box(targetPos),
								EntityPredicates.EXCEPT_SPECTATOR
						);

						if (armadillos.isEmpty()) {
							this.setSuccess(false);
							return stack;
						}

						for (ArmadilloEntity armadillo : armadillos) {
							if (armadillo.brushScute(null, stack)) {
								stack.damage(BRUSH_SCUTE_DAMAGE, serverWorld, null, item -> {});
								return stack;
							}
						}

						this.setSuccess(false);
						return stack;
					}
				}
		);
		DispenserBlock.registerBehavior(
				Items.HONEYCOMB, new FallibleItemDispenserBehavior() {
					@Override
					public ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
						BlockPos targetPos = pointer.pos().offset(pointer.state().get(DispenserBlock.FACING));
						World world = pointer.world();
						BlockState targetState = world.getBlockState(targetPos);
						Optional<BlockState> waxedState = HoneycombItem.getWaxedState(targetState);

						if (waxedState.isEmpty()) {
							return super.dispenseSilently(pointer, stack);
						}

						world.setBlockState(targetPos, waxedState.get());
						world.syncWorldEvent(WAX_ON_WORLD_EVENT, targetPos, 0);
						stack.decrement(1);
						this.setSuccess(true);
						return stack;
					}
				}
		);
		DispenserBlock.registerBehavior(
				Items.POTION,
				new ItemDispenserBehavior() {
					private final ItemDispenserBehavior fallbackBehavior = new ItemDispenserBehavior();

					@Override
					public ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
						PotionContentsComponent potionContents = stack.getOrDefault(
								DataComponentTypes.POTION_CONTENTS,
								PotionContentsComponent.DEFAULT
						);

						if (potionContents.matches(Potions.WATER) == false) {
							return this.fallbackBehavior.dispense(pointer, stack);
						}

						ServerWorld serverWorld = pointer.world();
						BlockPos dispenserPos = pointer.pos();
						BlockPos targetPos = pointer.pos().offset(pointer.state().get(DispenserBlock.FACING));

						if (serverWorld.getBlockState(targetPos).isIn(BlockTags.CONVERTABLE_TO_MUD) == false) {
							return this.fallbackBehavior.dispense(pointer, stack);
						}

						if (serverWorld.isClient() == false) {
							for (int particleIndex = 0; particleIndex < MUD_PARTICLE_COUNT; particleIndex++) {
								serverWorld.spawnParticles(
										ParticleTypes.SPLASH,
										dispenserPos.getX() + serverWorld.random.nextDouble(),
										dispenserPos.getY() + 1,
										dispenserPos.getZ() + serverWorld.random.nextDouble(),
										1,
										0.0,
										0.0,
										0.0,
										1.0
								);
							}
						}

						serverWorld.playSound(
								null,
								dispenserPos,
								SoundEvents.ITEM_BOTTLE_EMPTY,
								SoundCategory.BLOCKS,
								1.0F,
								1.0F
						);
						serverWorld.emitGameEvent(null, GameEvent.FLUID_PLACE, dispenserPos);
						serverWorld.setBlockState(targetPos, Blocks.MUD.getDefaultState());
						return this.decrementStackWithRemainder(pointer, stack, new ItemStack(Items.GLASS_BOTTLE));
					}
				}
		);
		DispenserBlock.registerBehavior(Items.MINECART, new MinecartDispenserBehavior(EntityType.MINECART));
		DispenserBlock.registerBehavior(Items.CHEST_MINECART, new MinecartDispenserBehavior(EntityType.CHEST_MINECART));
		DispenserBlock.registerBehavior(
				Items.FURNACE_MINECART,
				new MinecartDispenserBehavior(EntityType.FURNACE_MINECART)
		);
		DispenserBlock.registerBehavior(Items.TNT_MINECART, new MinecartDispenserBehavior(EntityType.TNT_MINECART));
		DispenserBlock.registerBehavior(
				Items.HOPPER_MINECART,
				new MinecartDispenserBehavior(EntityType.HOPPER_MINECART)
		);
		DispenserBlock.registerBehavior(
				Items.COMMAND_BLOCK_MINECART,
				new MinecartDispenserBehavior(EntityType.COMMAND_BLOCK_MINECART)
		);
	}
}
