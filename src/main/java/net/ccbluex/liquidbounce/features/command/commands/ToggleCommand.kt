/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command

class ToggleCommand : Command("控制", arrayOf("t")) {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            val module = LiquidBounce.moduleManager.getModule(args[1])

            if (module == null) {
                chat("未找到 '${args[1]}'.")
                return
            }

            if (args.size > 2) {
                val newState = args[2].toLowerCase();

                if (newState == "on" || newState == "off") {
                    module.state = newState == "on"
                } else {
                    chatSyntax("控制 <模块> [on/off]")
                    return
                }
            } else {
                module.toggle()
            }

            chat(" 模块 §f${module.name}§7 已设置为 ${if (module.state) "开启状态" else "关闭状态"}.")
            return
        }

        chatSyntax("控制 <模块> [on/off]")
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
