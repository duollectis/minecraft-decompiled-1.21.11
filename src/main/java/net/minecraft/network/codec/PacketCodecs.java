package net.minecraft.network.codec;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.encoding.StringEncoding;
import net.minecraft.network.encoding.VarInts;
import net.minecraft.network.encoding.VarLongs;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Uuids;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

import java.util.*;
import java.util.function.*;

/**
 * Набор стандартных {@link PacketCodec}-констант и фабричных методов для примитивных типов,
 * коллекций, NBT, реестровых значений и JSON.
 *
 * <p>Все константы являются полями интерфейса (неявно {@code public static final}).
 * Фабричные методы ({@link #collection}, {@link #map}, {@link #optional}, {@link #indexed} и др.)
 * позволяют строить составные кодеки без написания анонимных классов вручную.</p>
 */
public interface PacketCodecs {

	int MAX_COLLECTION_SIZE = 65536;

	PacketCodec<ByteBuf, Boolean> BOOLEAN = new PacketCodec<ByteBuf, Boolean>() {
		@Override
		public Boolean decode(ByteBuf buf) {
			return buf.readBoolean();
		}

		@Override
		public void encode(ByteBuf buf, Boolean value) {
			buf.writeBoolean(value);
		}
	};

	PacketCodec<ByteBuf, Byte> BYTE = new PacketCodec<ByteBuf, Byte>() {
		@Override
		public Byte decode(ByteBuf buf) {
			return buf.readByte();
		}

		@Override
		public void encode(ByteBuf buf, Byte value) {
			buf.writeByte(value);
		}
	};

	PacketCodec<ByteBuf, Float> DEGREES = BYTE.xmap(MathHelper::unpackDegrees, MathHelper::packDegrees);

	PacketCodec<ByteBuf, Short> SHORT = new PacketCodec<ByteBuf, Short>() {
		@Override
		public Short decode(ByteBuf buf) {
			return buf.readShort();
		}

		@Override
		public void encode(ByteBuf buf, Short value) {
			buf.writeShort(value);
		}
	};

	PacketCodec<ByteBuf, Integer> UNSIGNED_SHORT = new PacketCodec<ByteBuf, Integer>() {
		@Override
		public Integer decode(ByteBuf buf) {
			return buf.readUnsignedShort();
		}

		@Override
		public void encode(ByteBuf buf, Integer value) {
			buf.writeShort(value);
		}
	};

	PacketCodec<ByteBuf, Integer> INTEGER = new PacketCodec<ByteBuf, Integer>() {
		@Override
		public Integer decode(ByteBuf buf) {
			return buf.readInt();
		}

		@Override
		public void encode(ByteBuf buf, Integer value) {
			buf.writeInt(value);
		}
	};

	PacketCodec<ByteBuf, Integer> VAR_INT = new PacketCodec<ByteBuf, Integer>() {
		@Override
		public Integer decode(ByteBuf buf) {
			return VarInts.read(buf);
		}

		@Override
		public void encode(ByteBuf buf, Integer value) {
			VarInts.write(buf, value);
		}
	};

	PacketCodec<ByteBuf, OptionalInt> OPTIONAL_INT = VAR_INT.xmap(
			value -> value == 0 ? OptionalInt.empty() : OptionalInt.of(value - 1),
			value -> value.isPresent() ? value.getAsInt() + 1 : 0
	);

	PacketCodec<ByteBuf, Long> LONG = new PacketCodec<ByteBuf, Long>() {
		@Override
		public Long decode(ByteBuf buf) {
			return buf.readLong();
		}

		@Override
		public void encode(ByteBuf buf, Long value) {
			buf.writeLong(value);
		}
	};

	PacketCodec<ByteBuf, Long> VAR_LONG = new PacketCodec<ByteBuf, Long>() {
		@Override
		public Long decode(ByteBuf buf) {
			return VarLongs.read(buf);
		}

		@Override
		public void encode(ByteBuf buf, Long value) {
			VarLongs.write(buf, value);
		}
	};

	PacketCodec<ByteBuf, Float> FLOAT = new PacketCodec<ByteBuf, Float>() {
		@Override
		public Float decode(ByteBuf buf) {
			return buf.readFloat();
		}

		@Override
		public void encode(ByteBuf buf, Float value) {
			buf.writeFloat(value);
		}
	};

