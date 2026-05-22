package net.minecraft.loot.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.text.Texts;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.context.ContextParameter;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Функция лута, устанавливающая имя предмета (пользовательское или встроенное).
 * Поддерживает разрешение текстовых компонентов через источник-сущность.
 */
public class SetNameLootFunction extends ConditionalLootFunction {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<SetNameLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance)
			.and(
				instance.group(
					TextCodecs.CODEC.optionalFieldOf("name").forGetter(function -> function.name),
					LootContext.EntityReference.CODEC
						.optionalFieldOf("entity")
						.forGetter(function -> function.entity),
					SetNameLootFunction.Target.CODEC
						.optionalFieldOf("target", SetNameLootFunction.Target.CUSTOM_NAME)
						.forGetter(function -> function.target)
				)
			)
			.apply(instance, SetNameLootFunction::new)
	);

	private final Optional<Text> name;
	private final Optional<LootContext.EntityReference> entity;
	private final SetNameLootFunction.Target target;

	private SetNameLootFunction(
		List<LootCondition> conditions,
		Optional<Text> name,
		Optional<LootContext.EntityReference> entity,
		SetNameLootFunction.Target target
	) {
		super(conditions);
		this.name = name;
		this.entity = entity;
		this.target = target;
	}

	@Override
	public LootFunctionType<SetNameLootFunction> getType() {
		return LootFunctionTypes.SET_NAME;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return entity.<Set<ContextParameter<?>>>map(ref -> Set.of(ref.contextParam())).orElse(Set.of());
	}

	/**
	 * Создаёт оператор разрешения текстового компонента через команду сущности.
	 * Если сущность недоступна в контексте, возвращает оператор-идентичность.
	 */
	public static UnaryOperator<Text> applySourceEntity(
		LootContext context,
		LootContext.@Nullable EntityReference sourceEntity
	) {
		if (sourceEntity == null) {
			return text -> text;
		}

		Entity entity = context.get(sourceEntity.contextParam());
		if (entity == null) {
			return text -> text;
		}

		ServerCommandSource source = entity
			.getCommandSource(context.getWorld())
			.withPermissions(LeveledPermissionPredicate.GAMEMASTERS);
		return text -> {
			try {
				return Texts.parse(source, text, entity, 0);
			} catch (CommandSyntaxException exception) {
				LOGGER.warn("Failed to resolve text component", exception);
				return text;
			}
		};
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		name.ifPresent(nameText -> stack.set(
			target.getComponentType(),
			applySourceEntity(context, entity.orElse(null)).apply(nameText)
		));
		return stack;
	}

	public static ConditionalLootFunction.Builder<?> builder(Text name, SetNameLootFunction.Target target) {
		return builder(conditions -> new SetNameLootFunction(conditions, Optional.of(name), Optional.empty(), target));
	}

	public static ConditionalLootFunction.Builder<?> builder(
		Text name,
		SetNameLootFunction.Target target,
		LootContext.EntityReference entity
	) {
		return builder(conditions -> new SetNameLootFunction(
			conditions,
			Optional.of(name),
			Optional.of(entity),
			target
		));
	}

	/** Цель установки имени: пользовательское имя или встроенное имя предмета. */
	public enum Target implements StringIdentifiable {
		CUSTOM_NAME("custom_name"),
		ITEM_NAME("item_name");

		public static final Codec<SetNameLootFunction.Target> CODEC =
			StringIdentifiable.createCodec(SetNameLootFunction.Target::values);

		private final String id;

		Target(String id) {
			this.id = id;
		}

		@Override
		public String asString() {
			return id;
		}

		public ComponentType<Text> getComponentType() {
			return switch (this) {
				case CUSTOM_NAME -> DataComponentTypes.CUSTOM_NAME;
				case ITEM_NAME -> DataComponentTypes.ITEM_NAME;
			};
		}
	}
}
