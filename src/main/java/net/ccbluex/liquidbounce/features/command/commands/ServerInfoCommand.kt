/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.command.Command
import net.minecraft.network.handshake.client.C00Handshake

class ServerInfoCommand : Command("serverinfo", emptyArray()), Listenable {
    init {
        LiquidBounce.eventManager.registerListener(this)
    }

    private var ip = ""
    private var port = 0

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (mc.currentServerData == null) {
            chat("此命令无法在 单人游戏 中使用.")
            return
        }

        val data = mc.currentServerData

        chat("服务器信息:")
        chat("§7名字: §8${data.serverName}")
        chat("§7IP: §8$ip:$port")
        chat("§7玩家: §8${data.populationInfo}")
        chat("§7MOTD: §8${data.serverMOTD}")
        chat("§7服务器版本: §8${data.gameVersion}")
        chat("§7Protocol版本: §8${data.version}")
        chat("§7延迟: §8${data.pingToServer}")
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is C00Handshake) {
            ip = packet.ip
            port = packet.port
        }
    }

    override fun handleEvents() = true
}
