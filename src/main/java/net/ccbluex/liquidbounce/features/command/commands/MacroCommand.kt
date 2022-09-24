/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.special.MacroManager
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import org.lwjgl.input.Keyboard

class MacroCommand : Command("宏", emptyArray()) {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 2) {
            val key = Keyboard.getKeyIndex(args[2].toUpperCase())
            if (key == 0) {
                chat("§c不存在或不被允许的键盘绑定.")
                chatSyntax("宏 <列表/清空/添加/删除>")
                return
            }
            when (args[1].toLowerCase()) {
                "添加" -> {
                    if (args.size < 4) {
                        chatSyntax("宏 添加 <按键> <消息>")
                        return
                    }
                    val message = StringUtils.toCompleteString(args, 3)
                    val existed = MacroManager.macroMapping.containsKey(key)
                    MacroManager.addMacro(key, message)
                    LiquidBounce.fileManager.saveConfig(LiquidBounce.fileManager.valuesConfig)
                    if (existed)
                        chat("§7成功更改位于 §f${Keyboard.getKeyName(key)} §7的宏为 §f$message§7.")
                    else
                        chat("§7成功将 §f$message §7绑定至 §f${Keyboard.getKeyName(key)}§7.")
                    playEdit()
                    return
                }
                "remove" -> {
                    if (MacroManager.macroMapping.containsKey(key)) {
                        val lastMessage = MacroManager.macroMapping[key]
                        MacroManager.removeMacro(key)
                        LiquidBounce.fileManager.saveConfig(LiquidBounce.fileManager.valuesConfig)
                        chat("§7成功删除了位于 §f${Keyboard.getKeyName(key)} §7的宏.")
                        playEdit()
                        return
                    }
                    chat("§c没有宏绑定在这个按键上.")
                    chatSyntax("宏 删除 <按键>")
                    return
                }
            }
        }
        if (args.size == 2) {
            when (args[1].toLowerCase()) {
                "list" -> {
                    chat("§7宏:")
                    MacroManager.macroMapping.forEach {
                        ClientUtils.displayChatMessage("§7> §f${Keyboard.getKeyName(it.key)}: §f${it.value}")
                    }
                    return
                }
                "clear" -> {
                    MacroManager.macroMapping.clear()
                    playEdit()
                    LiquidBounce.fileManager.saveConfig(LiquidBounce.fileManager.valuesConfig)
                    chat("§7成功清除了宏列表.")
                    return
                }
                "add" -> {
                    chatSyntax("宏 添加 <按键> <消息>")
                    return
                }
                "remove" -> {
                    chatSyntax("宏 删除 <按键>")
                    return
                }
            }
        }

        chatSyntax("宏 <列表/清空/添加/删除>")
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> listOf("添加", "删除", "列表", "清空")
                .filter { it.startsWith(args[0], true) }
            else -> emptyList()
        }
    }
}
