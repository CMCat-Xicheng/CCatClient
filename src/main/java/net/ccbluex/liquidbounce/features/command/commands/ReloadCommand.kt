/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.NewUi
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.misc.sound.TipSoundManager

class ReloadCommand : Command("重载", arrayOf("reload")) {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        chat("重载中")
        chat("§7重载命令...")
        LiquidBounce.commandManager = CommandManager()
        LiquidBounce.commandManager.registerCommands()
        LiquidBounce.isStarting = true
        LiquidBounce.scriptManager.disableScripts()
        LiquidBounce.scriptManager.unloadScripts()
        for(module in LiquidBounce.moduleManager.modules)
            LiquidBounce.moduleManager.generateCommand(module)
        chat("§7重载脚本...")
        LiquidBounce.scriptManager.loadScripts()
        LiquidBounce.scriptManager.enableScripts()
        chat("§7重载字体...")
        Fonts.loadFonts()
        chat("§7重载模块...")
        LiquidBounce.fileManager.loadConfig(LiquidBounce.fileManager.modulesConfig)
        LiquidBounce.isStarting = false
        chat("§7重载值...")
        LiquidBounce.fileManager.loadConfig(LiquidBounce.fileManager.valuesConfig)
        chat("§7重载账户...")
        LiquidBounce.fileManager.loadConfig(LiquidBounce.fileManager.accountsConfig)
        chat("§7重载HUD...")
        LiquidBounce.fileManager.loadConfig(LiquidBounce.fileManager.hudConfig)
        chat("已重载")
    }
}
