/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.exploit.*
import net.ccbluex.liquidbounce.features.module.modules.misc.*
import net.ccbluex.liquidbounce.features.module.modules.movement.*
import net.ccbluex.liquidbounce.features.module.modules.player.*
import net.ccbluex.liquidbounce.features.module.modules.render.*
import net.ccbluex.liquidbounce.features.module.modules.world.*
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.*
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemAxe
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.*
import net.minecraft.potion.Potion
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MathHelper
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraft.world.WorldSettings
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.cos
import kotlin.math.sin

@ModuleInfo(name = "TestAura", spacedName = "Test Aura", description = "Automatically attacks targets around you. (Rewritten to work with Watchdog.)",
        category = ModuleCategory.COMBAT, keyBind = Keyboard.KEY_R)
class Aura : Module() {

    /*
     * Options
     */
    
    // A. Combat delay
    private val minAPS: IntegerValue = object : IntegerValue("MinAPS", 5, 1, 50) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = maxAPS.get()
            if (i < newValue) set(i)

            attackDelay = TimeUtils.randomClickDelay(this.get(), maxAPS.get())
        }
    }

    private val maxAPS: IntegerValue = object : IntegerValue("MaxAPS", 8, 1, 50) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = minAPS.get()
            if (i > newValue) set(i)

            attackDelay = TimeUtils.randomClickDelay(minAPS.get(), this.get())
        }
    }

    // B. Detect range/conditions
    val rangeValue = FloatValue("Range", 3f, 0.1f, 7f)
    private val sprintReduceRangeValue = FloatValue("SprintReduceRange", 0f, 0f, 0.4f)

    private val throughWallsValue = BoolValue("ThroughWalls", true)
    private val throughWallsRangeValue = FloatValue("ThroughWallsRange", 3f, 0f, 8f, { throughWallsValue.get() })

    private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)
    private val fovValue = FloatValue("FOV", 180f, 0f, 180f)

    // C. Target sorting
    val targetModeValue = ListValue("Mode", arrayOf("Single", "Switch", "Multi"), "Switch")
    private val switchDelayValue = IntegerValue("SwitchDelay", 1000, 1, 2000, { targetModeValue.get().equals("switch", true) })
    private val priorityValue = ListValue("Priority", arrayOf("Health", "Distance", "Direction", "LivingTime"), "Distance")
    
    // D. Bypass/Safety
    private val swingValue = BoolValue("Swing", true)
    private val keepSprintValue = BoolValue("KeepSprint", true)
    private val aacValue = BoolValue("AAC", false)
    private val noScaffValue = BoolValue("NoScaffold", true)
    private val failRateValue = FloatValue("FailRate", 0f, 0f, 100f)
    private val fakeSwingValue = BoolValue("FakeSwing", true)
    private val noInventoryAttackValue = BoolValue("NoInvAttack", false)
    private val noInventoryDelayValue = IntegerValue("NoInvDelay", 200, 0, 500, { noInventoryAttackValue.get() })
    private val limitedMultiTargetsValue = IntegerValue("LimitedMultiTargets", 0, 0, 50, { targetModeValue.get().equals("multi", true) })

    // E. Autoblock
    private val autoBlockModeValue = ListValue("AutoBlock", arrayOf("None", "Packet", "Watchdog"), "None")
    private val interactValue = BoolValue("Interact", true, { autoBlockModeValue.get().equals("Packet", true) })
    private val wallCheckValue = BoolValue("BlockWallCheck", true, { !autoBlockModeValue.get().equals("None", true) })
    private val blockRate = IntegerValue("BlockRate", 100, 1, 100, { !autoBlockModeValue.get().equals("None", true) })

    // Autoblock smart addon (Thanks to yorik)
    private val verusAutoBlockValue = BoolValue("Verus", false, { !autoBlockModeValue.get().equals("None", true) })
    private val smartAutoBlockValue = BoolValue("Smart", false, { !autoBlockModeValue.get().equals("None", true) })
    private val smartABItemValue = BoolValue("ItemCheck", true, { !autoBlockModeValue.get().equals("None", true) && smartAutoBlockValue.get() })
    private val smartABFacingValue = BoolValue("FacingCheck", true, { !autoBlockModeValue.get().equals("None", true) && smartAutoBlockValue.get() })
    private val smartABRangeValue = FloatValue("CheckRange", 3.5F, 3F, 8F, { !autoBlockModeValue.get().equals("None", true) && smartAutoBlockValue.get() })
    private val smartABTolerationValue = FloatValue("Toleration", 0F, 0F, 2F, { !autoBlockModeValue.get().equals("None", true) && smartAutoBlockValue.get() })

    // F. Raycast
    private val raycastValue = BoolValue("Raycast", true)
    private val raycastIgnoredValue = BoolValue("RaycastAll", false, { raycastValue.get() })
    private val livingRaycastValue = BoolValue("RaycastLiving", true, { raycastValue.get() })

    // G. Rotations
    private val rotations = ListValue("Rotation", arrayOf("LiquidBounce", "LiquidSense"), "LiquidSense") // TODO spin xoay vong hack
    private val alwaysHitValue = BoolValue("Always-Hit", false)
    private val silentRotationValue = BoolValue("Silent", true)

    private val minTurnSpeed: FloatValue = object : FloatValue("MinTurnSpeed", 180f, 0f, 180f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = maxTurnSpeed.get()
            if (v < newValue) set(v)
        }
    }
    private val maxTurnSpeed: FloatValue = object : FloatValue("MaxTurnSpeed", 180f, 0f, 180f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = minTurnSpeed.get()
            if (v > newValue) set(v)
        }
    }

    private val randomCenterValue = BoolValue("RandomCenter", false)
    private val randomCenterNewValue = BoolValue("NewCalc", true, { randomCenterValue.get() })
    private val minRand: FloatValue = object : FloatValue("MinMultiply", 0.8f, 0f, 2f, { randomCenterValue.get() }) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = maxRand.get()
            if (v < newValue) set(v)
        }
    }
    private val maxRand: FloatValue = object : FloatValue("MaxMultiply", 0.8f, 0f, 2f, { randomCenterValue.get() }) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = minRand.get()
            if (v > newValue) set(v)
        }
    }
    private val outborderValue = BoolValue("Outborder", false)

    // H. Visuals
    val moveMarkValue = FloatValue("MarkY", 0F, 0F, 2F)
    private val fakeSharpValue = BoolValue("FakeSharp", true)

    /**
     * Main module
     */

    // Target
    var target: EntityLivingBase? = null
    var currentTarget: EntityLivingBase? = null
    var hitable = false
    private val prevTargetEntities = mutableListOf<Int>()

    // Attack delay
    private val attackTimer = MSTimer()
    private var attackDelay = 0L

    private var lastHitTick = 0

    // Container Delay
    private var containerOpen = -1L

    // Fake block status
    var blockingStatus = false
    var fakeBlock = false

    var smartBlocking = false
    private val canSmartBlock: Boolean
        get() = !smartAutoBlockValue.get() || smartBlocking

    override fun onEnable() {
        mc.thePlayer ?: return
        mc.theWorld ?: return

        updateTarget()
        smartBlocking = false
        verusBlocking = false
    }

    override fun onDisable() {
        target = null
        currentTarget = null
        hitable = false
        prevTargetEntities.clear()
        attackTimer.reset()

        stopBlocking()
        if (verusBlocking && !blockingStatus && !mc.thePlayer.isBlocking) {
            verusBlocking = false
            if (verusAutoBlockValue.get())
                PacketUtils.sendPacketNoEvent(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
        }
    }

    
    /**
     * Motion event
     */
    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState == EventState.PRE) {
            if (autoBlockModeValue.get().equals("watchdog", true))
                stopBlocking()

            update()
        } 

        if (event.eventState == EventState.POST) {
            update()

            target ?: return
            currentTarget ?: return
            updateHitable()
            runAttackProcess()
            addonProgress()
        }
    }

    fun addonProgress() {
        smartBlocking = false
        if (smartAutoBlockValue.get() && target != null) {
            val smTarget = target!!
            if (!smartABItemValue.get() || (smTarget.heldItem != null && smTarget.heldItem.getItem() != null && (smTarget.heldItem.getItem() is ItemSword || smTarget.heldItem.getItem() is ItemAxe))) {
                if (mc.thePlayer.getDistanceToEntityBox(smTarget) < smartABRangeValue.get()) {
                    if (smartABFacingValue.get()) {
                        if (smTarget.rayTrace(smartABRangeValue.get().toDouble(), 1F).typeOfHit == MovingObjectPosition.MovingObjectType.MISS) {
                            val eyesVec = smTarget.getPositionEyes(1F)
                            val lookVec = smTarget.getLook(1F)
                            val pointingVec = eyesVec.addVector(lookVec.xCoord * smartABRangeValue.get(), lookVec.yCoord * smartABRangeValue.get(), lookVec.zCoord * smartABRangeValue.get())
                            val border = mc.thePlayer.getCollisionBorderSize() + smartABTolerationValue.get()
                            val bb = mc.thePlayer.entityBoundingBox.expand(border.toDouble(), border.toDouble(), border.toDouble())
                            smartBlocking = bb.calculateIntercept(eyesVec, pointingVec) != null || bb.intersectsWith(smTarget.entityBoundingBox)
                        }
                    } else
                        smartBlocking = true
                }
            }
        }

        if (blockingStatus || mc.thePlayer.isBlocking())
            verusBlocking = true
        else if (verusBlocking) {
            verusBlocking = false
            if (verusAutoBlockValue.get()) 
                PacketUtils.sendPacketNoEvent(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
        }
    }

    fun update() {
        if (cancelRun || (noInventoryAttackValue.get() && (mc.currentScreen is GuiContainer ||
                        System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get())))
            return

        // Update target
        updateTarget()

        if (target == null) {
            stopBlocking()
            return
        }

        // Target
        currentTarget = target

        if (!targetModeValue.get().equals("Switch", ignoreCase = true) && isEnemy(currentTarget))
            target = currentTarget
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (verusBlocking 
            && ((packet is C07PacketPlayerDigging 
                    && packet.getStatus() == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) 
                    || packet is C08PacketPlayerBlockPlacement)
            && verusAutoBlockValue.get())
            event.cancelEvent()

        if (packet is C09PacketHeldItemChange)
            verusBlocking = false
    }

    private fun runAttackProcess() {
        if (cancelRun) {
            target = null
            currentTarget = null
            hitable = false
            stopBlocking()
            return
        }

        if (noInventoryAttackValue.get() && (mc.currentScreen is GuiContainer ||
                        System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get())) {
            target = null
            currentTarget = null
            hitable = false
            if (mc.currentScreen is GuiContainer) containerOpen = System.currentTimeMillis()
            return
        }

        if (target != null && currentTarget != null) {
            while (clicks > 0) {
                runAttack()
                clicks--
            }
        }
    }

    /**
     * Render event
     */
    /*@EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (circleValue.get()) {
            GL11.glPushMatrix()
            GL11.glTranslated(
                mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * mc.timer.renderPartialTicks - mc.getRenderManager().renderPosX,
                mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * mc.timer.renderPartialTicks - mc.getRenderManager().renderPosY,
                mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * mc.timer.renderPartialTicks - mc.getRenderManager().renderPosZ
            )
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glEnable(GL11.GL_LINE_SMOOTH)
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

            GL11.glLineWidth(1F)
            GL11.glColor4f(red.get().toFloat() / 255.0F, green.get().toFloat() / 255.0F, blue.get().toFloat() / 255.0F, alpha.get().toFloat() / 255.0F)
            GL11.glRotatef(90F, 1F, 0F, 0F)
            GL11.glBegin(GL11.GL_LINE_STRIP)

            for (i in 0..360 step 60 - accuracyValue.get()) { // You can change circle accuracy  (60 - accuracy)
                GL11.glVertex2f(Math.cos(i * Math.PI / 180.0).toFloat() * rangeValue.get(), (Math.sin(i * Math.PI / 180.0).toFloat() * rangeValue.get()))
            }

            GL11.glEnd()

            GL11.glDisable(GL11.GL_BLEND)
            GL11.glEnable(GL11.GL_TEXTURE_2D)
            GL11.glEnable(GL11.GL_DEPTH_TEST)
            GL11.glDisable(GL11.GL_LINE_SMOOTH)

            GL11.glPopMatrix()
        }

        if (cancelRun) {
            target = null
            currentTarget = null
            hitable = false
            stopBlocking()
            return
        }

        if (noInventoryAttackValue.get() && (mc.currentScreen is GuiContainer ||
                        System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get())) {
            target = null
            currentTarget = null
            hitable = false
            if (mc.currentScreen is GuiContainer) containerOpen = System.currentTimeMillis()
            return
        }

        target ?: return

        if (currentTarget != null && attackTimer.hasTimePassed(attackDelay) &&
                currentTarget!!.hurtTime <= hurtTimeValue.get()) {
            clicks++
            attackTimer.reset()
            attackDelay = TimeUtils.randomClickDelay(minCPS.get(), maxCPS.get())
        }
    }*/

    /**
     * Handle entity move
     */
    @EventTarget
    fun onEntityMove(event: EntityMovementEvent) {
        val movedEntity = event.movedEntity

        if (target == null || movedEntity != currentTarget)
            return

        updateHitable()
    }

    /**
     * Attack enemy
     */
    private fun runAttack() {
        target ?: return
        currentTarget ?: return

        // Settings
        val failRate = failRateValue.get()
        val swing = swingValue.get()
        val multi = targetModeValue.get().equals("Multi", ignoreCase = true)
        val openInventory = aacValue.get() && mc.currentScreen is GuiInventory
        val failHit = failRate > 0 && Random().nextInt(100) <= failRate

        // Close inventory when open
        if (openInventory)
            mc.netHandler.addToSendQueue(C0DPacketCloseWindow())

        // Check is not hitable or check failrate
        if (!hitable || failHit) {
            if (swing && (fakeSwingValue.get() || failHit))
                mc.thePlayer.swingItem()
        } else {
            // Attack
            if (!multi) {
                attackEntity(currentTarget!!)
            } else {
                var targets = 0

                for (entity in mc.theWorld.loadedEntityList) {
                    val distance = mc.thePlayer.getDistanceToEntityBox(entity)

                    if (entity is EntityLivingBase && isEnemy(entity) && distance <= getRange(entity)) {
                        attackEntity(entity)

                        targets += 1

                        if (limitedMultiTargetsValue.get() != 0 && limitedMultiTargetsValue.get() <= targets)
                            break
                    }
                }
            }

            prevTargetEntities.add(if (aacValue.get()) target!!.entityId else currentTarget!!.entityId)

            if (target == currentTarget)
                target = null
        }

        if(targetModeValue.get().equals("Switch", ignoreCase = true) && attackTimer.hasTimePassed((switchDelayValue.get()).toLong())) {
            if(switchDelayValue.get() != 0) {
                prevTargetEntities.add(if (aacValue.get()) target!!.entityId else currentTarget!!.entityId)
                attackTimer.reset()
            }
        }

        // Open inventory
        if (openInventory)
            mc.netHandler.addToSendQueue(C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT))
    }

    /**
     * Update current target
     */
    private fun updateTarget() {
        // Reset fixed target to null
        var searchTarget = null

        // Settings
        val hurtTime = hurtTimeValue.get()
        val fov = fovValue.get()
        val switchMode = targetModeValue.get().equals("Switch", ignoreCase = true)

        // Find possible targets
        val targets = mutableListOf<EntityLivingBase>()

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity !is EntityLivingBase || !isEnemy(entity) || (switchMode && prevTargetEntities.contains(entity.entityId)))
                continue

            val distance = mc.thePlayer.getDistanceToEntityBox(entity)
            val entityFov = RotationUtils.getRotationDifference(entity)

            if (distance <= maxRange && (fov == 180F || entityFov <= fov) && entity.hurtTime <= hurtTime)
                targets.add(entity)
        }

        // Sort targets by priority
        when (priorityValue.get().toLowerCase()) {
            "distance" -> targets.sortBy { mc.thePlayer.getDistanceToEntityBox(it) } // Sort by distance
            "health" -> targets.sortBy { it.health } // Sort by health
            "direction" -> targets.sortBy { RotationUtils.getRotationDifference(it) } // Sort by FOV
            "livingtime" -> targets.sortBy { -it.ticksExisted } // Sort by existence
        }

        var found = false

        // Find best target
        for (entity in targets) {
            // Update rotations to current target
            if (!updateRotations(entity)) // when failed then try another target
                continue

            // Set target to current entity
            target = entity
            found = true
            break
        }

        if (found) {
            if (rotations.get().equals("spin", true)) {
                spinYaw += RandomUtils.nextFloat(minSpinSpeed.get(), maxSpinSpeed.get())
                spinYaw = MathHelper.wrapAngleTo180_float(spinYaw)
                val rot = Rotation(spinYaw, 90F)
                RotationUtils.setTargetRotation(rot, 0)
            }
            return
        }

        if (searchTarget != null) {
            if (target != searchTarget) target = searchTarget
            return
        } else {
            target = null
        }

        // Cleanup last targets when no target found and try again
        if (prevTargetEntities.isNotEmpty()) {
            prevTargetEntities.clear()
            updateTarget()
        }
    }

    /**
     * Check if [entity] is selected as enemy with current target options and other modules
     */
    public fun isEnemy(entity: Entity?): Boolean {
        if (entity is EntityLivingBase && (EntityUtils.targetDead || isAlive(entity)) && entity != mc.thePlayer) {
            if (!EntityUtils.targetInvisible && entity.isInvisible())
                return false

            if (EntityUtils.targetPlayer && entity is EntityPlayer) {
                if (entity.isSpectator || AntiBot.isBot(entity))
                    return false

                if (EntityUtils.isFriend(entity) && !LiquidBounce.moduleManager[NoFriends::class.java]!!.state)
                    return false

                val teams = LiquidBounce.moduleManager[Teams::class.java] as Teams

                return !teams.state || !teams.isInYourTeam(entity)
            }

            return EntityUtils.targetMobs && EntityUtils.isMob(entity) || EntityUtils.targetAnimals &&
                    EntityUtils.isAnimal(entity)
        }

        return false
    }

    /**
     * Attack [entity]
     */
    private fun attackEntity(entity: EntityLivingBase) {
        // Stop blocking
        if (mc.thePlayer.isBlocking || blockingStatus) {
            stopBlocking()
        }

        // Call attack event
        LiquidBounce.eventManager.callEvent(AttackEvent(entity))

        markEntity = entity
            
        // Get rotation and send packet if possible
        if (rotations.get().equals("spin", true))
        {
            val targetedRotation = getTargetRotation(entity) ?: return
            mc.netHandler.addToSendQueue(C03PacketPlayer.C05PacketPlayerLook(targetedRotation.yaw, targetedRotation.pitch, mc.thePlayer.onGround))

            if (debugValue.get())
                ClientUtils.displayChatMessage("[KillAura] Silent rotation change.")
        }

        // Attack target
        if (swingValue.get())
            mc.thePlayer.swingItem()

        mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))

        if (keepSprintValue.get()) {
            // Critical Effect
            if (mc.thePlayer.fallDistance > 0F && !mc.thePlayer.onGround && !mc.thePlayer.isOnLadder &&
                    !mc.thePlayer.isInWater && !mc.thePlayer.isPotionActive(Potion.blindness) && !mc.thePlayer.isRiding)
                mc.thePlayer.onCriticalHit(entity)

            // Enchant Effect
            if (EnchantmentHelper.getModifierForCreature(mc.thePlayer.heldItem, entity.creatureAttribute) > 0F)
                mc.thePlayer.onEnchantmentCritical(entity)
        } else {
            if (mc.playerController.currentGameType != WorldSettings.GameType.SPECTATOR)
                mc.thePlayer.attackTargetEntityWithCurrentItem(entity)
        }

        // Extra critical effects
        val criticals = LiquidBounce.moduleManager[Criticals::class.java] as Criticals

        for (i in 0..2) {
            // Critical Effect
            if (mc.thePlayer.fallDistance > 0F && !mc.thePlayer.onGround && !mc.thePlayer.isOnLadder && !mc.thePlayer.isInWater && !mc.thePlayer.isPotionActive(Potion.blindness) && mc.thePlayer.ridingEntity == null || criticals.state && criticals.msTimer.hasTimePassed(criticals.delayValue.get().toLong()) && !mc.thePlayer.isInWater && !mc.thePlayer.isInLava && !mc.thePlayer.isInWeb)
                mc.thePlayer.onCriticalHit(target)

            // Enchant Effect
            if (EnchantmentHelper.getModifierForCreature(mc.thePlayer.heldItem, target!!.creatureAttribute) > 0.0f || fakeSharpValue.get())
                mc.thePlayer.onEnchantmentCritical(target)
        }

        // Start blocking after attack
        if ((!afterTickPatchValue.get() || !autoBlockModeValue.get().equals("AfterTick", true)) && (mc.thePlayer.isBlocking || canBlock)) 
            startBlocking(entity, interactAutoBlockValue.get())
    }

    /**
     * Update killaura rotations to enemy
     */
    private fun updateRotations(entity: Entity): Boolean {
        val disabler = LiquidBounce.moduleManager.getModule(Disabler::class.java)!! as Disabler
        val modify = disabler.canModifyRotation

        if (modify) return true // just ignore then

        var defRotation = getTargetRotation(entity) ?: return false

        if (silentRotationValue.get()) {
            RotationUtils.setTargetRotation(defRotation, if (aacValue.get() && !rotations.get().equals("Spin", ignoreCase = true)) 15 else 0)
        } else {
            defRotation.toPlayer(mc.thePlayer!!)
        }

        return true
    }

    private fun getTargetRotation(entity: Entity): Rotation? {
        var boundingBox = entity.entityBoundingBox
        if (rotations.get().equals("Vanilla", ignoreCase = true)){
            if (maxTurnSpeed.get() <= 0F)
                return RotationUtils.serverRotation

            if (predictValue.get())
                boundingBox = boundingBox.offset(
                        (entity.posX - entity.prevPosX) * RandomUtils.nextFloat(minPredictSize.get(), maxPredictSize.get()),
                        (entity.posY - entity.prevPosY) * RandomUtils.nextFloat(minPredictSize.get(), maxPredictSize.get()),
                        (entity.posZ - entity.prevPosZ) * RandomUtils.nextFloat(minPredictSize.get(), maxPredictSize.get())
                )

            val (_, rotation) = RotationUtils.searchCenter(
                    boundingBox,
                    outborderValue.get() && !attackTimer.hasTimePassed(attackDelay / 2),
                    randomCenterValue.get(),
                    predictValue.get(),
                    mc.thePlayer!!.getDistanceToEntityBox(entity) < throughWallsRangeValue.get(),
                    maxRange,
                    RandomUtils.nextFloat(minRand.get(), maxRand.get()),
                    randomCenterNewValue.get()
            ) ?: return null

            val limitedRotation = RotationUtils.limitAngleChange(RotationUtils.serverRotation, rotation,
                    (Math.random() * (maxTurnSpeed.get() - minTurnSpeed.get()) + minTurnSpeed.get()).toFloat())

            return limitedRotation
        }
        if (rotations.get().equals("Spin", ignoreCase = true)){
            if (maxTurnSpeed.get() <= 0F)
                return RotationUtils.serverRotation

            if (predictValue.get())
                boundingBox = boundingBox.offset(
                        (entity.posX - entity.prevPosX) * RandomUtils.nextFloat(minPredictSize.get(), maxPredictSize.get()),
                        (entity.posY - entity.prevPosY) * RandomUtils.nextFloat(minPredictSize.get(), maxPredictSize.get()),
                        (entity.posZ - entity.prevPosZ) * RandomUtils.nextFloat(minPredictSize.get(), maxPredictSize.get())
                )

            val (_, rotation) = RotationUtils.searchCenter(
                    boundingBox,
                    false,
                    false,
                    false,
                    mc.thePlayer!!.getDistanceToEntityBox(entity) < throughWallsRangeValue.get(),
                    maxRange
            ) ?: return null

            return rotation
        }
        if (rotations.get().equals("BackTrack", ignoreCase = true)) {
            if (predictValue.get())
                boundingBox = boundingBox.offset(
                        (entity.posX - entity.prevPosX) * RandomUtils.nextFloat(minPredictSize.get(), maxPredictSize.get()),
                        (entity.posY - entity.prevPosY) * RandomUtils.nextFloat(minPredictSize.get(), maxPredictSize.get()),
                        (entity.posZ - entity.prevPosZ) * RandomUtils.nextFloat(minPredictSize.get(), maxPredictSize.get())
                )

            val limitedRotation = RotationUtils.limitAngleChange(RotationUtils.serverRotation,
                    RotationUtils.OtherRotation(boundingBox,RotationUtils.getCenter(entity.entityBoundingBox), predictValue.get(),
                            mc.thePlayer!!.getDistanceToEntityBox(entity) < throughWallsRangeValue.get(),maxRange), (Math.random() * (maxTurnSpeed.get() - minTurnSpeed.get()) + minTurnSpeed.get()).toFloat())

            return limitedRotation
        }
        return RotationUtils.serverRotation
    }

    /**
     * Check if enemy is hitable with current rotations
     */
    private fun updateHitable() {
        val disabler = LiquidBounce.moduleManager.getModule(Disabler::class.java)!! as Disabler

        // Modify hit check for some situations
        if (rotations.get().equals("spin", true)) {
            hitable = target!!.hurtTime <= 1
            return
        }

        // Completely disable rotation check if turn speed equals to 0 or NoHitCheck is enabled
        if(maxTurnSpeed.get() <= 0F || noHitCheck.get() || disabler.canModifyRotation) {
            hitable = true
            return
        }

        val reach = min(maxRange.toDouble(), mc.thePlayer.getDistanceToEntityBox(target!!)) + 1

        if (raycastValue.get()) {
            val raycastedEntity = RaycastUtils.raycastEntity(reach) {
                (!livingRaycastValue.get() || it is EntityLivingBase && it !is EntityArmorStand) &&
                        (isEnemy(it) || raycastIgnoredValue.get() || aacValue.get() && mc.theWorld.getEntitiesWithinAABBExcludingEntity(it, it.entityBoundingBox).isNotEmpty())
            }

            if (raycastValue.get() && raycastedEntity is EntityLivingBase
                    && (LiquidBounce.moduleManager[NoFriends::class.java]!!.state || !EntityUtils.isFriend(raycastedEntity)))
                currentTarget = raycastedEntity

            hitable = if(maxTurnSpeed.get() > 0F) currentTarget == raycastedEntity else true
        } else
            hitable = RotationUtils.isFaced(currentTarget, reach)
    }

    /**
     * Start blocking
     */


    private fun startBlocking(interactEntity: Entity, interact: Boolean) {
        if (!canSmartBlock || autoBlockModeValue.get().equals("none", true) || !(blockRate.get() > 0 && Random().nextInt(100) <= blockRate.get()))
            return

        if (!abThruWallValue.get() && interactEntity is EntityLivingBase) {
            val entityLB = interactEntity as EntityLivingBase
            if (!entityLB.canEntityBeSeen(mc.thePlayer!!)) {
                fakeBlock = true
                return
            }
        }

        if (autoBlockModeValue.get().equals("ncp", true)) {
            mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, null, 0.0f, 0.0f, 0.0f))
            blockingStatus = true
            return
        }

        if (autoBlockModeValue.get().equals("oldhypixel", true) || autoBlockModeValue.get().equals("watchdog", true)) {
            mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, mc.thePlayer.inventory.getCurrentItem(), 0.0f, 0.0f, 0.0f))
            blockingStatus = true
            return
        }

        if (interact) {
            //mc.netHandler.addToSendQueue(C02PacketUseEntity(interactEntity, interactEntity.positionVector))
            val positionEye = mc.renderViewEntity?.getPositionEyes(1F)

            val expandSize = interactEntity.collisionBorderSize.toDouble()
            val boundingBox = interactEntity.entityBoundingBox.expand(expandSize, expandSize, expandSize)

            val (yaw, pitch) = RotationUtils.targetRotation ?: Rotation(mc.thePlayer!!.rotationYaw, mc.thePlayer!!.rotationPitch)
            val yawCos = cos(-yaw * 0.017453292F - Math.PI.toFloat())
            val yawSin = sin(-yaw * 0.017453292F - Math.PI.toFloat())
            val pitchCos = -cos(-pitch * 0.017453292F)
            val pitchSin = sin(-pitch * 0.017453292F)
            val range = min(maxRange.toDouble(), mc.thePlayer!!.getDistanceToEntityBox(interactEntity)) + 1
            val lookAt = positionEye!!.addVector(yawSin * pitchCos * range, pitchSin * range, yawCos * pitchCos * range)

            val movingObject = boundingBox.calculateIntercept(positionEye, lookAt) ?: return
            val hitVec = movingObject.hitVec

            mc.netHandler.addToSendQueue(C02PacketUseEntity(interactEntity, Vec3(
                    hitVec.xCoord - interactEntity.posX,
                    hitVec.yCoord - interactEntity.posY,
                    hitVec.zCoord - interactEntity.posZ)
            ))
            mc.netHandler.addToSendQueue(C02PacketUseEntity(interactEntity, C02PacketUseEntity.Action.INTERACT))
        }

        mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()))
        blockingStatus = true
    }

    /**
     * Stop blocking
     */
    private fun stopBlocking() {
        fakeBlock = false

        if (blockingStatus) {
            if (autoBlockModeValue.get().equals("oldhypixel", true))
                mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos(1.0, 1.0, 1.0), EnumFacing.DOWN))
            else
                mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
            
            blockingStatus = false
        }
    }

    /**
     * Check if run should be cancelled
     */
    private val cancelRun: Boolean
        get() = mc.thePlayer.isSpectator || !isAlive(mc.thePlayer)
                || LiquidBounce.moduleManager[Blink::class.java]!!.state || LiquidBounce.moduleManager[FreeCam::class.java]!!.state || 
                (noScaffValue.get() && LiquidBounce.moduleManager[Scaffold::class.java]!!.state)

    /**
     * Check if [entity] is alive
     */
    private fun isAlive(entity: EntityLivingBase) = entity.isEntityAlive && entity.health > 0 ||
            aacValue.get() && entity.hurtTime > 5


    /**
     * Check if player is able to block
     */
    private val canBlock: Boolean
        get() = mc.thePlayer.heldItem != null && mc.thePlayer.heldItem.item is ItemSword

    /**
     * Range
     */
    private val maxRange: Float
        get() = max(rangeValue.get(), throughWallsRangeValue.get())

    private fun getRange(entity: Entity) =
            (if (mc.thePlayer.getDistanceToEntityBox(entity) >= throughWallsRangeValue.get()) rangeValue.get() else throughWallsRangeValue.get()) - if (mc.thePlayer.isSprinting) rangeSprintReducementValue.get() else 0F

    /**
     * HUD Tag
     */
    override val tag: String?
        get() = targetModeValue.get()
   
}