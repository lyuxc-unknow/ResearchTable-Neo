package snownee.researchtable.client.gui;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Button widget that blits one of three full-size textures (normal / hovered / disabled)
 * stretched to the button bounds, and renders the message text centered on top.
 *
 * <p>Place PNG files at e.g.
 *   {@code assets/researchtable/textures/gui/button_normal.png}
 *   {@code assets/researchtable/textures/gui/button_hovered.png}
 *   {@code assets/researchtable/textures/gui/button_disabled.png}
 *
 * <p>If {@code hovered} / {@code disabled} are {@code null} the button falls back to the
 * {@code normal} texture so you only need a single PNG if you want a flat look.
 */
public class TexturedButton extends Button {

	private final ResourceLocation textureNormal;
	@Nullable
	private final ResourceLocation textureHovered;
	@Nullable
	private final ResourceLocation textureDisabled;

	private int textColorNormal = 0xFFFFFFFF;
	private int textColorHovered = 0xFFFFFF55;
	private int textColorDisabled = 0xFFA0A0A0;

	public TexturedButton(int x, int y, int width, int height,
			Component message, OnPress onPress,
			ResourceLocation normal,
			@Nullable ResourceLocation hovered,
			@Nullable ResourceLocation disabled) {
		super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
		this.textureNormal = normal;
		this.textureHovered = hovered;
		this.textureDisabled = disabled;
	}

	public TexturedButton setTextColors(int normal, int hovered, int disabled) {
		this.textColorNormal = normal;
		this.textColorHovered = hovered;
		this.textColorDisabled = disabled;
		return this;
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		ResourceLocation tex;
		int textColor;
		if (!active) {
			tex = textureDisabled != null ? textureDisabled : textureNormal;
			textColor = textColorDisabled;
		} else if (isHoveredOrFocused()) {
			tex = textureHovered != null ? textureHovered : textureNormal;
			textColor = textColorHovered;
		} else {
			tex = textureNormal;
			textColor = textColorNormal;
		}
		// Full-stretch blit: source = whole texture, destination = button bounds.
		graphics.blit(tex, getX(), getY(), 0, 0, getWidth(), getHeight(), getWidth(), getHeight());

		Font font = Minecraft.getInstance().font;
		graphics.drawCenteredString(font, getMessage(),
				getX() + getWidth() / 2,
				getY() + (getHeight() - font.lineHeight) / 2,
				textColor);
	}
}
