package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.datafixer.TypeReferences;

/**
 * Мигрирует список принудительно загруженных чанков из поля {@code Forced}
 * (массив long-позиций) в поле {@code tickets} — список объектов с типом
 * {@code minecraft:forced}, уровнем {@code 31} и позицией чанка.
 */
public class ForcedChunkToTicketFix extends DataFix {

	private static final int FORCED_TICKET_LEVEL = 31;

	public ForcedChunkToTicketFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
			"ForcedChunkToTicketFix",
			getInputSchema().getType(TypeReferences.TICKETS_SAVED_DATA),
			typed -> typed.update(
				DSL.remainderFinder(),
				dynamic -> dynamic.update(
					"data",
					data -> data.renameAndFixField(
						"Forced",
						"tickets",
						forcedChunks -> forcedChunks.createList(
							forcedChunks.asLongStream().mapToObj(chunkPos ->
								dynamic.emptyMap()
									.set("type", dynamic.createString("minecraft:forced"))
									.set("level", dynamic.createInt(FORCED_TICKET_LEVEL))
									.set("ticks_left", dynamic.createLong(0L))
									.set("chunk_pos", dynamic.createLong(chunkPos))
							)
						)
					)
				)
			)
		);
	}
}