	PacketCodec<ByteBuf, Double> DOUBLE = new PacketCodec<ByteBuf, Double>() {
		@Override
		public Double decode(ByteBuf buf) {
			return buf.readDouble();
		}

		@Override
		public void encode(ByteBuf buf, Double value) {
			buf.writeDouble(value);
		}
	};

	PacketCodec<ByteBuf, byte[]> BYTE_ARRAY = new PacketCodec<ByteBuf, byte[]>() {
		@Override
		public byte[] decode(ByteBuf buf) {
			return PacketByteBuf.readByteArray(buf);
		}

		@Override
		public void encode(ByteBuf buf, byte[] value) {
			PacketByteBuf.writeByteArray(buf, value);
		}
	};

	PacketCodec<ByteBuf, long[]> LONG_ARRAY = new PacketCodec<ByteBuf, long[]>() {
		@Override
		public long[] decode(ByteBuf buf) {
			return PacketByteBuf.readLongArray(buf);
		}

		@Override
		public void encode(ByteBuf buf, long[] values) {
			PacketByteBuf.writeLongArray(buf, values);
		}
	};

	PacketCodec<ByteBuf, String> STRING = string(PacketByteBuf.DEFAULT_MAX_STRING_LENGTH);

	PacketCodec<ByteBuf, NbtElement> NBT_ELEMENT = nbt(NbtSizeTracker::forPacket);

	PacketCodec<ByteBuf, NbtElement> UNLIMITED_NBT_ELEMENT = nbt(NbtSizeTracker::ofUnlimitedBytes);

	PacketCodec<ByteBuf, NbtCompound> NBT_COMPOUND = nbtCompound(NbtSizeTracker::forPacket);

	PacketCodec<ByteBuf, NbtCompound> UNLIMITED_NBT_COMPOUND = nbtCompound(NbtSizeTracker::ofUnlimitedBytes);

	PacketCodec<ByteBuf, Optional<NbtCompound>> OPTIONAL_NBT = new PacketCodec<ByteBuf, Optional<NbtCompound>>() {
		@Override
		public Optional<NbtCompound> decode(ByteBuf buf) {
			return Optional.ofNullable(PacketByteBuf.readNbt(buf));
		}

		@Override
		public void encode(ByteBuf buf, Optional<NbtCompound> value) {
			PacketByteBuf.writeNbt(buf, value.orElse(null));
		}
	};

	PacketCodec<ByteBuf, Vector3fc> VECTOR_3F = new PacketCodec<ByteBuf, Vector3fc>() {
		@Override
		public Vector3fc decode(ByteBuf buf) {
			return PacketByteBuf.readVector3f(buf);
		}

		@Override
		public void encode(ByteBuf buf, Vector3fc value) {
			PacketByteBuf.writeVector3f(buf, value);
		}
	};

	PacketCodec<ByteBuf, Quaternionfc> QUATERNION_F = new PacketCodec<ByteBuf, Quaternionfc>() {
		@Override
		public Quaternionfc decode(ByteBuf buf) {
			return PacketByteBuf.readQuaternionf(buf);
		}

		@Override
		public void encode(ByteBuf buf, Quaternionfc value) {
			PacketByteBuf.writeQuaternionf(buf, value);
		}
	};

	PacketCodec<ByteBuf, Integer> SYNC_ID = new PacketCodec<ByteBuf, Integer>() {
		@Override
		public Integer decode(ByteBuf buf) {
			return PacketByteBuf.readSyncId(buf);
		}

		@Override
		public void encode(ByteBuf buf, Integer value) {
			PacketByteBuf.writeSyncId(buf, value);
		}
	};

