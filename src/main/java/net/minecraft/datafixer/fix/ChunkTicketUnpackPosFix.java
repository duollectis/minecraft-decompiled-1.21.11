package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.datafixer.TypeReferences;

import java.util.stream.IntStream;

/**
 * Распаковывает позицию чанка из упакованного {@code long} (x в младших 32 битах, z в старших)
 * в массив из двух {@code int} для каждого тикета в сохранённых данных.
 */
public class ChunkTicketUnpackPosFix extends DataFix {

	private static final long COORD_MASK = 0xFFFFFFFFL;
	private static final int COORD_SHIFT = 32;

	public ChunkTicketUnpackPosFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	protected TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
				"ChunkTicketUnpackPosFix",
				getInputSchema().getType(TypeReferences.TICKETS_SAVED_DATA),
				typed -> typed.update(
						DSL.remainderFinder(),
						dynamic -> dynamic.update(
								"data",
								data -> data.update(
										"tickets",
										tickets -> tickets.createList(
												tickets.asStream().map(ticket -> ticket.update(
														"chunk_pos",
														packedPos -> {
															long packed = packedPos.asLong(0L);
															int chunkX = (int) (packed & COORD_MASK);
															int chunkZ = (int) (packed >>> COORD_SHIFT & COORD_MASK);
															return packedPos.createIntList(IntStream.of(chunkX, chunkZ));
														}
												))
										)
								)
						)
				)
		);
	}
}
