package snownee.researchtable.client.renderer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.api.ICondition;
import snownee.researchtable.core.ConditionDisplays;
import snownee.researchtable.core.FluidDisplayCondition;

public class FluidConditionRenderer<T extends ICondition<?> & FluidDisplayCondition> extends ConditionRenderer<T> {
	private static final DecimalFormat COMMA = new DecimalFormat("#,###");

	private final List<FluidStack> fluids;

	public FluidConditionRenderer(T condition) {
		fluids = ConditionDisplays.copyFluids(condition.getDisplayFluids());
	}

	private FluidStack getFluid() {
		if (fluids.isEmpty()) {
			return FluidStack.EMPTY;
		}
		long ticks = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
		int index = (int) ((ticks / 30) % fluids.size());
		return fluids.get(index);
	}

	@Override
	public void draw(GuiGraphics graphics, Minecraft mc, int x, int y) {
		FluidStack fluid = getFluid();
		if (fluid.isEmpty()) {
			return;
		}
		IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
		ResourceLocation still = ext.getStillTexture(fluid);
		TextureAtlasSprite sprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
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
		FluidStack fluid = getFluid();
		if (!fluid.isEmpty()) {
			return fluid.getHoverName().getString();
		}
		return I18n.get(ResearchTable.MODID + ".gui.unknown_fluid");
	}

	@Override
	public String format(long number) {
		if (number >= 10000) {
			return COMMA.format(number / 1000) + "B";
		}
		return COMMA.format(number) + "mB";
	}

	@Override
	public Font getFont() {
		return Minecraft.getInstance().font;
	}

	@Override
	public List<Component> getTooltip(TooltipFlag flag) {
		FluidStack fluid = getFluid();
		if (fluid.isEmpty()) {
			return Collections.emptyList();
		}
		List<Component> tooltip = new ArrayList<>();
		tooltip.add(fluid.getHoverName());
		if (flag.isAdvanced()) {
			ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
			tooltip.add(Component.literal(id.toString()).withStyle(ChatFormatting.DARK_GRAY));
		}
		return tooltip;
	}
}