	/** Максимальное количество свойств профиля игрока (ограничение Mojang Auth). */
	PacketCodec<ByteBuf, PropertyMap> PROPERTY_MAP = new PacketCodec<ByteBuf, PropertyMap>() {
		private static final int MAX_PROPERTIES = 16;
		private static final int MAX_NAME_LENGTH = 64;
		private static final int MAX_VALUE_LENGTH = 32767;
		private static final int MAX_SIGNATURE_LENGTH = 1024;

		@Override
		public PropertyMap decode(ByteBuf buf) {
			int count = PacketCodecs.readCollectionSize(buf, MAX_PROPERTIES);
			Builder<String, Property> builder = ImmutableMultimap.builder();

			for (int index = 0; index < count; index++) {
				String name = StringEncoding.decode(buf, MAX_NAME_LENGTH);
				String value = StringEncoding.decode(buf, MAX_VALUE_LENGTH);
				String signature = PacketByteBuf.readNullable(buf, b -> StringEncoding.decode(b, MAX_SIGNATURE_LENGTH));
				Property property = new Property(name, value, signature);
				builder.put(property.name(), property);
			}

			return new PropertyMap(builder.build());
		}

		@Override
		public void encode(ByteBuf buf, PropertyMap propertyMap) {
			PacketCodecs.writeCollectionSize(buf, propertyMap.size(), Uuids.BYTE_ARRAY_SIZE);

			for (Property property : propertyMap.values()) {
				StringEncoding.encode(buf, property.name(), MAX_NAME_LENGTH);
				StringEncoding.encode(buf, property.value(), MAX_VALUE_LENGTH);
				PacketByteBuf.writeNullable(
						buf,
						property.signature(),
						(b, signature) -> StringEncoding.encode(b, signature, MAX_SIGNATURE_LENGTH)
				);
			}
		}
	};

	PacketCodec<ByteBuf, String> PLAYER_NAME = string(16);

	PacketCodec<ByteBuf, GameProfile> GAME_PROFILE = PacketCodec.tuple(
			Uuids.PACKET_CODEC,
			GameProfile::id,
			PLAYER_NAME,
			GameProfile::name,
			PROPERTY_MAP,
			GameProfile::properties,
			GameProfile::new
	);

	PacketCodec<ByteBuf, Integer> RGB = new PacketCodec<ByteBuf, Integer>() {
		@Override
		public Integer decode(ByteBuf buf) {
			return ColorHelper.getArgb(buf.readByte() & 0xFF, buf.readByte() & 0xFF, buf.readByte() & 0xFF);
		}

		@Override
		public void encode(ByteBuf buf, Integer value) {
			buf.writeByte(ColorHelper.getRed(value));
			buf.writeByte(ColorHelper.getGreen(value));
			buf.writeByte(ColorHelper.getBlue(value));
		}
	};

	static PacketCodec<ByteBuf, byte[]> byteArray(int maxLength) {
		return new PacketCodec<ByteBuf, byte[]>() {
			@Override
			public byte[] decode(ByteBuf buf) {
				return PacketByteBuf.readByteArray(buf, maxLength);
			}

			@Override
			public void encode(ByteBuf buf, byte[] value) {
				if (value.length > maxLength) {
					throw new EncoderException(
							"ByteArray with size " + value.length + " is bigger than allowed " + maxLength);
				}

				PacketByteBuf.writeByteArray(buf, value);
			}
		};
	}

	static PacketCodec<ByteBuf, String> string(int maxLength) {
		return new PacketCodec<ByteBuf, String>() {
			@Override
			public String decode(ByteBuf buf) {
				return StringEncoding.decode(buf, maxLength);
			}

			@Override
			public void encode(ByteBuf buf, String value) {
				StringEncoding.encode(buf, value, maxLength);
			}
		};
	}

	static PacketCodec<ByteBuf, Optional<NbtElement>> nbtElement(Supplier<NbtSizeTracker> sizeTrackerSupplier) {
		return new PacketCodec<ByteBuf, Optional<NbtElement>>() {
			@Override
			public Optional<NbtElement> decode(ByteBuf buf) {
				return Optional.ofNullable(PacketByteBuf.readNbt(buf, sizeTrackerSupplier.get()));
			}

			@Override
			public void encode(ByteBuf buf, Optional<NbtElement> value) {
				PacketByteBuf.writeNbt(buf, value.orElse(null));
			}
		};
	}

	static PacketCodec<ByteBuf, NbtElement> nbt(Supplier<NbtSizeTracker> sizeTracker) {
		return new PacketCodec<ByteBuf, NbtElement>() {
			@Override
			public NbtElement decode(ByteBuf buf) {
				NbtElement nbt = PacketByteBuf.readNbt(buf, sizeTracker.get());
				if (nbt == null) {
					throw new DecoderException("Expected non-null compound tag");
				}

				return nbt;
			}

			@Override
			public void encode(ByteBuf buf, NbtElement nbt) {
				if (nbt == NbtEnd.INSTANCE) {
					throw new EncoderException("Expected non-null compound tag");
				}

				PacketByteBuf.writeNbt(buf, nbt);
			}
		};
	}

