package net.minecraft.loot.function;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.provider.nbt.ContextLootNbtProvider;
import net.minecraft.loot.provider.nbt.LootNbtProvider;
import net.minecraft.loot.provider.nbt.LootNbtProviderTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.context.ContextParameter;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class CopyNbtLootFunction extends ConditionalLootFunction {

	public static final MapCodec<CopyNbtLootFunction> CODEC = RecordCodecBuilder.mapCodec(
			instance -> addConditionsField(instance)
					.and(
							instance.group(
									LootNbtProviderTypes.CODEC.fieldOf("source").forGetter(function -> function.source),
									CopyNbtLootFunction.Operation.CODEC
											.listOf()
											.fieldOf("ops")
											.forGetter(function -> function.operations)
							)
					)
					.apply(instance, CopyNbtLootFunction::new)
	);

	private final LootNbtProvider source;
	private final List<CopyNbtLootFunction.Operation> operations;

	CopyNbtLootFunction(
			List<LootCondition> conditions,
			LootNbtProvider source,
			List<CopyNbtLootFunction.Operation> operations
	) {
		super(conditions);
		this.source = source;
		this.operations = List.copyOf(operations);
	}

	@Override
	public LootFunctionType<CopyNbtLootFunction> getType() {
		return LootFunctionTypes.COPY_CUSTOM_DATA;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return source.getRequiredParameters();
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		NbtElement sourceNbt = source.getNbt(context);

		if (sourceNbt == null) {
			return stack;
		}

		MutableObject<NbtCompound> targetNbt = new MutableObject<>();
		Supplier<NbtElement> lazyTargetNbt = () -> {
			if (targetNbt.getValue() == null) {
				targetNbt.setValue(
						stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt()
				);
			}

			return targetNbt.getValue();
		};

		operations.forEach(operation -> operation.execute(lazyTargetNbt, sourceNbt));

		NbtCompound result = targetNbt.getValue();

		if (result != null) {
			NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, result);
		}

		return stack;
	}

	@Deprecated
	public static CopyNbtLootFunction.Builder builder(LootNbtProvider source) {
		return new CopyNbtLootFunction.Builder(source);
	}

	public static CopyNbtLootFunction.Builder builder(LootContext.EntityReference target) {
		return new CopyNbtLootFunction.Builder(ContextLootNbtProvider.fromTarget(target));
	}

	public static class Builder extends ConditionalLootFunction.Builder<CopyNbtLootFunction.Builder> {

		private final LootNbtProvider source;
		private final List<CopyNbtLootFunction.Operation> operations = Lists.newArrayList();

		Builder(LootNbtProvider source) {
			this.source = source;
		}

		public CopyNbtLootFunction.Builder withOperation(
				String sourcePath,
				String targetPath,
				CopyNbtLootFunction.Operator operator
		) {
			try {
				operations.add(new CopyNbtLootFunction.Operation(
						NbtPathArgumentType.NbtPath.parse(sourcePath),
						NbtPathArgumentType.NbtPath.parse(targetPath),
						operator
				));

				return this;
			} catch (CommandSyntaxException exception) {
				throw new IllegalArgumentException(exception);
			}
		}

		public CopyNbtLootFunction.Builder withOperation(String sourcePath, String targetPath) {
			return withOperation(sourcePath, targetPath, CopyNbtLootFunction.Operator.REPLACE);
		}

		protected CopyNbtLootFunction.Builder getThisBuilder() {
			return this;
		}

		@Override
		public LootFunction build() {
			return new CopyNbtLootFunction(getConditions(), source, operations);
		}
	}

	record Operation(
			NbtPathArgumentType.NbtPath parsedSourcePath,
			NbtPathArgumentType.NbtPath parsedTargetPath,
			CopyNbtLootFunction.Operator operator
	) {

		public static final Codec<CopyNbtLootFunction.Operation> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						NbtPathArgumentType.NbtPath.CODEC
								.fieldOf("source")
								.forGetter(CopyNbtLootFunction.Operation::parsedSourcePath),
						NbtPathArgumentType.NbtPath.CODEC
								.fieldOf("target")
								.forGetter(CopyNbtLootFunction.Operation::parsedTargetPath),
						CopyNbtLootFunction.Operator.CODEC
								.fieldOf("op")
								.forGetter(CopyNbtLootFunction.Operation::operator)
				).apply(instance, CopyNbtLootFunction.Operation::new)
		);

		public void execute(Supplier<NbtElement> itemNbtGetter, NbtElement sourceEntityNbt) {
			try {
				List<NbtElement> sourceElements = parsedSourcePath.get(sourceEntityNbt);

				if (!sourceElements.isEmpty()) {
					operator.merge(itemNbtGetter.get(), parsedTargetPath, sourceElements);
				}
			} catch (CommandSyntaxException ignored) {
				// Ошибки пути NBT при копировании намеренно игнорируются — невалидный путь не должен ломать лут
			}
		}
	}

	public enum Operator implements StringIdentifiable {
		REPLACE("replace") {
			@Override
			public void merge(
					NbtElement itemNbt,
					NbtPathArgumentType.NbtPath targetPath,
					List<NbtElement> sourceNbts
			) throws CommandSyntaxException {
				targetPath.put(itemNbt, Iterables.getLast(sourceNbts));
			}
		},
		APPEND("append") {
			@Override
			public void merge(
					NbtElement itemNbt,
					NbtPathArgumentType.NbtPath targetPath,
					List<NbtElement> sourceNbts
			) throws CommandSyntaxException {
				List<NbtElement> targets = targetPath.getOrInit(itemNbt, NbtList::new);
				targets.forEach(foundNbt -> {
					if (foundNbt instanceof NbtList nbtList) {
						sourceNbts.forEach(sourceNbt -> nbtList.add(sourceNbt.copy()));
					}
				});
			}
		},
		MERGE("merge") {
			@Override
			public void merge(
					NbtElement itemNbt,
					NbtPathArgumentType.NbtPath targetPath,
					List<NbtElement> sourceNbts
			) throws CommandSyntaxException {
				List<NbtElement> targets = targetPath.getOrInit(itemNbt, NbtCompound::new);
				targets.forEach(foundNbt -> {
					if (foundNbt instanceof NbtCompound targetCompound) {
						sourceNbts.forEach(sourceNbt -> {
							if (sourceNbt instanceof NbtCompound sourceCompound) {
								targetCompound.copyFrom(sourceCompound);
							}
						});
					}
				});
			}
		};

		public static final Codec<CopyNbtLootFunction.Operator> CODEC =
				StringIdentifiable.createCodec(CopyNbtLootFunction.Operator::values);

		private final String name;

		public abstract void merge(
				NbtElement itemNbt,
				NbtPathArgumentType.NbtPath targetPath,
				List<NbtElement> sourceNbts
		) throws CommandSyntaxException;

		Operator(final String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return name;
		}
	}
}
