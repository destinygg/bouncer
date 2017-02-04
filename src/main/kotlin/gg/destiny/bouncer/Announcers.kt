package gg.destiny.bouncer

import net.minecraft.util.EnumChatFormatting.*

interface Announcer {
  fun announceTrusted(playerName: String): String
  fun announceSubscriber(playerName: String, authorizedName: String): String
  fun kickMessage(playerName: String): String
}

object DefaultAnnouncer : Announcer {
  override fun announceTrusted(playerName: String) =
    "${GREEN}Trusted user $RED$playerName$WHITE connected"

  override fun announceSubscriber(playerName: String, authorizedName: String) =
    "${GREEN}Subscriber $RED$authorizedName$WHITE connected as $BLUE$playerName"

  override fun kickMessage(playerName: String) =
    "Sorry, you need an active destiny.gg subscription to play!"
}
