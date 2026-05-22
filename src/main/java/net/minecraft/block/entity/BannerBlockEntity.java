package net.minecraft.block.entity;

import net.minecraft.block.AbstractBannerBlock;
import net.minecraft.block.BannerBlock;
import net.minecraft.block.BlockState;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * Блок-сущность баннера. Хранит базовый цвет и список узоров {@link BannerPatternsComponent}.
 */
public class BannerBlockEntity extends BlockEntity implements Nameable {

	public static final int MAX_PATTERN_COUNT = 6;
	private static final String PATTERNS_KEY = "patterns";
	private static final Text BLOCK_NAME = Text.translatable("block.minecraft.banner");
	private @Nullable Text customName;
	private final DyeColor baseColor;
	private BannerPatternsComponent patterns = BannerPatternsComponent.DEFAULT;

	public BannerBlockEntity(BlockPos pos, BlockState state) {
		this(pos, state, ((AbstractBannerBlock) state.getBlock()).getColor());
	}

	public BannerBlockEntity(BlockPos pos, BlockState state, DyeColor baseColor) {
		super(BlockEntityType.BANNER, pos, state);
		this.baseColor = baseColor;
	}

	@Override
	public Text getName() {
		return customName != null ? customName : BLOCK_NAME;
	}

	@Override
	public @Nullable Text getCustomName() {
		return customName;
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);

		if (!patterns.equals(BannerPatternsComponent.DEFAULT)) {
			view.put("patterns", BannerPatternsComponent.CODEC, patterns);
		}

		view.putNullable("CustomName", TextCodecs.CODEC, customName);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		customName = tryParseCustomName(view, "CustomName");
		patterns = view.<BannerPatternsComponent>read("patterns", BannerPatternsComponent.CODEC)
				.orElse(BannerPatternsComponent.DEFAULT);
	}

	@Override
	public BlockEntityUpdateS2CPacket toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return createNbt(registries);
	}

	public BannerPatternsComponent getPatterns() {
		return patterns;
	}

	public ItemStack getPickStack() {
		ItemStack stack = new ItemStack(BannerBlock.getForColor(baseColor));
		stack.applyComponentsFrom(createComponentMap());
		return stack;
	}

	public DyeColor getColorForState() {
		return baseColor;
	}

	@Override
	protected void readComponents(ComponentsAccess components) {
		super.readComponents(components);
		patterns = components.getOrDefault(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT);
		customName = components.get(DataComponentTypes.CUSTOM_NAME);
	}

	@Override
	protected void addComponents(ComponentMap.Builder builder) {
		super.addComponents(builder);
		builder.add(DataComponentTypes.BANNER_PATTERNS, patterns);
		builder.add(DataComponentTypes.CUSTOM_NAME, customName);
	}

	@Override
	public void removeFromCopiedStackData(WriteView view) {
		view.remove("patterns");
		view.remove("CustomName");
	}
}
