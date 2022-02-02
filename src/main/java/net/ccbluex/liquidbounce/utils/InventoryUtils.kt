/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 *
 * This part is taken from UnlegitMC/FDPClient. Please credit them when using this in your repository.
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.event.ClickWindowEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemPotion
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.client.C16PacketClientStatus
import net.minecraft.potion.Potion

object InventoryUtils : MinecraftInstance(), Listenable {
    val CLICK_TIMER = MSTimer()
    val INV_TIMER = MSTimer()
    val BLOCK_BLACKLIST = listOf(Blocks.enchanting_table, 
            Blocks.chest, 
            Blocks.ender_chest, 
            Blocks.trapped_chest,
            Blocks.anvil, 
            Blocks.sand, 
            Blocks.web, 
            Blocks.torch, 
            Blocks.crafting_table, 
            Blocks.furnace, 
            Blocks.waterlily, 
            Blocks.dispenser,
            Blocks.stone_pressure_plate, 
            Blocks.wooden_pressure_plate, 
            Blocks.noteblock, 
            Blocks.dropper, 
            Blocks.tnt, 
            Blocks.standing_banner, 
            Blocks.wall_banner,
            Blocks.redstone_torch,
            // recently added
            Blocks.gravel,
            Blocks.cactus,
            Blocks.bed,
            Blocks.lever,
            Blocks.standing_sign,
            Blocks.wall_sign,
            Blocks.jukebox,
            Blocks.oak_fence,
            Blocks.spruce_fence,
            Blocks.birch_fence,
            Blocks.jungle_fence,
            Blocks.dark_oak_fence,
            Blocks.oak_fence_gate,
            Blocks.spruce_fence_gate,
            Blocks.birch_fence_gate,
            Blocks.jungle_fence_gate,
            Blocks.dark_oak_fence_gate,
            Blocks.nether_brick_fence,
            //Blocks.cake,
            Blocks.trapdoor,
            Blocks.melon_block,
            Blocks.brewing_stand,
            Blocks.cauldron,
            Blocks.skull,
            Blocks.hopper,
            Blocks.carpet,
            Blocks.redstone_wire,
            Blocks.light_weighted_pressure_plate,
            Blocks.heavy_weighted_pressure_plate,
            Blocks.daylight_detector)

    fun findItem(startSlot: Int, endSlot: Int, item: Item): Int {
        for (i in startSlot until endSlot) {
            val stack = mc.thePlayer.inventoryContainer.getSlot(i).stack
            if (stack != null && stack.item === item) {
                return i
            }
        }
        return -1
    }

    fun hasSpaceHotbar(): Boolean {
        for (i in 36..44) {
            mc.thePlayer.inventoryContainer.getSlot(i).stack ?: return true
        }
        return false
    }

    fun findAutoBlockBlock(): Int {
        for (i in 36..44) {
            val itemStack = mc.thePlayer.inventoryContainer.getSlot(i).stack
            if (itemStack != null && itemStack.item is ItemBlock) {
                val itemBlock = itemStack.item as ItemBlock
                val block = itemBlock.getBlock()
                if (canPlaceBlock(block) && itemStack.stackSize > 0) {
                    return i
                }
            }
        }
        return -1
    }

    fun canPlaceBlock(block: Block): Boolean {
        return block.isFullCube && !BLOCK_BLACKLIST.contains(block)
    }

    fun isBlockListBlock(itemBlock: ItemBlock): Boolean {
        val block = itemBlock.getBlock()
        return BLOCK_BLACKLIST.contains(block) || !block.isFullCube
    }

    @EventTarget
    fun onClick(event: ClickWindowEvent) {
        CLICK_TIMER.reset()
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is C0EPacketClickWindow || packet is C08PacketPlayerBlockPlacement) {
            INV_TIMER.reset()
        }
        if (packet is C08PacketPlayerBlockPlacement) {
            CLICK_TIMER.reset()
        }
    }

    fun openPacket() {
        mc.netHandler.addToSendQueue(C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT))
    }

    fun closePacket() {
        mc.netHandler.addToSendQueue(C0DPacketCloseWindow())
    }

    fun isPositivePotionEffect(id: Int): Boolean {
        if (id == Potion.regeneration.id || id == Potion.moveSpeed.id ||
            id == Potion.heal.id || id == Potion.nightVision.id ||
            id == Potion.jump.id || id == Potion.invisibility.id ||
            id == Potion.resistance.id || id == Potion.waterBreathing.id ||
            id == Potion.absorption.id || id == Potion.digSpeed.id ||
            id == Potion.damageBoost.id || id == Potion.healthBoost.id ||
            id == Potion.fireResistance.id) {
            return true
        }
        return false
    }

    fun isPositivePotion(item: ItemPotion, stack: ItemStack): Boolean {
        item.getEffects(stack).forEach {
            if (isPositivePotionEffect(it.potionID)) {
                return true
            }
        }

        return false
    }

    fun getItemDurability(stack: ItemStack): Float {
        if (stack.isItemStackDamageable && stack.maxDamage> 0) {
            return (stack.maxDamage - stack.itemDamage) / stack.maxDamage.toFloat()
        }
        return 1f
    }

    override fun handleEvents() = true
}