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

/**
 * {@code LootEntityValueSource}.
 */
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

	/**
	 * {@code Builder}.
	 */
	public static final class Builder<R> {

		private final Codecs.IdMapper<String, LootEntityValueSource<R>> ID_MAPPER = new Codecs.IdMapper<>();

		Builder() {
		}

		public <T> LootEntityValueSource.Builder<R> addAll(
				T[] values,
				Function<T, String> idGetter,
				Function<T, ? extends LootEntityValueSource<R>> sourceGetter
		) {
			for (T object : values) {
				this.ID_MAPPER.put(idGetter.apply(object), (LootEntityValueSource<R>) sourceGetter.apply(object));
			}

			return this;
		}

		public <T extends StringIdentifiable> LootEntityValueSource.Builder<R> addEnum(
				T[] values,
				Function<T, ? extends LootEntityValueSource<R>> sourceGetter
		) {
			return this.addAll(values, StringIdentifiable::asString, sourceGetter);
		}

		public <T extends StringIdentifiable & LootEntityValueSource<? extends R>> LootEntityValueSource.Builder<R> addEntityReferences(
				T[] values
		) {
			return this.addEnum(
					values,
					value -> LootEntityValueSource.cast((LootEntityValueSource<? extends R>) value)
			);
		}

		public LootEntityValueSource.Builder<R> forEntities(
				Function<? super ContextParameter<? extends Entity>, ? extends LootEntityValueSource<R>> sourceFactory
		) {
			return this.addEnum(
					LootContext.EntityReference.values(),
					reference -> sourceFactory.apply(reference.contextParam())
			);
		}

		public LootEntityValueSource.Builder<R> forBlockEntities(
				Function<? super ContextParameter<? extends BlockEntity>, ? extends LootEntityValueSource<R>> sourceFactory
		) {
			return this.addEnum(
					LootContext.BlockEntityReference.values(),
					reference -> sourceFactory.apply(reference.contextParam())
			);
		}

		public LootEntityValueSource.Builder<R> forItemStacks(
				Function<? super ContextParameter<? extends ItemStack>, ? extends LootEntityValueSource<R>> sourceFactory
		) {
			return this.addEnum(
					LootContext.ItemStackReference.values(),
					reference -> sourceFactory.apply(reference.contextParam())
			);
		}

		Codec<LootEntityValueSource<R>> getCodec() {
			return this.ID_MAPPER.getCodec(Codec.STRING);
		}
	}

	/**
	 * {@code ContextBased}.
	 */
	public interface ContextBased<T> extends LootEntityValueSource<T> {

		@Override
		ContextParameter<? extends T> contextParam();

		@Override
		default @Nullable T get(LootContext context) {
			return context.get((ContextParameter<T>) this.contextParam());
		}
	}

	/**
	 * {@code ContextComponentBased}.
	 */
	public interface ContextComponentBased<T, R> extends LootEntityValueSource<R> {

		@Nullable R get(T contextValue);

		@Override
		ContextParameter<? extends T> contextParam();

		@Override
		default @Nullable R get(LootContext context) {
			T object = context.get((ContextParameter<T>) this.contextParam());
			return object != null ? this.get(object) : null;
		}
	}
}
