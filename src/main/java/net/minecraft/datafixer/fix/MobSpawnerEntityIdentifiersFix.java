package net.minecraft.datafixer.fix;

import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Исправляет данные в формате DataFixer.
 */
public class MobSpawnerEntityIdentifiersFix extends DataFix {

	public MobSpawnerEntityIdentifiersFix(Schema schema, boolean bl) {
		super(schema, bl);
	}

	private Dynamic<?> fixSpawner(Dynamic<?> spawnerDynamic) {
		if (!"MobSpawner".equals(spawnerDynamic.get("id").asString(""))) {
			return spawnerDynamic;
		}
		else {
			Optional<String> optional = spawnerDynamic.get("EntityId").asString().result();
			if (optional.isPresent()) {
				Dynamic<?>
						dynamic =
						(Dynamic<?>) DataFixUtils.orElse(
								spawnerDynamic.get("SpawnData").result(),
								spawnerDynamic.emptyMap()
						);
				dynamic = dynamic.set("id", dynamic.createString(optional.get().isEmpty() ? "Pig" : optional.get()));
				spawnerDynamic = spawnerDynamic.set("SpawnData", dynamic);
				spawnerDynamic = spawnerDynamic.remove("EntityId");
			}

			Optional<? extends Stream<? extends Dynamic<?>>>
					optional2 =
					spawnerDynamic.get("SpawnPotentials").asStreamOpt().result();
			if (optional2.isPresent()) {
				spawnerDynamic = spawnerDynamic.set(
						"SpawnPotentials",
						spawnerDynamic.createList(
								optional2.get()
								         .map(
										         (Dynamic<?> spawnPotentialsDynamic) -> {
											         Optional<String>
													         optionalx =
													         spawnPotentialsDynamic.get("Type").asString().result();
											         if (optionalx.isPresent()) {
												         Dynamic<?> dynamic = ((Dynamic) DataFixUtils.orElse(
														         spawnPotentialsDynamic.get("Properties").result(),
														         spawnPotentialsDynamic.emptyMap()
												         )
												         )
														         .set(
																         "id",
																         spawnPotentialsDynamic.createString(optionalx.get())
														         );
												         return spawnPotentialsDynamic
														         .set("Entity", dynamic)
														         .remove("Type")
														         .remove("Properties");
											         }
											         else {
												         return (Dynamic<?>) spawnPotentialsDynamic;
											         }
										         }
								         )
						)
				);
			}

			return spawnerDynamic;
		}
	}

	public TypeRewriteRule makeRule() {
		Type<?> type = getOutputSchema().getType(TypeReferences.UNTAGGED_SPAWNER);
		return fixTypeEverywhereTyped(
				"MobSpawnerEntityIdentifiersFix",
				getInputSchema().getType(TypeReferences.UNTAGGED_SPAWNER),
				type,
				untaggedSpawnerTyped -> {
					Dynamic<?> dynamic = (Dynamic<?>) untaggedSpawnerTyped.get(DSL.remainderFinder());
					dynamic = dynamic.set("id", dynamic.createString("MobSpawner"));
					DataResult<? extends Pair<? extends Typed<?>, ?>>
							dataResult =
							type.readTyped(this.fixSpawner(dynamic));
					return dataResult.result().isEmpty() ? untaggedSpawnerTyped
					                                     : (Typed) ((Pair) dataResult.result().get()).getFirst();
				}
		);
	}
}
