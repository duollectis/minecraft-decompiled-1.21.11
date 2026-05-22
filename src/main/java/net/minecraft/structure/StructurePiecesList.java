package net.minecraft.structure;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Иммутабельный список кусков структуры с поддержкой сериализации и десериализации.
 * Хранит маппинг устаревших идентификаторов кусков на актуальный {@code jigsaw}.
 */
public record StructurePiecesList(List<StructurePiece> pieces) {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Identifier JIGSAW = Identifier.ofVanilla("jigsaw");

	/** Маппинг устаревших идентификаторов кусков на актуальный jigsaw. */
	private static final Map<Identifier, Identifier> ID_UPDATES = ImmutableMap.<Identifier, Identifier>builder()
		.put(Identifier.ofVanilla("nvi"), JIGSAW)
		.put(Identifier.ofVanilla("pcp"), JIGSAW)
		.put(Identifier.ofVanilla("bastionremnant"), JIGSAW)
		.put(Identifier.ofVanilla("runtime"), JIGSAW)
		.build();

	public StructurePiecesList(final List<StructurePiece> pieces) {
		this.pieces = List.copyOf(pieces);
	}

	public boolean isEmpty() {
		return pieces.isEmpty();
	}

	public boolean contains(BlockPos pos) {
		for (StructurePiece piece : pieces) {
			if (piece.getBoundingBox().contains(pos)) {
				return true;
			}
		}

		return false;
	}

	public NbtElement toNbt(StructureContext context) {
		NbtList nbtList = new NbtList();
		for (StructurePiece piece : pieces) {
			nbtList.add(piece.toNbt(context));
		}

		return nbtList;
	}

	/**
	 * Десериализует список кусков из NBT, применяя маппинг устаревших идентификаторов.
	 * Куски с неизвестными идентификаторами пропускаются с логированием ошибки.
	 */
	public static StructurePiecesList fromNbt(NbtList list, StructureContext context) {
		List<StructurePiece> pieces = Lists.newArrayList();

		for (int index = 0; index < list.size(); index++) {
			NbtCompound nbt = list.getCompoundOrEmpty(index);
			String rawId = nbt.getString("id", "").toLowerCase(Locale.ROOT);
			Identifier identifier = Identifier.of(rawId);
			Identifier resolvedId = ID_UPDATES.getOrDefault(identifier, identifier);
			StructurePieceType pieceType = Registries.STRUCTURE_PIECE.get(resolvedId);
			if (pieceType == null) {
				LOGGER.error("Unknown structure piece id: {}", resolvedId);
				continue;
			}

			try {
				pieces.add(pieceType.load(context, nbt));
			} catch (Exception exception) {
				LOGGER.error("Exception loading structure piece with id {}", resolvedId, exception);
			}
		}

		return new StructurePiecesList(pieces);
	}

	public BlockBox getBoundingBox() {
		return StructurePiece.boundingBox(pieces.stream());
	}
}
