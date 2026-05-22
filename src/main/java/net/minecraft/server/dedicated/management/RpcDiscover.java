package net.minecraft.server.dedicated.management;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.Registries;
import net.minecraft.server.dedicated.management.schema.RpcSchema;
import net.minecraft.server.dedicated.management.schema.RpcSchemaEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Класс Rpc Discover.
 */
public class RpcDiscover {

	public static RpcDiscover.Document handleRpcDiscover(List<RpcSchemaEntry<?>> entries) {
		List<RpcMethodInfo.Entry<?, ?>>
				list =
				new ArrayList<>(Registries.INCOMING_RPC_METHOD.size() + Registries.OUTGOING_RPC_METHOD.size());
		Registries.INCOMING_RPC_METHOD.streamEntries().forEach(entry -> {
			if (entry.value().attributes().discoverable()) {
				list.add(entry.value().info().toEntry(entry.registryKey().getValue()));
			}
		});
		Registries.OUTGOING_RPC_METHOD.streamEntries().forEach(entry -> {
			if (entry.value().attributes().discoverable()) {
				list.add(entry.value().info().toEntry(entry.registryKey().getValue()));
			}
		});
		Map<String, RpcSchema<?>> map = new HashMap<>();

		for (RpcSchemaEntry<?> rpcSchemaEntry : entries) {
			map.put(rpcSchemaEntry.name(), rpcSchemaEntry.schema().copy());
		}

		RpcDiscover.Info info = new RpcDiscover.Info("Minecraft Server JSON-RPC", "2.0.0");
		return new RpcDiscover.Document("1.3.2", info, list, new RpcDiscover.Components(map));
	}

	public record Components(Map<String, RpcSchema<?>> schemas) {

		public static final MapCodec<RpcDiscover.Components> CODEC = createCodec();

		@SuppressWarnings("unchecked")
		private static MapCodec<RpcDiscover.Components> createCodec() {
			Codec<Map<String, RpcSchema<?>>>
					schemasCodec =
					(Codec<Map<String, RpcSchema<?>>>) (Codec<?>) Codec.unboundedMap(Codec.STRING, RpcSchema.CODEC);
			return RecordCodecBuilder.<RpcDiscover.Components>mapCodec(
					instance -> instance
							.group(schemasCodec.fieldOf("schemas").forGetter(RpcDiscover.Components::schemas))
							.apply(instance, RpcDiscover.Components::new)
			);
		}
	}

	public record Document(
			String jsonRpcProtocolVersion,
			RpcDiscover.Info discoverInfo,
			List<RpcMethodInfo.Entry<?, ?>> methods,
			RpcDiscover.Components components
	) {

		public static final MapCodec<RpcDiscover.Document> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    Codec.STRING.fieldOf("openrpc").forGetter(RpcDiscover.Document::jsonRpcProtocolVersion),
						                    RpcDiscover.Info.CODEC.codec().fieldOf("info").forGetter(RpcDiscover.Document::discoverInfo),
						                    Codec
								                    .list(RpcMethodInfo.Entry.CODEC)
								                    .fieldOf("methods")
								                    .forGetter(RpcDiscover.Document::methods),
						                    RpcDiscover.Components.CODEC
								                    .codec()
								                    .fieldOf("components")
								                    .forGetter(RpcDiscover.Document::components)
				                    )
				                    .apply(instance, RpcDiscover.Document::new)
		);
	}

	public record Info(String title, String version) {

		public static final MapCodec<RpcDiscover.Info> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    Codec.STRING.fieldOf("title").forGetter(RpcDiscover.Info::title),
						                    Codec.STRING.fieldOf("version").forGetter(RpcDiscover.Info::version)
				                    )
				                    .apply(instance, RpcDiscover.Info::new)
		);
	}
}
