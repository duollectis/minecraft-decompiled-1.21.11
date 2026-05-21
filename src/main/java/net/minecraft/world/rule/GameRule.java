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
 * {@code GameRule}.
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
		return this.toShortString();
	}

	/**
	 * To short string.
	 *
	 * @return String — результат операции
	 */
	public String toShortString() {
		return this.getId().toShortString();
	}

	public Identifier getId() {
		return Objects.requireNonNull(Registries.GAME_RULE.getId(this));
	}

	public String getTranslationKey() {
		return Util.createTranslationKey("gamerule", this.getId());
	}

	public String getValueName(T value) {
		return value.toString();
	}

	/**
	 * Deserialize.
	 *
	 * @param value value
	 *
	 * @return DataResult — результат операции
	 */
	public DataResult<T> deserialize(String value) {
		try {
			StringReader stringReader = new StringReader(value);
			T object = (T) this.argumentType.parse(stringReader);
			return stringReader.canRead() ? DataResult.error(() -> "Failed to deserialize; trailing characters", object)
			                              : DataResult.success(object);
		}
		catch (CommandSyntaxException var4) {
			return DataResult.error(() -> "Failed to deserialize");
		}
	}

	public Class<T> getValueClass() {
		return (Class<T>) this.defaultValue.getClass();
	}

	/**
	 * Accept.
	 *
	 * @param visitor visitor
	 */
	public void accept(GameRuleVisitor visitor) {
		this.acceptor.call(visitor, this);
	}

	public int getCommandResult(T value) {
		return this.commandResultSupplier.applyAsInt(value);
	}

	public GameRuleCategory getCategory() {
		return this.category;
	}

	public GameRuleType getType() {
		return this.type;
	}

	public ArgumentType<T> getArgumentType() {
		return this.argumentType;
	}

	public Codec<T> getCodec() {
		return this.codec;
	}

	public T getDefaultValue() {
		return this.defaultValue;
	}

	@Override
	public FeatureSet getRequiredFeatures() {
		return this.requiredFeatures;
	}
}
