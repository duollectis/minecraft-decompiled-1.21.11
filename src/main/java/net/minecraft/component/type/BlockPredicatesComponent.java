package net.minecraft.component.type;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.predicate.BlockPredicate;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Formatting;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
	 * Компонент предикатов блоков для режима приключений (can_break / can_place_on).
	 * Кэширует результат последней проверки для оптимизации повторных вызовов на одной позиции.
	 */
public class BlockPredicatesComponent {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final Codec<BlockPredicatesComponent>
			CODEC =
			Codecs.listOrSingle(BlockPredicate.CODEC, Codecs.nonEmptyList(BlockPredicate.CODEC.listOf()))
					.xmap(BlockPredicatesComponent::new, checker -> checker.predicates);
	public static final PacketCodec<RegistryByteBuf, BlockPredicatesComponent> PACKET_CODEC = PacketCodec.tuple(
			BlockPredicate.PACKET_CODEC.collect(PacketCodecs.toList()),
			blockPredicatesChecker -> blockPredicatesChecker.predicates,
			BlockPredicatesComponent::new
	);
	public static final Text CAN_BREAK_TEXT = Text.translatable("item.canBreak").formatted(Formatting.GRAY);
	public static final Text CAN_PLACE_TEXT = Text.translatable("item.canPlace").formatted(Formatting.GRAY);
	private static final Text
			CAN_USE_UNKNOWN_TEXT =
			Text.translatable("item.canUse.unknown").formatted(Formatting.GRAY);
	private final List<BlockPredicate> predicates;
	private @Nullable List<Text> tooltipText;
	private @Nullable CachedBlockPosition cachedPos;
	private boolean lastResult;
	private boolean nbtAware;

	public BlockPredicatesComponent(List<BlockPredicate> predicates) {
		this.predicates = predicates;
	}

	private static boolean canUseCache(
			CachedBlockPosition pos,
			@Nullable CachedBlockPosition cachedPos,
			boolean nbtAware
	) {
		if (cachedPos == null || pos.getBlockState() != cachedPos.getBlockState()) {
			return false;
		}

		if (!nbtAware) {
			return true;
		}

		if (pos.getBlockEntity() == null && cachedPos.getBlockEntity() == null) {
			return true;
		}

		if (pos.getBlockEntity() == null || cachedPos.getBlockEntity() == null) {
			return false;
		}

		boolean nbtEqual;
		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(LOGGER)) {
			DynamicRegistryManager registries = pos.getWorld().getRegistryManager();
			NbtCompound posNbt = getNbt(pos.getBlockEntity(), registries, logging);
			NbtCompound cachedNbt = getNbt(cachedPos.getBlockEntity(), registries, logging);
			nbtEqual = Objects.equals(posNbt, cachedNbt);
		}

		return nbtEqual;
	}

	private static NbtCompound getNbt(
			BlockEntity blockEntity,
			DynamicRegistryManager registries,
			ErrorReporter errorReporter
	) {
		NbtWriteView
				nbtWriteView =
				NbtWriteView.create(errorReporter.makeChild(blockEntity.getReporterContext()), registries);
		blockEntity.writeDataWithId(nbtWriteView);
		return nbtWriteView.getNbt();
	}

	/**
		 * Проверяет, удовлетворяет ли позиция хотя бы одному из предикатов.
		 * Результат кэшируется: повторный вызов для той же позиции возвращает кэшированное значение
		 * без повторного обхода предикатов.
		 *
		 * @param pos позиция блока для проверки
		 * @return {@code true} если хотя бы один предикат выполнен
		 */
	public boolean check(CachedBlockPosition pos) {
		if (canUseCache(pos, cachedPos, nbtAware)) {
			return lastResult;
		}

		cachedPos = pos;
		nbtAware = false;

		for (BlockPredicate predicate : predicates) {
			if (predicate.test(pos)) {
				nbtAware |= predicate.hasNbt();
				lastResult = true;
				return true;
			}
		}

		lastResult = false;
		return false;
	}

	private List<Text> getOrCreateTooltipText() {
		if (tooltipText == null) {
			tooltipText = createTooltipText(predicates);
		}

		return tooltipText;
	}

	public void addTooltips(Consumer<Text> adder) {
		getOrCreateTooltipText().forEach(adder);
	}

	private static List<Text> createTooltipText(List<BlockPredicate> blockPredicates) {
		for (BlockPredicate blockPredicate : blockPredicates) {
			if (blockPredicate.blocks().isEmpty()) {
				return List.of(CAN_USE_UNKNOWN_TEXT);
			}
		}

		return blockPredicates.stream()
								.flatMap(predicate -> predicate.blocks().orElseThrow().stream())
								.distinct()
								.map(block -> (Text) block.value().getName().formatted(Formatting.DARK_GRAY))
								.toList();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		return o instanceof BlockPredicatesComponent other && predicates.equals(other.predicates);
	}

	@Override
	public int hashCode() {
		return predicates.hashCode();
	}

	@Override
	public String toString() {
		return "AdventureModePredicate{predicates=" + predicates + "}";
	}
}
