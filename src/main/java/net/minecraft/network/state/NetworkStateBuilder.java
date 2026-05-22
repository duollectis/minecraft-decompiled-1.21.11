package net.minecraft.network.state;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.NetworkPhase;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.handler.PacketBundleHandler;
import net.minecraft.network.handler.SideValidatingDispatchingCodecBuilder;
import net.minecraft.network.listener.ClientPacketListener;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.listener.ServerPacketListener;
import net.minecraft.network.packet.BundlePacket;
import net.minecraft.network.packet.BundleSplitterPacket;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketCodecModifier;
import net.minecraft.util.Unit;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Строитель {@link NetworkState} для конкретной фазы и стороны протокола.
 * Регистрирует типы пакетов с их кодеками и опциональными модификаторами,
 * затем создаёт фабрику состояний через {@link #buildFactory} или {@link #buildContextAwareFactory}.
 *
 * @param <T> тип слушателя пакетов
 * @param <B> тип буфера (обычно {@link net.minecraft.network.RegistryByteBuf})
 * @param <C> тип контекста для модификаторов кодеков
 */
public class NetworkStateBuilder<T extends PacketListener, B extends ByteBuf, C> {

	final NetworkPhase phase;
	final NetworkSide side;
	private final List<NetworkStateBuilder.PacketType<T, ?, B, C>> packetTypes = new ArrayList<>();
	private @Nullable PacketBundleHandler bundleHandler;

	public NetworkStateBuilder(NetworkPhase phase, NetworkSide side) {
		this.phase = phase;
		this.side = side;
	}

	public <P extends Packet<? super T>> NetworkStateBuilder<T, B, C> add(
			net.minecraft.network.packet.PacketType<P> type,
			PacketCodec<? super B, P> codec
	) {
		packetTypes.add(new NetworkStateBuilder.PacketType<>(type, codec, null));
		return this;
	}

	public <P extends Packet<? super T>> NetworkStateBuilder<T, B, C> add(
			net.minecraft.network.packet.PacketType<P> type,
			PacketCodec<? super B, P> codec,
			PacketCodecModifier<B, P, C> modifier
	) {
		packetTypes.add(new NetworkStateBuilder.PacketType<>(type, codec, modifier));
		return this;
	}

	/**
	 * Регистрирует bundle-пакет: добавляет разделитель-сплиттер и создаёт обработчик bundle.
	 *
	 * @param id      тип bundle-пакета
	 * @param bundler фабрика bundle-пакета из набора пакетов
	 * @param splitter маркерный пакет-разделитель
	 */
	public <P extends BundlePacket<? super T>, D extends BundleSplitterPacket<? super T>> NetworkStateBuilder<T, B, C> addBundle(
			net.minecraft.network.packet.PacketType<P> id,
			Function<Iterable<Packet<? super T>>, P> bundler,
			D splitter
	) {
		PacketCodec<ByteBuf, D> splitterCodec = PacketCodec.unit(splitter);
		@SuppressWarnings("unchecked")
		net.minecraft.network.packet.PacketType<D> splitterType =
				(net.minecraft.network.packet.PacketType<D>) splitter.getPacketType();
		packetTypes.add(new NetworkStateBuilder.PacketType<>(splitterType, splitterCodec, null));
		bundleHandler = PacketBundleHandler.create(id, bundler, splitter);
		return this;
	}

	PacketCodec<ByteBuf, Packet<? super T>> createCodec(
			Function<ByteBuf, B> bufUpgrader,
			List<NetworkStateBuilder.PacketType<T, ?, B, C>> types,
			C context
	) {
		SideValidatingDispatchingCodecBuilder<ByteBuf, T> builder =
				new SideValidatingDispatchingCodecBuilder<>(side);

		for (NetworkStateBuilder.PacketType<T, ?, B, C> packetType : types) {
			packetType.add(builder, bufUpgrader, context);
		}

		return builder.build();
	}

	private static NetworkState.Unbound createUnbound(
			NetworkPhase phase,
			NetworkSide side,
			List<? extends NetworkStateBuilder.PacketType<?, ?, ?, ?>> types
	) {
		return new NetworkState.Unbound() {
			@Override
			public NetworkPhase phase() {
				return phase;
			}

			@Override
			public NetworkSide side() {
				return side;
			}

			@Override
			public void forEachPacketType(NetworkState.Unbound.PacketTypeConsumer callback) {
				for (int index = 0; index < types.size(); index++) {
					callback.accept(types.get(index).type(), index);
				}
			}
		};
	}

	/**
	 * Строит фабрику состояний с фиксированным контекстом.
	 *
	 * @param context контекст для модификаторов кодеков
	 * @return фабрика, привязывающая состояние к реестру через {@link NetworkStateFactory#bind}
	 */
	public NetworkStateFactory<T, B> buildFactory(C context) {
		final List<NetworkStateBuilder.PacketType<T, ?, B, C>> frozenTypes = List.copyOf(packetTypes);
		final PacketBundleHandler frozenBundleHandler = bundleHandler;
		final NetworkState.Unbound unbound = createUnbound(phase, side, frozenTypes);

		return new NetworkStateFactory<>() {
			@Override
			public NetworkState<T> bind(Function<ByteBuf, B> registryBinder) {
				return new NetworkStateBuilder.NetworkStateImpl<>(
						phase,
						side,
						createCodec(registryBinder, frozenTypes, context),
						frozenBundleHandler
				);
			}

			@Override
			public NetworkState.Unbound buildUnbound() {
				return unbound;
			}
		};
	}

	/**
	 * Строит фабрику состояний, принимающую контекст при каждой привязке.
	 * Используется когда контекст зависит от конкретного соединения (например, режим игры).
	 *
	 * @return контекстно-зависимая фабрика состояний
	 */
	public ContextAwareNetworkStateFactory<T, B, C> buildContextAwareFactory() {
		final List<NetworkStateBuilder.PacketType<T, ?, B, C>> frozenTypes = List.copyOf(packetTypes);
		final PacketBundleHandler frozenBundleHandler = bundleHandler;
		final NetworkState.Unbound unbound = createUnbound(phase, side, frozenTypes);

		return new ContextAwareNetworkStateFactory<>() {
			@Override
			public NetworkState<T> bind(Function<ByteBuf, B> registryBinder, C context) {
				return new NetworkStateBuilder.NetworkStateImpl<>(
						phase,
						side,
						createCodec(registryBinder, frozenTypes, context),
						frozenBundleHandler
				);
			}

			@Override
			public NetworkState.Unbound buildUnbound() {
				return unbound;
			}
		};
	}

	private static <L extends PacketListener, B extends ByteBuf> NetworkStateFactory<L, B> build(
			NetworkPhase phase,
			NetworkSide side,
			Consumer<NetworkStateBuilder<L, B, Unit>> registrar
	) {
		NetworkStateBuilder<L, B, Unit> builder = new NetworkStateBuilder<>(phase, side);
		registrar.accept(builder);
		return builder.buildFactory(Unit.INSTANCE);
	}

	public static <T extends ServerPacketListener, B extends ByteBuf> NetworkStateFactory<T, B> c2s(
			NetworkPhase phase, Consumer<NetworkStateBuilder<T, B, Unit>> registrar
	) {
		return build(phase, NetworkSide.SERVERBOUND, registrar);
	}

	public static <T extends ClientPacketListener, B extends ByteBuf> NetworkStateFactory<T, B> s2c(
			NetworkPhase phase, Consumer<NetworkStateBuilder<T, B, Unit>> registrar
	) {
		return build(phase, NetworkSide.CLIENTBOUND, registrar);
	}

	private static <L extends PacketListener, B extends ByteBuf, C> ContextAwareNetworkStateFactory<L, B, C> buildContextAware(
			NetworkPhase phase,
			NetworkSide side,
			Consumer<NetworkStateBuilder<L, B, C>> registrar
	) {
		NetworkStateBuilder<L, B, C> builder = new NetworkStateBuilder<>(phase, side);
		registrar.accept(builder);
		return builder.buildContextAwareFactory();
	}

	public static <T extends ServerPacketListener, B extends ByteBuf, C> ContextAwareNetworkStateFactory<T, B, C> contextAwareC2S(
			NetworkPhase phase, Consumer<NetworkStateBuilder<T, B, C>> registrar
	) {
		return buildContextAware(phase, NetworkSide.SERVERBOUND, registrar);
	}

	public static <T extends ClientPacketListener, B extends ByteBuf, C> ContextAwareNetworkStateFactory<T, B, C> contextAwareS2C(
			NetworkPhase phase, Consumer<NetworkStateBuilder<T, B, C>> registrar
	) {
		return buildContextAware(phase, NetworkSide.CLIENTBOUND, registrar);
	}

	record NetworkStateImpl<L extends PacketListener>(
			NetworkPhase id,
			NetworkSide side,
			PacketCodec<ByteBuf, Packet<? super L>> codec,
			@Nullable PacketBundleHandler bundleHandler
	) implements NetworkState<L> {
	}

	record PacketType<T extends PacketListener, P extends Packet<? super T>, B extends ByteBuf, C>(
			net.minecraft.network.packet.PacketType<P> type,
			PacketCodec<? super B, P> codec,
			@Nullable PacketCodecModifier<B, P, C> modifier
	) {

		public void add(
				SideValidatingDispatchingCodecBuilder<ByteBuf, T> builder,
				Function<ByteBuf, B> bufUpgrader,
				C context
		) {
			PacketCodec<? super B, P> resolvedCodec = modifier != null
					? modifier.apply(codec, context)
					: codec;

			builder.add(type, resolvedCodec.mapBuf(bufUpgrader));
		}
	}
}
