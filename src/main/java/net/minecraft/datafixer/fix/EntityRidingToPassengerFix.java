package net.minecraft.datafixer.fix;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import net.minecraft.datafixer.TypeReferences;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Конвертирует старую систему верховой езды: поле {@code Riding} (одна сущность-носитель)
 * заменяется на поле {@code Passengers} (список пассажиров на носителе).
 * Логика инвертирует направление связи: вместо «кто на ком едет» — «кто кого везёт».
 */
public class EntityRidingToPassengerFix extends DataFix {

	public EntityRidingToPassengerFix(Schema outputSchema, boolean changesType) {
		super(outputSchema, changesType);
	}

	@Override
	public TypeRewriteRule makeRule() {
		Schema inputSchema = getInputSchema();
		Schema outputSchema = getOutputSchema();
		Type<?> inputEntityTreeType = inputSchema.getTypeRaw(TypeReferences.ENTITY_TREE);
		Type<?> outputEntityTreeType = outputSchema.getTypeRaw(TypeReferences.ENTITY_TREE);
		Type<?> inputEntityType = inputSchema.getTypeRaw(TypeReferences.ENTITY);

		return fixEntityTree(inputSchema, outputSchema, inputEntityTreeType, outputEntityTreeType, inputEntityType);
	}

	private <OldEntityTree, NewEntityTree, Entity> TypeRewriteRule fixEntityTree(
			Schema inputSchema,
			Schema outputSchema,
			Type<OldEntityTree> inputEntityTreeType,
			Type<NewEntityTree> outputEntityTreeType,
			Type<Entity> inputEntityType
	) {
		Type<Pair<String, Pair<Either<OldEntityTree, Unit>, Entity>>> oldTreeType = DSL.named(
				TypeReferences.ENTITY_TREE.typeName(),
				DSL.and(DSL.optional(DSL.field("Riding", inputEntityTreeType)), inputEntityType)
		);
		Type<Pair<String, Pair<Either<List<NewEntityTree>, Unit>, Entity>>> newTreeType = DSL.named(
				TypeReferences.ENTITY_TREE.typeName(),
				DSL.and(DSL.optional(DSL.field("Passengers", DSL.list(outputEntityTreeType))), inputEntityType)
		);

		Type<?> verifyOldType = inputSchema.getType(TypeReferences.ENTITY_TREE);
		Type<?> verifyNewType = outputSchema.getType(TypeReferences.ENTITY_TREE);

		if (!Objects.equals(verifyOldType, oldTreeType)) {
			throw new IllegalStateException("Old entity type is not what was expected.");
		}

		if (!verifyNewType.equals(newTreeType, true, true)) {
			throw new IllegalStateException("New entity type is not what was expected.");
		}

		OpticFinder<Pair<String, Pair<Either<OldEntityTree, Unit>, Entity>>> oldTreeFinder = DSL.typeFinder(oldTreeType);
		OpticFinder<Pair<String, Pair<Either<List<NewEntityTree>, Unit>, Entity>>> newTreeFinder = DSL.typeFinder(newTreeType);
		OpticFinder<NewEntityTree> newEntityTreeFinder = DSL.typeFinder(outputEntityTreeType);

		Type<?> inputPlayerType = inputSchema.getType(TypeReferences.PLAYER);
		Type<?> outputPlayerType = outputSchema.getType(TypeReferences.PLAYER);

		return TypeRewriteRule.seq(
				fixTypeEverywhere(
						"EntityRidingToPassengerFix",
						oldTreeType,
						newTreeType,
						dynamicOps -> pair -> {
							Optional<Pair<String, Pair<Either<List<NewEntityTree>, Unit>, Entity>>> currentResult = Optional.empty();
							Pair<String, Pair<Either<OldEntityTree, Unit>, Entity>> currentPair = pair;

							while (true) {
								Either<List<NewEntityTree>, Unit> passengersEither = (Either<List<NewEntityTree>, Unit>) DataFixUtils.orElse(
										currentResult.map(resultPair -> {
											Typed<NewEntityTree> newEntityTree = (Typed<NewEntityTree>) outputEntityTreeType
													.pointTyped(dynamicOps)
													.orElseThrow(() -> new IllegalStateException("Could not create new entity tree"));

											NewEntityTree newEntityTreeValue = (NewEntityTree) newEntityTree
													.set(newTreeFinder, resultPair)
													.getOptional(newEntityTreeFinder)
													.orElseThrow(() -> new IllegalStateException("Should always have an entity tree here"));

											return Either.left(ImmutableList.of(newEntityTreeValue));
										}),
										Either.right(DSL.unit())
								);

								currentResult = Optional.of(
										Pair.<String, Pair<Either<List<NewEntityTree>, Unit>, Entity>>of(
												TypeReferences.ENTITY_TREE.typeName(),
												Pair.<Either<List<NewEntityTree>, Unit>, Entity>of(
														passengersEither,
														((Pair<?, Pair<?, Entity>>) currentPair.getSecond())
																.getSecond()
																.getSecond()
												)
										)
								);

								Optional<OldEntityTree> ridingEntity = ((Either) ((Pair) currentPair.getSecond()).getFirst()).left();

								if (ridingEntity.isEmpty()) {
									return currentResult.orElseThrow(
											() -> new IllegalStateException("Should always have an entity tree here")
									);
								}

								currentPair = (Pair<String, Pair<Either<OldEntityTree, Unit>, Entity>>) new Typed<>(
										inputEntityTreeType,
										dynamicOps,
										ridingEntity.get()
								)
										.getOptional(oldTreeFinder)
										.orElseThrow(() -> new IllegalStateException("Should always have an entity here"));
							}
						}
				),
				writeAndRead("player RootVehicle injecter", inputPlayerType, outputPlayerType)
		);
	}
}
