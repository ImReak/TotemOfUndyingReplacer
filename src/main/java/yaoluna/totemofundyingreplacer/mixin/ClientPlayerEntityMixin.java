package yaoluna.totemofundyingreplacer.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    private static final int OFFHAND_INV_INDEX = 40;

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void onTickMovement(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        ClientPlayerInteractionManager im = client.interactionManager;
        if (client.player == null || im == null ||
                player.currentScreenHandler != player.playerScreenHandler ||
                !player.playerScreenHandler.getCursorStack().isEmpty() ||
                !shouldEquipTotem(player)) {
            return;
        }

        tryMoveTotemToOffhand(player, im);
    }

    private boolean shouldEquipTotem(ClientPlayerEntity player) {
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        if (health > (maxHealth / 2.0f)) return false;
        ItemStack offhand = player.getOffHandStack();
        if (offhand.isOf(Items.TOTEM_OF_UNDYING)) return false;
        return true;
    }

    private void tryMoveTotemToOffhand(ClientPlayerEntity player, ClientPlayerInteractionManager im) {
        ScreenHandler sh = player.playerScreenHandler;
        int offhandSlotId = findSlotIdByInventoryIndex(sh, player, OFFHAND_INV_INDEX);
        if (offhandSlotId == -1) return;
        int totemSlotId = findFirstTotemSlotId(sh, player);
        if (totemSlotId == -1) return;
        click(im, sh.syncId, totemSlotId);
        click(im, sh.syncId, offhandSlotId);
        click(im, sh.syncId, totemSlotId);
    }

    private void click(ClientPlayerInteractionManager im, int syncId, int slotId) {
        im.clickSlot(syncId, slotId, 0, SlotActionType.PICKUP, MinecraftClient.getInstance().player);
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