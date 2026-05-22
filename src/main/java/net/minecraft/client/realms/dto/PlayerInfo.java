package net.minecraft.client.realms.dto;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.mojang.util.UUIDTypeAdapter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.RealmsSerializable;

import java.util.UUID;

/**
 * DTO информации об игроке, приглашённом на сервер Realms.
 * Содержит имя, UUID и флаги статуса: оператор, принял приглашение, онлайн.
 */
@Environment(EnvType.CLIENT)
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

	public PlayerInfo(String name, UUID uuid, boolean operator, boolean accepted, boolean online) {
		this.name = name;
		this.uuid = uuid;
		this.operator = operator;
		this.accepted = accepted;
		this.online = online;
	}
}
