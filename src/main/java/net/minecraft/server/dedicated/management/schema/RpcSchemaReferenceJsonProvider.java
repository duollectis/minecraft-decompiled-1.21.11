package net.minecraft.server.dedicated.management.schema;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.server.dedicated.management.RpcDiscover;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * {@code RpcSchemaReferenceJsonProvider}.
 */
public class RpcSchemaReferenceJsonProvider implements DataProvider {

	private final Path path;

	public RpcSchemaReferenceJsonProvider(DataOutput output) {
		this.path = output.resolvePath(DataOutput.OutputType.REPORTS).resolve("json-rpc-api-schema.json");
	}

	@Override
	public CompletableFuture<?> run(DataWriter writer) {
		RpcDiscover.Document document = RpcDiscover.handleRpcDiscover(RpcSchema.getRegisteredSchemas());
		return DataProvider.writeToPath(
				writer,
				(JsonElement) RpcDiscover.Document.CODEC.codec().encodeStart(JsonOps.INSTANCE, document).getOrThrow(),
				this.path
		);
	}

	@Override
	public String getName() {
		return "Json RPC API schema";
	}
}
