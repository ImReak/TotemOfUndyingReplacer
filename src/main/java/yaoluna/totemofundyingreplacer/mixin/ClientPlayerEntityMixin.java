package yaoluna.totemofundyingreplacer.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yaoluna.totemofundyingreplacer.TotemOfUndyingReplacerConfig;
import yaoluna.totemofundyingreplacer.TotemOfUndyingReplacerConfigManager;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Shadow
    @Final
    protected MinecraftClient client;
    private static final int OFFHAND_INV_INDEX = 40;

    private int cooldown = 0;

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void onTickMovement(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        TotemOfUndyingReplacerConfig cfg = TotemOfUndyingReplacerConfigManager.get();

        if (!canTryEquipTotem(client, player, cfg)) return;

        tryMoveTotemToOffhand(player, client, OFFHAND_INV_INDEX);
        cooldown = cfg.cooldownTicks;
    }

    private boolean canTryEquipTotem(
            MinecraftClient client,
            ClientPlayerEntity player,
            TotemOfUndyingReplacerConfig cfg
    ) {
        if (client.player == null || client.interactionManager == null) return false;
        if (!isSurvivalLike(client)) return false;

        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        if (player.currentScreenHandler != player.playerScreenHandler) return false;
        if (!player.playerScreenHandler.getCursorStack().isEmpty()) return false;
        if (player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return false;

        return shouldEquipTotem(player, cfg);
    }

    private boolean isSurvivalLike(MinecraftClient client) {
        ClientPlayerInteractionManager im = client.interactionManager;
        if (im == null) return false;

        GameMode gm = im.getCurrentGameMode();
        return gm == GameMode.SURVIVAL || gm == GameMode.ADVENTURE;
    }

    private boolean shouldEquipTotem(ClientPlayerEntity player, TotemOfUndyingReplacerConfig cfg) {

        float hp = player.getHealth();
        float max = player.getMaxHealth();
        boolean lowHealth = hp <= (float)(max * cfg.healthThresholdPercent);

        float effectiveHp = hp + player.getAbsorptionAmount();

        boolean fireLavaRisk = false;
        if (cfg.enableFireLava) {
            if (!player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                boolean onFire = player.isOnFire();
                boolean inLava = player.isInLava();
                fireLavaRisk = onFire || inLava;
            }
        }

        boolean fallRisk = false;
        if (cfg.enableFall) {
            if (!player.isOnGround() && player.fallDistance >= (float) cfg.fallDistanceThreshold) {
                fallRisk = true;
            }

            float predicted = (float) Math.max(0.0f, player.fallDistance - 3.0f) * 0.5f;
            if (!player.isOnGround() && predicted >= effectiveHp - 1.0f) {
                fallRisk = true;
            }
        }

        boolean explosionRisk = false;
        if (cfg.enableExplosions) {
            explosionRisk = detectExplosionRisk(player, cfg);
        }

        return lowHealth || fireLavaRisk || fallRisk || explosionRisk;
    }

    private boolean detectExplosionRisk(ClientPlayerEntity player, TotemOfUndyingReplacerConfig cfg) {

        boolean shieldUp = isShieldRaised(player);
        double r = cfg.explosionScanRadius;
        double r2 = r * r;

        if (client.world == null) return false;

        for (Entity e : client.world.getEntities()) {
            if (e.squaredDistanceTo(player) > r2) continue;
            if (e instanceof TntEntity tnt) {
                if (tnt.getFuse() <= cfg.explosionFuseTicksThreshold) {
                    if (!shieldUp) return true;
                }
            }
            if (e instanceof CreeperEntity creeper) {
                boolean ignited = creeper.isIgnited();
                int fuseSpeed = creeper.getFuseSpeed();
                if (ignited || fuseSpeed > 0) {
                    if (!shieldUp) return true;
                }
            }

            if (e instanceof EndCrystalEntity) {
                return true;
            }
        }

        return false;
    }

    private boolean isShieldRaised(ClientPlayerEntity player) {
        return player.isUsingItem() && player.getActiveItem().isOf(Items.SHIELD);
    }


    private void tryMoveTotemToOffhand(ClientPlayerEntity player, MinecraftClient client, int offhandInvIndex) {
        ScreenHandler sh = player.playerScreenHandler;
        int offhandSlotId = findSlotIdByInventoryIndex(sh, player, offhandInvIndex);
        if (offhandSlotId == -1) return;
        int totemSlotId = findFirstTotemSlotId(sh, player);
        if (totemSlotId == -1) return;
        click(client, sh.syncId, totemSlotId);
        click(client, sh.syncId, offhandSlotId);
        click(client, sh.syncId, totemSlotId);
    }

    private void click(MinecraftClient client, int syncId, int slotId) {
        client.interactionManager.clickSlot(syncId, slotId, 0, SlotActionType.PICKUP, client.player);
    }

    private int findSlotIdByInventoryIndex(ScreenHandler sh, ClientPlayerEntity player, int invIndex) {
        for (int slotId = 0; slotId < sh.slots.size(); slotId++) {
            Slot s = sh.slots.get(slotId);
            if (s.inventory == player.getInventory() && s.getIndex() == invIndex) {
                return slotId;
            }
        }
        return -1;
    }

    private int findFirstTotemSlotId(ScreenHandler sh, ClientPlayerEntity player) {
        for (int slotId = 0; slotId < sh.slots.size(); slotId++) {
            Slot s = sh.slots.get(slotId);
            if (s.inventory != player.getInventory()) continue;

            int idx = s.getIndex();
            if (idx < 0 || idx > 35) continue;

            ItemStack st = s.getStack();
            if (!st.isEmpty() && st.isOf(Items.TOTEM_OF_UNDYING)) {
                return slotId;
            }
        }
        return -1;
    }
}