	static PacketCodec<ByteBuf, NbtCompound> nbtCompound(Supplier<NbtSizeTracker> sizeTracker) {
		return nbt(sizeTracker).xmap(
				nbt -> {
					if (nbt instanceof NbtCompound compound) {
						return compound;
					}

					throw new DecoderException("Not a compound tag: " + nbt);
				},
				nbt -> (NbtElement) nbt
		);
	}

	static <T> PacketCodec<ByteBuf, T> unlimitedCodec(Codec<T> codec) {
		return codec(codec, NbtSizeTracker::ofUnlimitedBytes);
	}

	static <T> PacketCodec<ByteBuf, T> codec(Codec<T> codec) {
		return codec(codec, NbtSizeTracker::forPacket);
	}

	/**
	 * Создаёт {@link PacketCodec.ResultFunction}, которая оборачивает кодек NBT-элемента
	 * в кодек произвольного типа {@code V} через {@link Codec} из DataFixerUpper.
	 */
	static <T, B extends ByteBuf, V> PacketCodec.ResultFunction<B, T, V> fromCodec(DynamicOps<T> ops, Codec<V> codec) {
		return baseCodec -> new PacketCodec<B, V>() {
			@Override
			public V decode(B buf) {
				T nbt = (T) baseCodec.decode(buf);
				return (V) codec
						.parse(ops, nbt)
						.getOrThrow(error -> new DecoderException("Failed to decode: " + error + " " + nbt));
			}

			@Override
			public void encode(B buf, V value) {
				T nbt = (T) codec
						.encodeStart(ops, value)
						.getOrThrow(error -> new EncoderException("Failed to encode: " + error + " " + value));
				baseCodec.encode(buf, nbt);
			}
		};
	}

	static <T> PacketCodec<ByteBuf, T> codec(Codec<T> codec, Supplier<NbtSizeTracker> sizeTracker) {
		return nbt(sizeTracker).collect(fromCodec(NbtOps.INSTANCE, codec));
	}

	static <T> PacketCodec<RegistryByteBuf, T> unlimitedRegistryCodec(Codec<T> codec) {
		return registryCodec(codec, NbtSizeTracker::ofUnlimitedBytes);
	}

	static <T> PacketCodec<RegistryByteBuf, T> registryCodec(Codec<T> codec) {
		return registryCodec(codec, NbtSizeTracker::forPacket);
	}

	static <T> PacketCodec<RegistryByteBuf, T> registryCodec(Codec<T> codec, Supplier<NbtSizeTracker> sizeTracker) {
		final PacketCodec<ByteBuf, NbtElement> nbtCodec = nbt(sizeTracker);
		return new PacketCodec<RegistryByteBuf, T>() {
			@Override
			public T decode(RegistryByteBuf buf) {
				NbtElement nbt = nbtCodec.decode(buf);
				RegistryOps<NbtElement> ops = buf.getRegistryManager().getOps(NbtOps.INSTANCE);
				return (T) codec
						.parse(ops, nbt)
						.getOrThrow(error -> new DecoderException("Failed to decode: " + error + " " + nbt));
			}

			@Override
			public void encode(RegistryByteBuf buf, T value) {
				RegistryOps<NbtElement> ops = buf.getRegistryManager().getOps(NbtOps.INSTANCE);
				NbtElement nbt = (NbtElement) codec
						.encodeStart(ops, value)
						.getOrThrow(error -> new EncoderException("Failed to encode: " + error + " " + value));
				nbtCodec.encode(buf, nbt);
			}
		};
	}

	static <B extends ByteBuf, V> PacketCodec<B, Optional<V>> optional(PacketCodec<? super B, V> codec) {
		return new PacketCodec<B, Optional<V>>() {
			@Override
			public Optional<V> decode(B buf) {
				return buf.readBoolean() ? Optional.of(codec.decode(buf)) : Optional.empty();
			}

			@Override
			public void encode(B buf, Optional<V> value) {
				if (value.isPresent()) {
					buf.writeBoolean(true);
					codec.encode(buf, value.get());
				} else {
					buf.writeBoolean(false);
				}
			}
		};
	}

