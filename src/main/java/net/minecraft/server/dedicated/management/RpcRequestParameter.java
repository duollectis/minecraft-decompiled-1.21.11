package net.minecraft.server.dedicated.management;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.dedicated.management.schema.RpcSchema;

/**
 * {@code RpcRequestParameter}.
 */
public record RpcRequestParameter<Param>(String name, RpcSchema<Param> schema, boolean required) {

	public RpcRequestParameter(String name, RpcSchema<Param> schema) {
		this(name, schema, true);
	}

	public static <Param> MapCodec<RpcRequestParameter<Param>> createCodec() {
		return RecordCodecBuilder.<RpcRequestParameter<Param>>mapCodec(
				instance -> instance.group(
						                    Codec.STRING.fieldOf("name").forGetter(RpcRequestParameter::name),
						                    RpcSchema.<Param>getCodec().fieldOf("schema").forGetter(RpcRequestParameter::schema),
						                    Codec.BOOL.fieldOf("required").forGetter(RpcRequestParameter::required)
				                    )
				                    .apply(instance, RpcRequestParameter::new)
		);
	}
}
