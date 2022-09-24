/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import org.lwjgl.input.Keyboard

class BindCommand : Command("绑定", emptyArray()) {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 2) {
            // Get module by name
            val module = LiquidBounce.moduleManager.getModule("ClickGui")

            if (module == null) 
                return
            }
            // Find key by name and change
            val key = Keyboard.getKeyIndex(args[1].toUpperCase())
            module.keyBind = key

            // Response to user
            chat("已将ClickGui绑定至${Keyboard.getKeyName(key)}.")
            LiquidBounce.hud.addNotification(Notification("已将ClickGui绑定至${Keyboard.getKeyName(key)}", Notification.Type.SUCCESS))
            playEdit()
            return
        }

        chatSyntax(arrayOf("<按键>", "none"))
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        val moduleName = args[0]

        return when (args.size) {
            1 -> LiquidBounce.moduleManager.modules
                    .map { it.name }
                    .filter { it.startsWith(moduleName, true) }
                    .toList()
            else -> emptyList()
        }
    }
}
