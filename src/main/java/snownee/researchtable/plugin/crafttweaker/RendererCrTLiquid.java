package snownee.researchtable.plugin.crafttweaker;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import snownee.researchtable.client.renderer.ConditionRenderer;

public class RendererCrTLiquid extends ConditionRenderer<ConditionCrTLiquid> {
	private static final DecimalFormat COMMA = new DecimalFormat("#,###");

	private final FluidStack fluid;

	public RendererCrTLiquid(ConditionCrTLiquid condition) {
		this.fluid = condition.getFluid();
	}

	@Override
	public void draw(GuiGraphics graphics, Minecraft mc, int x, int y) {
		IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
		ResourceLocation still = ext.getStillTexture(fluid);
		TextureAtlasSprite sprite = mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(still);
		int tint = ext.getTintColor(fluid);
		float a = ((tint >> 24) & 0xFF) / 255.0F;
		float r = ((tint >> 16) & 0xFF) / 255.0F;
		float g = ((tint >> 8) & 0xFF) / 255.0F;
		float b = (tint & 0xFF) / 255.0F;
		RenderSystem.setShaderColor(r, g, b, a);
		graphics.blit(x, y, 0, 16, 16, sprite);
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}

	@Override
	public String name() {
		return fluid.getHoverName().getString();
	}

	@Override
	public String format(long number) {
		if (number >= 10000) {
			return COMMA.format(number / 1000) + "B";
		}
		return COMMA.format(number) + "mB";
	}

	public static class Factory implements ConditionRendererFactory<ConditionCrTLiquid> {
		@Override
		public ConditionRenderer<ConditionCrTLiquid> get(ConditionCrTLiquid condition) {
			return new RendererCrTLiquid(condition);
		}
	}

	@Override
	public Font getFont() {
		return Minecraft.getInstance().font;
	}

	@Override
	public List<Component> getTooltip(TooltipFlag flag) {
		List<Component> tooltip = new ArrayList<>();
		tooltip.add(fluid.getHoverName());
		if (flag.isAdvanced()) {
			ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
			tooltip.add(Component.literal(id.toString()).withStyle(ChatFormatting.DARK_GRAY));
		}
		return tooltip;
	}
}
