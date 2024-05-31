package dev.mingqian.coyotewithmc.event;

import dev.mingqian.coyotewithmc.CoyoteSocketControl;
import dev.mingqian.coyotewithmc.SocketConnection;
import dev.mingqian.coyotewithmc.util.KeyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class ClientEvents {
    @Mod.EventBusSubscriber(modid = CoyoteSocketControl.MOD_ID, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (KeyBinding.GUI_KEY.consumeClick()) {

                if (SocketConnection.clientId.isEmpty()) {
                    SocketConnection.connectServer();

                } else {
                    Minecraft.getInstance().player.sendSystemMessage(Component.literal("already connected!"));
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = CoyoteSocketControl.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModBusEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(KeyBinding.GUI_KEY);
        }

    }
}
