package net.minecraft.server.dedicated.management;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * {@code JsonRpc}.
 */
public class JsonRpc {

	public static final String JSON_RPC_VERSION = "2.0";

	/**
	 * Кодирует result.
	 *
	 * @param id id
	 * @param result result
	 *
	 * @return JsonObject — результат операции
	 */
	public static JsonObject encodeResult(JsonElement id, JsonElement result) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("jsonrpc", "2.0");
		jsonObject.add("id", id);
		jsonObject.add("result", result);
		return jsonObject;
	}

	/**
	 * Кодирует request.
	 *
	 * @param id id
	 * @param method method
	 * @param parameters parameters
	 *
	 * @return JsonObject — результат операции
	 */
	public static JsonObject encodeRequest(@Nullable Integer id, Identifier method, List<JsonElement> parameters) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("jsonrpc", "2.0");
		if (id != null) {
			jsonObject.addProperty("id", id);
		}

		jsonObject.addProperty("method", method.toString());
		if (!parameters.isEmpty()) {
			JsonArray jsonArray = new JsonArray(parameters.size());

			for (JsonElement jsonElement : parameters) {
				jsonArray.add(jsonElement);
			}

			jsonObject.add("params", jsonArray);
		}

		return jsonObject;
	}

	/**
	 * Кодирует error.
	 *
	 * @param id id
	 * @param message message
	 * @param code code
	 * @param data data
	 *
	 * @return JsonObject — результат операции
	 */
	public static JsonObject encodeError(JsonElement id, String message, int code, @Nullable String data) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("jsonrpc", "2.0");
		jsonObject.add("id", id);
		JsonObject jsonObject2 = new JsonObject();
		jsonObject2.addProperty("code", code);
		jsonObject2.addProperty("message", message);
		if (data != null && !data.isBlank()) {
			jsonObject2.addProperty("data", data);
		}

		jsonObject.add("error", jsonObject2);
		return jsonObject;
	}

	public static @Nullable JsonElement getId(JsonObject request) {
		return request.get("id");
	}

	public static @Nullable String getMethod(JsonObject request) {
		return JsonHelper.getString(request, "method", null);
	}

	public static @Nullable JsonElement getParameters(JsonObject request) {
		return request.get("params");
	}

	public static @Nullable JsonElement getResult(JsonObject response) {
		return response.get("result");
	}

	public static @Nullable JsonObject getError(JsonObject response) {
		return JsonHelper.getObject(response, "error", null);
	}
}
