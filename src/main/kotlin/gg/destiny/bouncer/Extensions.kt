package gg.destiny.bouncer

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.server.MinecraftServer
import net.minecraft.util.ChatComponentText

fun MinecraftServer.broadcastChatMessage(message: String) {
  configurationManager.playerEntityList.forEach {
    it as? EntityPlayer ?: return@forEach
    it.addChatMessage(ChatComponentText(message))
  }
}