	static int readCollectionSize(ByteBuf buf, int maxSize) {
		int size = VarInts.read(buf);
		if (size > maxSize) {
			throw new DecoderException(size + " elements exceeded max size of: " + maxSize);
		}

		return size;
	}

	static void writeCollectionSize(ByteBuf buf, int size, int maxSize) {
		if (size > maxSize) {
			throw new EncoderException(size + " elements exceeded max size of: " + maxSize);
		}

		VarInts.write(buf, size);
	}

	static <B extends ByteBuf, V, C extends Collection<V>> PacketCodec<B, C> collection(
			IntFunction<C> factory,
			PacketCodec<? super B, V> elementCodec
	) {
		return collection(factory, elementCodec, Integer.MAX_VALUE);
	}

	static <B extends ByteBuf, V, C extends Collection<V>> PacketCodec<B, C> collection(
			IntFunction<C> factory, PacketCodec<? super B, V> elementCodec, int maxSize
	) {
		return new PacketCodec<B, C>() {
			@Override
			public C decode(B buf) {
				int size = PacketCodecs.readCollectionSize(buf, maxSize);
				C collection = factory.apply(Math.min(size, MAX_COLLECTION_SIZE));

				for (int index = 0; index < size; index++) {
					collection.add(elementCodec.decode(buf));
				}

				return collection;
			}

			@Override
			public void encode(B buf, C collection) {
				PacketCodecs.writeCollectionSize(buf, collection.size(), maxSize);

				for (V element : collection) {
					elementCodec.encode(buf, element);
				}
			}
		};
	}

	static <B extends ByteBuf, V, C extends Collection<V>> PacketCodec.ResultFunction<B, V, C> toCollection(IntFunction<C> collectionFactory) {
		return codec -> collection(collectionFactory, codec);
	}

	static <B extends ByteBuf, V> PacketCodec.ResultFunction<B, V, List<V>> toList() {
		return codec -> collection(ArrayList::new, codec);
	}

	static <B extends ByteBuf, V> PacketCodec.ResultFunction<B, V, List<V>> toList(int maxLength) {
		return codec -> collection(ArrayList::new, codec, maxLength);
	}

	static <B extends ByteBuf, K, V, M extends Map<K, V>> PacketCodec<B, M> map(
			IntFunction<? extends M> factory, PacketCodec<? super B, K> keyCodec, PacketCodec<? super B, V> valueCodec
	) {
		return map(factory, keyCodec, valueCodec, Integer.MAX_VALUE);
	}

	static <B extends ByteBuf, K, V, M extends Map<K, V>> PacketCodec<B, M> map(
			IntFunction<? extends M> factory,
			PacketCodec<? super B, K> keyCodec,
			PacketCodec<? super B, V> valueCodec,
			int maxSize
	) {
		return new PacketCodec<B, M>() {
			@Override
			public M decode(B buf) {
				int size = PacketCodecs.readCollectionSize(buf, maxSize);
				M map = (M) factory.apply(Math.min(size, MAX_COLLECTION_SIZE));

				for (int index = 0; index < size; index++) {
					K key = keyCodec.decode(buf);
					V value = valueCodec.decode(buf);
					map.put(key, value);
				}

				return map;
			}

			@Override
			public void encode(B buf, M map) {
				PacketCodecs.writeCollectionSize(buf, map.size(), maxSize);
				map.forEach((key, value) -> {
					keyCodec.encode(buf, (K) key);
					valueCodec.encode(buf, (V) value);
				});
			}
		};
	}

	static <B extends ByteBuf, L, R> PacketCodec<B, Either<L, R>> either(
			PacketCodec<? super B, L> left,
			PacketCodec<? super B, R> right
	) {
		return new PacketCodec<B, Either<L, R>>() {
			@Override
			public Either<L, R> decode(B buf) {
				return buf.readBoolean()
						? Either.left(left.decode(buf))
						: Either.right(right.decode(buf));
			}

			@Override
			public void encode(B buf, Either<L, R> either) {
				either.ifLeft(value -> {
					buf.writeBoolean(true);
					left.encode(buf, (L) value);
				}).ifRight(value -> {
					buf.writeBoolean(false);
					right.encode(buf, (R) value);
				});
			}
		};
	}

