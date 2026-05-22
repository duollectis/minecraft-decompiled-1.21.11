package net.minecraft.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.CampfireCookingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Clearable;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Optional;

/**
 * Блок-сущность костра. Управляет готовкой предметов на 4 слотах,
 * охлаждением при незажжённом состоянии и клиентскими частицами дыма.
 */
public class CampfireBlockEntity extends BlockEntity implements Clearable {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int UNLIT_COOLING_RATE = 2;
	private static final int MAX_COOKING_SLOTS = 4;
	private final DefaultedList<ItemStack> itemsBeingCooked = DefaultedList.ofSize(MAX_COOKING_SLOTS, ItemStack.EMPTY);
	private final int[] cookingTimes = new int[MAX_COOKING_SLOTS];
	private final int[] cookingTotalTimes = new int[MAX_COOKING_SLOTS];

	public CampfireBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.CAMPFIRE, pos, state);
	}

	public static void litServerTick(
			ServerWorld world,
			BlockPos pos,
			BlockState state,
			CampfireBlockEntity blockEntity,
			ServerRecipeManager.MatchGetter<SingleStackRecipeInput, CampfireCookingRecipe> recipeMatchGetter
	) {
		boolean anyCooked = false;

		for (int slot = 0; slot < blockEntity.itemsBeingCooked.size(); slot++) {
			ItemStack cookingStack = blockEntity.itemsBeingCooked.get(slot);

			if (cookingStack.isEmpty()) {
				continue;
			}

			anyCooked = true;
			blockEntity.cookingTimes[slot]++;

			if (blockEntity.cookingTimes[slot] >= blockEntity.cookingTotalTimes[slot]) {
				SingleStackRecipeInput recipeInput = new SingleStackRecipeInput(cookingStack);
				ItemStack result = recipeMatchGetter.getFirstMatch(recipeInput, world)
						.map(recipe -> recipe.value().craft(recipeInput, world.getRegistryManager()))
						.orElse(cookingStack);

				if (result.isItemEnabled(world.getEnabledFeatures())) {
					ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), result);
					blockEntity.itemsBeingCooked.set(slot, ItemStack.EMPTY);
					world.updateListeners(pos, state, state, 3);
					world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(state));
				}
			}
		}

		if (anyCooked) {
			markDirty(world, pos, state);
		}
	}

	public static void unlitServerTick(World world, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
		boolean anyCooling = false;

		for (int slot = 0; slot < campfire.itemsBeingCooked.size(); slot++) {
			if (campfire.cookingTimes[slot] > 0) {
				anyCooling = true;
				campfire.cookingTimes[slot] = MathHelper.clamp(
						campfire.cookingTimes[slot] - UNLIT_COOLING_RATE,
						0,
						campfire.cookingTotalTimes[slot]
				);
			}
		}

		if (anyCooling) {
			markDirty(world, pos, state);
		}
	}

	private static final float SMOKE_PARTICLE_OFFSET = 0.3125F;
	private static final int SMOKE_PARTICLES_PER_SLOT = 4;

	public static void clientTick(World world, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
		Random random = world.random;

		if (random.nextFloat() < 0.11F) {
			for (int count = 0; count < random.nextInt(2) + 2; count++) {
				CampfireBlock.spawnSmokeParticle(world, pos, state.get(CampfireBlock.SIGNAL_FIRE), false);
			}
		}

		int facingTurns = state.get(CampfireBlock.FACING).getHorizontalQuarterTurns();

		for (int slot = 0; slot < campfire.itemsBeingCooked.size(); slot++) {
			if (campfire.itemsBeingCooked.get(slot).isEmpty() || random.nextFloat() >= 0.2F) {
				continue;
			}

			Direction direction = Direction.fromHorizontalQuarterTurns(Math.floorMod(slot + facingTurns, 4));
			double particleX = pos.getX() + 0.5
					- direction.getOffsetX() * SMOKE_PARTICLE_OFFSET
					+ direction.rotateYClockwise().getOffsetX() * SMOKE_PARTICLE_OFFSET;
			double particleY = pos.getY() + 0.5;
			double particleZ = pos.getZ() + 0.5
					- direction.getOffsetZ() * SMOKE_PARTICLE_OFFSET
					+ direction.rotateYClockwise().getOffsetZ() * SMOKE_PARTICLE_OFFSET;

			for (int p = 0; p < SMOKE_PARTICLES_PER_SLOT; p++) {
				world.addParticleClient(ParticleTypes.SMOKE, particleX, particleY, particleZ, 0.0, 5.0E-4, 0.0);
			}
		}
	}

	public DefaultedList<ItemStack> getItemsBeingCooked() {
		return itemsBeingCooked;
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		itemsBeingCooked.clear();
		Inventories.readData(view, itemsBeingCooked);
		view.getOptionalIntArray("CookingTimes")
				.ifPresentOrElse(
						times -> System.arraycopy(times, 0, cookingTimes, 0, Math.min(cookingTimes.length, times.length)),
						() -> Arrays.fill(cookingTimes, 0)
				);
		view.getOptionalIntArray("CookingTotalTimes")
				.ifPresentOrElse(
						times -> System.arraycopy(times, 0, cookingTotalTimes, 0, Math.min(cookingTotalTimes.length, times.length)),
						() -> Arrays.fill(cookingTotalTimes, 0)
				);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		Inventories.writeData(view, itemsBeingCooked, true);
		view.putIntArray("CookingTimes", cookingTimes);
		view.putIntArray("CookingTotalTimes", cookingTotalTimes);
	}

	@Override
	public BlockEntityUpdateS2CPacket toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		NbtCompound result;
		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(getReporterContext(), LOGGER)) {
			NbtWriteView nbtWriteView = NbtWriteView.create(logging, registries);
			Inventories.writeData(nbtWriteView, itemsBeingCooked, true);
			result = nbtWriteView.getNbt();
		}

		return result;
	}

	public boolean addItem(ServerWorld world, @Nullable LivingEntity entity, ItemStack stack) {
		for (int slot = 0; slot < itemsBeingCooked.size(); slot++) {
			if (!itemsBeingCooked.get(slot).isEmpty()) {
				continue;
			}

			Optional<RecipeEntry<CampfireCookingRecipe>> recipe = world.getRecipeManager()
					.getFirstMatch(RecipeType.CAMPFIRE_COOKING, new SingleStackRecipeInput(stack), world);

			if (recipe.isEmpty()) {
				return false;
			}

			cookingTotalTimes[slot] = recipe.get().value().getCookingTime();
			cookingTimes[slot] = 0;
			itemsBeingCooked.set(slot, stack.splitUnlessCreative(1, entity));
			world.emitGameEvent(GameEvent.BLOCK_CHANGE, getPos(), GameEvent.Emitter.of(entity, getCachedState()));
			updateListeners();
			return true;
		}

		return false;
	}

	private void updateListeners() {
		markDirty();
		getWorld().updateListeners(getPos(), getCachedState(), getCachedState(), 3);
	}

	@Override
	public void clear() {
		itemsBeingCooked.clear();
	}

	@Override
	public void onBlockReplaced(BlockPos pos, BlockState oldState) {
		if (world != null) {
			ItemScatterer.spawn(world, pos, getItemsBeingCooked());
		}
	}

	@Override
	protected void readComponents(ComponentsAccess components) {
		super.readComponents(components);
		components.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT)
				.copyTo(getItemsBeingCooked());
	}

	@Override
	protected void addComponents(ComponentMap.Builder builder) {
		super.addComponents(builder);
		builder.add(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(getItemsBeingCooked()));
	}

	@Override
	public void removeFromCopiedStackData(WriteView view) {
		view.remove("Items");
	}
}
