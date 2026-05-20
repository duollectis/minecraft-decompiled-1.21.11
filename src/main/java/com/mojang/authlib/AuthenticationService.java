package com.mojang.authlib;

import com.mojang.authlib.minecraft.MinecraftSessionService;

public interface AuthenticationService {
   MinecraftSessionService createMinecraftSessionService();

   GameProfileRepository createProfileRepository();
}
