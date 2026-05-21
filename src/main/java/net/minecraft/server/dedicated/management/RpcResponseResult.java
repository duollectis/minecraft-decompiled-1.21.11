package net.minecraft.server.dedicated.management;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.dedicated.management.schema.RpcSchema;

/**
 * {@code RpcResponseResult}.
 */
public record RpcResponseResult<Result>(String name, RpcSchema<Result> schema) {

	public static <Result> Codec<RpcResponseResult<Result>> getCodec() {
		return RecordCodecBuilder.<RpcResponseResult<Result>>create(
				instance -> instance.group(
						                    Codec.STRING.fieldOf("name").forGetter(RpcResponseResult::name),
						                    RpcSchema.<Result>getCodec().fieldOf("schema").forGetter(RpcResponseResult::schema)
				                    )
				                    .apply(instance, RpcResponseResult::new)
		);
	}
}