	/**
	 * Создаёт {@link PacketCodec.ResultFunction}, которая оборачивает кодек в length-prefixed формат:
	 * перед данными записывается VarInt с размером буфера, что позволяет читателю пропустить
	 * неизвестные поля при несовпадении версий протокола.
	 */
	static <B extends ByteBuf, V> PacketCodec.ResultFunction<B, V, V> lengthPrepended(
			int maxSize,
			BiFunction<B, ByteBuf, B> bufWrapper
	) {
		return codec -> new PacketCodec<B, V>() {
			@Override
			public V decode(B buf) {
				int length = VarInts.read(buf);
				if (length > maxSize) {
					throw new DecoderException("Buffer size " + length + " is larger than allowed limit of " + maxSize);
				}

				int startIndex = buf.readerIndex();
				B sliced = (B) ((ByteBuf) bufWrapper.apply(buf, buf.slice(startIndex, length)));
				buf.readerIndex(startIndex + length);
				return (V) codec.decode(sliced);
			}

			@Override
			public void encode(B buf, V value) {
				B temp = (B) ((ByteBuf) bufWrapper.apply(buf, buf.alloc().buffer()));

				try {
					codec.encode(temp, value);
					int length = temp.readableBytes();
					if (length > maxSize) {
						throw new EncoderException("Buffer size " + length + " is  larger than allowed limit of " + maxSize);
					}

					VarInts.write(buf, length);
					buf.writeBytes(temp);
				} finally {
					temp.release();
				}
			}
		};
	}

	static <V> PacketCodec.ResultFunction<ByteBuf, V, V> lengthPrepended(int maxSize) {
		return lengthPrepended(maxSize, (buf, bufToWrap) -> bufToWrap);
	}

	static <V> PacketCodec.ResultFunction<RegistryByteBuf, V, V> lengthPrependedRegistry(int maxSize) {
		return lengthPrepended(
				maxSize,
				(registryBuf, buf) -> new RegistryByteBuf(buf, registryBuf.getRegistryManager())
		);
	}

	static <T> PacketCodec<ByteBuf, T> indexed(IntFunction<T> indexToValue, ToIntFunction<T> valueToIndex) {
		return new PacketCodec<ByteBuf, T>() {
			@Override
			public T decode(ByteBuf buf) {
				int index = VarInts.read(buf);
				return indexToValue.apply(index);
			}

			@Override
			public void encode(ByteBuf buf, T value) {
				int index = valueToIndex.applyAsInt(value);
				VarInts.write(buf, index);
			}
		};
	}

	static <T> PacketCodec<ByteBuf, T> entryOf(IndexedIterable<T> iterable) {
		return indexed(iterable::getOrThrow, iterable::getRawIdOrThrow);
	}

	private static <T, R> PacketCodec<RegistryByteBuf, R> registry(
			RegistryKey<? extends Registry<T>> registry, Function<Registry<T>, IndexedIterable<R>> registryTransformer
	) {
		return new PacketCodec<RegistryByteBuf, R>() {
			private IndexedIterable<R> getIndexedEntries(RegistryByteBuf buf) {
				return registryTransformer.apply(buf.getRegistryManager().getOrThrow(registry));
			}

			@Override
			public R decode(RegistryByteBuf buf) {
				int index = VarInts.read(buf);
				return (R) getIndexedEntries(buf).getOrThrow(index);
			}

			@Override
			public void encode(RegistryByteBuf buf, R value) {
				int index = getIndexedEntries(buf).getRawIdOrThrow(value);
				VarInts.write(buf, index);
			}
		};
	}

	static <T> PacketCodec<RegistryByteBuf, T> registryValue(RegistryKey<? extends Registry<T>> registry) {
		return registry(registry, registryx -> registryx);
	}

	static <T> PacketCodec<RegistryByteBuf, RegistryEntry<T>> registryEntry(RegistryKey<? extends Registry<T>> registry) {
		return registry(registry, Registry::getIndexedEntries);
	}

