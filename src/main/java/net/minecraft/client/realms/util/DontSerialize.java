package net.minecraft.client.realms.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Environment(EnvType.CLIENT)
/**
 * {@code DontSerialize}.
 */
public @interface DontSerialize {
}
