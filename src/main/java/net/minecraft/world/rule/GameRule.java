package net.minecraft.world.rule;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.registry.Registries;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.resource.featuretoggle.ToggleableFeature;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * Правило игры — типизированная настройка сервера, доступная через команду {@code /gamerule}.
 * Каждое правило имеет категорию, тип данных, значение по умолчанию и набор требуемых фич.
 *
 * @param <T> тип значения правила ({@link Boolean} или {@link Integer})
 */
public final class GameRule<T> implements ToggleableFeature {

	private final GameRuleCategory category;
	private final GameRuleType type;
	private final ArgumentType<T> argumentType;
	private final GameRules.Acceptor<T> acceptor;
	private final Codec<T> codec;
	private final ToIntFunction<T> commandResultSupplier;
	private final T defaultValue;
	private final FeatureSet requiredFeatures;

	public GameRule(
		GameRuleCategory category,
		GameRuleType type,
		ArgumentType<T> argumentType,
		GameRules.Acceptor<T> acceptor,
		Codec<T> codec,
		ToIntFunction<T> commandResultSupplier,
		T defaultValue,
		FeatureSet requiredFeatures
	) {
		this.category = category;
		this.type = type;
		this.argumentType = argumentType;
		this.acceptor = acceptor;
		this.codec = codec;
		this.commandResultSupplier = commandResultSupplier;
		this.defaultValue = defaultValue;
		this.requiredFeatures = requiredFeatures;
	}

	@Override
	public String toString() {
		return toShortString();
	}

	public String toShortString() {
		return getId().toShortString();
	}

	public Identifier getId() {
		return Objects.requireNonNull(Registries.GAME_RULE.getId(this));
	}

	public String getTranslationKey() {
		return Util.createTranslationKey("gamerule", getId());
	}

	public String getValueName(T value) {
		return value.toString();
	}

	/**
	 * Десериализует строковое значение правила через {@link ArgumentType}.
	 * Возвращает ошибку, если строка содержит лишние символы после разбора.
	 *
	 * @param value строковое представление значения
	 * @return результат десериализации или описание ошибки
	 */
	public DataResult<T> deserialize(String value) {
		try {
			StringReader reader = new StringReader(value);
			T parsed = (T) argumentType.parse(reader);
			return reader.canRead()
				? DataResult.error(() -> "Failed to deserialize; trailing characters", parsed)
				: DataResult.success(parsed);
		} catch (CommandSyntaxException e) {
			return DataResult.error(() -> "Failed to deserialize");
		}
	}

	public Class<T> getValueClass() {
		return (Class<T>) defaultValue.getClass();
	}

	public void accept(GameRuleVisitor visitor) {
		acceptor.call(visitor, this);
	}

	public int getCommandResult(T value) {
		return commandResultSupplier.applyAsInt(value);
	}

	public GameRuleCategory getCategory() {
		return category;
	}

	public GameRuleType getType() {
		return type;
	}

	public ArgumentType<T> getArgumentType() {
		return argumentType;
	}

	public Codec<T> getCodec() {
		return codec;
	}

	public T getDefaultValue() {
		return defaultValue;
	}

	@Override
	public FeatureSet getRequiredFeatures() {
		return requiredFeatures;
	}
}
