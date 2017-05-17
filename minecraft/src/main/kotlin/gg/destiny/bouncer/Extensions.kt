package gg.destiny.bouncer

import net.minecraft.server.MinecraftServer
import net.minecraft.util.text.TextComponentString

fun MinecraftServer.broadcastChatMessage(message: String) {
  playerList.sendMessage(TextComponentString(message))
}
