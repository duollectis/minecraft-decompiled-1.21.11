package com.mojang.blaze3d.shaders;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.util.Identifier;
import net.minecraft.util.annotation.DeobfuscateClass;
import org.jspecify.annotations.Nullable;

/**
 * Тип шейдера в графическом конвейере.
 * Каждый тип имеет своё имя и расширение файла для поиска ресурсов.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public enum ShaderType {
	VERTEX("vertex", ".vsh"),
	FRAGMENT("fragment", ".fsh");

	private static final ShaderType[] TYPES = values();

	private final String name;
	private final String extension;

	ShaderType(String name, String extension) {
		this.name = name;
		this.extension = extension;
	}

	/**
	 * Определяет тип шейдера по расширению файла в идентификаторе ресурса.
	 *
	 * @param id идентификатор ресурса шейдера
	 * @return тип шейдера, или {@code null} если расширение не распознано
	 */
	public static @Nullable ShaderType byLocation(Identifier id) {
		for (ShaderType type : TYPES) {
			if (id.getPath().endsWith(type.extension)) {
				return type;
			}
		}

		return null;
	}

	public String getName() {
		return name;
	}

	/** Возвращает {@link ResourceFinder} для поиска шейдерных файлов данного типа. */
	public ResourceFinder idConverter() {
		return new ResourceFinder("shaders", extension);
	}
}
