package net.minecraft.client.realms.dto;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.mojang.util.UUIDTypeAdapter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.RealmsSerializable;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Environment(EnvType.CLIENT)
/**
 * {@code SentInvite}.
 */
public class SentInvite implements RealmsSerializable {

	@SerializedName("name")
	public @Nullable String profileName;
	@SerializedName("uuid")
	@JsonAdapter(UUIDTypeAdapter.class)
	public @Nullable UUID uuid;
}
