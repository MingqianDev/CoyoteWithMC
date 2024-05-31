package dev.mingqian.coyotewithmc.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBinding {
    public static final String KEY_CATEGORY_COYOTE = "key.category.CoyoteSocketControl.control";
    public static final String KEY_OPEN_GUI = "key.CoyoteSocketControl.open_gui";

    public static final KeyMapping GUI_KEY = new KeyMapping(KEY_OPEN_GUI, KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, KEY_CATEGORY_COYOTE);
}
