package net.minecraft.client.realms.dto;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.mojang.util.UUIDTypeAdapter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.RealmsSerializable;

import java.util.UUID;

@Environment(EnvType.CLIENT)
/**
 * {@code PlayerInfo}.
 */
public class PlayerInfo extends ValueObject implements RealmsSerializable {

	@SerializedName("name")
	public final String name;
	@SerializedName("uuid")
	@JsonAdapter(UUIDTypeAdapter.class)
	public final UUID uuid;
	@SerializedName("operator")
	public boolean operator;
	@SerializedName("accepted")
	public final boolean accepted;
	@SerializedName("online")
	public final boolean online;

	public PlayerInfo(String string, UUID uUID, boolean bl, boolean bl2, boolean bl3) {
		this.name = string;
		this.uuid = uUID;
		this.operator = bl;
		this.accepted = bl2;
		this.online = bl3;
	}
}