	/**
	 * Создаёт кодек для {@link RegistryEntry}, поддерживающий как ссылочные записи (по индексу реестра),
	 * так и прямые (inline) записи, закодированные через {@code directCodec}.
	 * Маркер {@code 0} означает прямую запись; {@code index + 1} — ссылочную.
	 */
	static <T> PacketCodec<RegistryByteBuf, RegistryEntry<T>> registryEntry(
			RegistryKey<? extends Registry<T>> registry, PacketCodec<? super RegistryByteBuf, T> directCodec
	) {
		return new PacketCodec<RegistryByteBuf, RegistryEntry<T>>() {
			private static final int DIRECT_ENTRY_MARKER = 0;

			private IndexedIterable<RegistryEntry<T>> getIndexedEntries(RegistryByteBuf buf) {
				return buf.getRegistryManager().getOrThrow(registry).getIndexedEntries();
			}

			@Override
			public RegistryEntry<T> decode(RegistryByteBuf buf) {
				int rawId = VarInts.read(buf);
				return rawId == DIRECT_ENTRY_MARKER
						? RegistryEntry.of(directCodec.decode(buf))
						: (RegistryEntry<T>) getIndexedEntries(buf).getOrThrow(rawId - 1);
			}

			@Override
			public void encode(RegistryByteBuf buf, RegistryEntry<T> entry) {
				switch (entry.getType()) {
					case REFERENCE:
						int rawId = getIndexedEntries(buf).getRawIdOrThrow(entry);
						VarInts.write(buf, rawId + 1);
						break;
					case DIRECT:
						VarInts.write(buf, DIRECT_ENTRY_MARKER);
						directCodec.encode(buf, entry.value());
				}
			}
		};
	}

	/**
	 * Создаёт кодек для {@link RegistryEntryList}: поддерживает как тег (по идентификатору),
	 * так и явный список записей. Маркер {@code -1} (encoded как {@code 0}) означает тег.
	 */
	static <T> PacketCodec<RegistryByteBuf, RegistryEntryList<T>> registryEntryList(RegistryKey<? extends Registry<T>> registryRef) {
		return new PacketCodec<RegistryByteBuf, RegistryEntryList<T>>() {
			private static final int TAG_ENTRY_MARKER = -1;
			private final PacketCodec<RegistryByteBuf, RegistryEntry<T>> entryCodec =
					PacketCodecs.registryEntry(registryRef);

			@Override
			public RegistryEntryList<T> decode(RegistryByteBuf buf) {
				int sizeOrTag = VarInts.read(buf) - 1;
				if (sizeOrTag == TAG_ENTRY_MARKER) {
					Registry<T> reg = buf.getRegistryManager().getOrThrow(registryRef);
					return reg
							.getOptional(TagKey.of(registryRef, Identifier.PACKET_CODEC.decode(buf)))
							.orElseThrow();
				}

				List<RegistryEntry<T>> list = new ArrayList<>(Math.min(sizeOrTag, MAX_COLLECTION_SIZE));

				for (int index = 0; index < sizeOrTag; index++) {
					list.add(entryCodec.decode(buf));
				}

				return RegistryEntryList.of(list);
			}

			@Override
			public void encode(RegistryByteBuf buf, RegistryEntryList<T> entryList) {
				Optional<TagKey<T>> tagKey = entryList.getTagKey();
				if (tagKey.isPresent()) {
					VarInts.write(buf, 0);
					Identifier.PACKET_CODEC.encode(buf, tagKey.get().id());
				} else {
					VarInts.write(buf, entryList.size() + 1);
	
					for (RegistryEntry<T> entry : entryList) {
						entryCodec.encode(buf, entry);
					}
				}
			}
		};
	}

	static PacketCodec<ByteBuf, JsonElement> lenientJson(int maxLength) {
		return new PacketCodec<ByteBuf, JsonElement>() {
			private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

			@Override
			public JsonElement decode(ByteBuf buf) {
				String json = StringEncoding.decode(buf, maxLength);

				try {
					return LenientJsonParser.parse(json);
				} catch (JsonSyntaxException e) {
					throw new DecoderException("Failed to parse JSON", e);
				}
			}

			@Override
			public void encode(ByteBuf buf, JsonElement value) {
				String json = GSON.toJson(value);
				StringEncoding.encode(buf, json, maxLength);
			}
		};
	}
}
