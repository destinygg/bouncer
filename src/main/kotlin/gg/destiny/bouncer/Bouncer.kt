package gg.destiny.bouncer

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.FMLLog
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLServerStartingEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.command.WrongUsageException
import net.minecraft.network.NetHandlerPlayServer
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.MathHelper
import org.apache.logging.log4j.Logger
import rx.schedulers.Schedulers
import java.io.File
import java.util.*

private val gson = Gson()

class AuthListener(val server: MinecraftServer,
                   val logger: Logger,
                   val authorizer: Authorizer,
                   val announcer: Announcer = DefaultAnnouncer,
                   val trustedUsers: Set<UUID> = mutableSetOf()) {
  var enabled: Boolean = true
    set(value) {
      field = value
      logger.info("Bouncer ${if (value) "enabled" else "disabled"}")
    }

  @SubscribeEvent(priority = EventPriority.HIGHEST)
  fun onLogin(event: FMLNetworkEvent.ServerConnectionFromClientEvent) {
    if (!enabled) return

    val handler = (event.handler as? NetHandlerPlayServer) ?: return
    val playerName = handler.playerEntity.name
    val playerId = handler.playerEntity.uniqueID

    if (trustedUsers.contains(playerId)) {
      logger.info("Trusted user connected: $playerName ($playerId)")
      server.broadcastChatMessage(announcer.announceTrusted(playerName))
      return
    }

    logger.info("Authenticating: $playerName -> $playerId")
    authorizer.authorize(playerId, playerName)
      .subscribeOn(Schedulers.io())
      .subscribe({ subscriberName ->
        logger.info("Successfully authenticated: $subscriberName ($playerName)")
        server.broadcastChatMessage(announcer.announceSubscriber(playerName, subscriberName))
      }, { error ->
        if (error is Authorizer.AuthFailedException) {
          logger.error("Failed to authenticate: $playerName ($playerId)")
        } else {
          logger.error("Uncaught exception $error, blocking $playerName by default")
        }
        handler.disconnect(announcer.kickMessage(playerName))
      })
  }
}

@Mod(modid = "bouncer", version = "0.2.0", acceptableRemoteVersions = "*", modLanguage = "kotlin")
class Bouncer {
  data class Config(@SerializedName("secret") val secret: String,
                    @SerializedName("trusted_uuids") val trustedUsers: List<UUID>)

  private val logger = FMLLog.getLogger()

  @Mod.EventHandler
  fun onServerStart(event: FMLServerStartingEvent) {
    logger.info("Bouncer initializing...")

    val configJson = File("bouncer_config.json").readText()
    val config = gson.fromJson(configJson, Config::class.java)
    val server = event.server
    val authListener = AuthListener(
      logger = logger,
      server = server,
      trustedUsers = config.trustedUsers.toMutableSet(),
      authorizer = DggAuthorizer(secret = config.secret))

    MinecraftForge.EVENT_BUS.register(authListener)

    event.registerServerCommand(object : CommandBase() {
      override fun getName() = "bouncer_toggle"

      override fun getUsage(sender: ICommandSender?) = "$name (on|off)"

      override fun execute(server: MinecraftServer?, sender: ICommandSender?, args: Array<out String>?) {
        val arg: String = args?.getOrNull(0) ?: throw WrongUsageException(getUsage(sender))
        when (arg.toLowerCase(Locale.US)) {
          "on" -> authListener.enabled = true
          "off" -> authListener.enabled = false
          else -> throw WrongUsageException(getUsage(sender))
        }
      }
    })

    event.registerServerCommand(object : CommandBase() {
      override fun getName() = "bouncer_tps"

      override fun getUsage(sender: ICommandSender?) = name

      override fun execute(server: MinecraftServer?, sender: ICommandSender?, args: Array<out String>?) {
        server?.worldTickTimes?.forEach { worldId, tickTimes ->
          val tps = Math.min(1000.0 / (MathHelper.average(tickTimes) * 1.0E-6), 20.0)
          val message = "World ${"$worldId".padEnd(4)}$tps"
          server.broadcastChatMessage(message)
          logger.info(message)
        }
      }
    })
  }
}