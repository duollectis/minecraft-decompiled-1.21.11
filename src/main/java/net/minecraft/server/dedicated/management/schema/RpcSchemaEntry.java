package net.minecraft.server.dedicated.management.schema;

import java.net.URI;
import java.util.List;

/**
 * {@code RpcSchemaEntry}.
 */
public record RpcSchemaEntry<T>(String name, URI reference, RpcSchema<T> schema) {

	/**
	 * Ref.
	 *
	 * @return RpcSchema — результат операции
	 */
	public RpcSchema<T> ref() {
		return RpcSchema.ofReference(this.reference, this.schema.codec());
	}

	/**
	 * Array.
	 *
	 * @return RpcSchema> — результат операции
	 */
	public RpcSchema<List<T>> array() {
		return RpcSchema.ofArray(this.ref(), this.schema.codec());
	}
}
