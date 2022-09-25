
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.EntityUtils

class TargetCommand : Command("目标", emptyArray()) {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            when {
                args[1].equals("玩家", ignoreCase = true) -> {
                    EntityUtils.targetPlayer = !EntityUtils.targetPlayer
                    chat("§7已${if (EntityUtils.targetPlayer) "设置" else "取消"}玩家目标.")
                    playEdit()
                    return
                }

                args[1].equals("敌对", ignoreCase = true) -> {
                    EntityUtils.targetMobs = !EntityUtils.targetMobs
                    chat("§7已${if (EntityUtils.targetMobs) "设置" else "取消"}敌对生物目标.")
                    playEdit()
                    return
                }

                args[1].equals("友好", ignoreCase = true) -> {
                    EntityUtils.targetAnimals = !EntityUtils.targetAnimals
                    chat("§7已${if (EntityUtils.targetAnimals) "设置" else "取消"}友好生物目标.")
                    playEdit()
                    return
                }

                args[1].equals("隐身", ignoreCase = true) -> {
                    EntityUtils.targetInvisible = !EntityUtils.targetInvisible
                    chat("§7已${if (EntityUtils.targetInvisible) "设置" else "取消"}隐身玩家目标.")
                    playEdit()
                    return
                }
            }
        }

        chatSyntax("目标 <玩家/敌对/友好/隐身>")
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> listOf("玩家", "敌对", "友好", "隐身")
                .filter { it.startsWith(args[0], true) }
            else -> emptyList()
        }
    }
}
