package net.minecraft.loot.context;

import com.mojang.serialization.Codec;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.context.ContextParameter;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;
import java.util.function.UnaryOperator;

/** Источник значения сущности/блок-сущности/предмета из контекста лута. */
public interface LootEntityValueSource<R> {

	Codec<LootEntityValueSource<Object>> ENTITY_OR_BLOCK_ENTITY_CODEC = createCodec(
		builder -> builder
			.addEntityReferences(LootContext.EntityReference.values())
			.addEntityReferences(LootContext.BlockEntityReference.values())
	);

	@Nullable R get(LootContext context);

	ContextParameter<?> contextParam();

	static <U> LootEntityValueSource<U> cast(LootEntityValueSource<? extends U> source) {
		return (LootEntityValueSource<U>) source;
	}

	static <R> Codec<LootEntityValueSource<R>> createCodec(UnaryOperator<LootEntityValueSource.Builder<R>> factory) {
		return factory.apply(new LootEntityValueSource.Builder<>()).getCodec();
	}

	/** Строитель маппинга строковых идентификаторов на источники значений. */
	final class Builder<R> {

		private final Codecs.IdMapper<String, LootEntityValueSource<R>> idMapper = new Codecs.IdMapper<>();

		Builder() {
		}

		public <T> LootEntityValueSource.Builder<R> addAll(
			T[] values,
			Function<T, String> idGetter,
			Function<T, ? extends LootEntityValueSource<R>> sourceGetter
		) {
			for (T value : values) {
				idMapper.put(idGetter.apply(value), (LootEntityValueSource<R>) sourceGetter.apply(value));
			}

			return this;
		}

		public <T extends StringIdentifiable> LootEntityValueSource.Builder<R> addEnum(
			T[] values,
			Function<T, ? extends LootEntityValueSource<R>> sourceGetter
		) {
			return addAll(values, StringIdentifiable::asString, sourceGetter);
		}

		public <T extends StringIdentifiable & LootEntityValueSource<? extends R>> LootEntityValueSource.Builder<R> addEntityReferences(
			T[] values
		) {
			return addEnum(
				values,
				value -> LootEntityValueSource.cast((LootEntityValueSource<? extends R>) value)
			);
		}

		public LootEntityValueSource.Builder<R> forEntities(
			Function<? super ContextParameter<? extends Entity>, ? extends LootEntityValueSource<R>> sourceFactory
		) {
			return addEnum(
				LootContext.EntityReference.values(),
				reference -> sourceFactory.apply(reference.contextParam())
			);
		}

		public LootEntityValueSource.Builder<R> forBlockEntities(
			Function<? super ContextParameter<? extends BlockEntity>, ? extends LootEntityValueSource<R>> sourceFactory
		) {
			return addEnum(
				LootContext.BlockEntityReference.values(),
				reference -> sourceFactory.apply(reference.contextParam())
			);
		}

		public LootEntityValueSource.Builder<R> forItemStacks(
			Function<? super ContextParameter<? extends ItemStack>, ? extends LootEntityValueSource<R>> sourceFactory
		) {
			return addEnum(
				LootContext.ItemStackReference.values(),
				reference -> sourceFactory.apply(reference.contextParam())
			);
		}

		Codec<LootEntityValueSource<R>> getCodec() {
			return idMapper.getCodec(Codec.STRING);
		}
	}

	/** Источник, получающий значение напрямую из параметра контекста. */
	interface ContextBased<T> extends LootEntityValueSource<T> {

		@Override
		ContextParameter<? extends T> contextParam();

		@Override
		default @Nullable T get(LootContext context) {
			return context.get((ContextParameter<T>) contextParam());
		}
	}

	/** Источник, получающий значение через компонент параметра контекста. */
	interface ContextComponentBased<T, R> extends LootEntityValueSource<R> {

		@Nullable R get(T contextValue);

		@Override
		ContextParameter<? extends T> contextParam();

		@Override
		default @Nullable R get(LootContext context) {
			T value = context.get((ContextParameter<T>) contextParam());
			return value != null ? get(value) : null;
		}
	}
}
