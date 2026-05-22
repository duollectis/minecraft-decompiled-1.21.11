package net.minecraft.data.report;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.network.state.*;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Генерирует JSON-отчёт о всех сетевых пакетах игры, сгруппированных по фазе и стороне.
 * Обходит все фабрики состояний сети (handshake, query, login, configuration, play)
 * и записывает их в {@code reports/packets.json} в формате: фаза → сторона → пакет → protocol_id.
 */
public class PacketReportProvider implements DataProvider {

	private final DataOutput output;

	public PacketReportProvider(DataOutput output) {
		this.output = output;
	}

	@Override
	public CompletableFuture<?> run(DataWriter writer) {
		Path path = output.resolvePath(DataOutput.OutputType.REPORTS).resolve("packets.json");
		return DataProvider.writeToPath(writer, toJson(), path);
	}

	/**
	 * Строит JSON-дерево всех пакетов, сгруппированных по фазе протокола.
	 * Структура: {@code { "<phase>": { "<side>": { "<packet_id>": { "protocol_id": N } } } }}.
	 */
	private JsonElement toJson() {
		JsonObject root = new JsonObject();

		Stream.of(
			HandshakeStates.C2S_FACTORY,
			QueryStates.S2C_FACTORY,
			QueryStates.C2S_FACTORY,
			LoginStates.S2C_FACTORY,
			LoginStates.C2S_FACTORY,
			ConfigurationStates.S2C_FACTORY,
			ConfigurationStates.C2S_FACTORY,
			PlayStateFactories.S2C,
			PlayStateFactories.C2S
		)
			.map(NetworkState.Factory::buildUnbound)
			.collect(Collectors.groupingBy(NetworkState.Unbound::phase))
			.forEach((phase, states) -> {
				JsonObject phaseJson = new JsonObject();
				root.add(phase.getId(), phaseJson);

				states.forEach(state -> {
					JsonObject sideJson = new JsonObject();
					phaseJson.add(state.side().getName(), sideJson);

					state.forEachPacketType((packetType, protocolId) -> {
						JsonObject packetJson = new JsonObject();
						packetJson.addProperty("protocol_id", protocolId);
						sideJson.add(packetType.id().toString(), packetJson);
					});
				});
			});

		return root;
	}

	@Override
	public String getName() {
		return "Packet Report";
	}
}
