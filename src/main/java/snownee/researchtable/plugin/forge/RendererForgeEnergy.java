package snownee.researchtable.plugin.forge;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.client.renderer.ConditionRenderer;

public class RendererForgeEnergy extends ConditionRenderer<ConditionForgeEnergy> {
	private static final RendererForgeEnergy INSTANCE = new RendererForgeEnergy();
	private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(ResearchTable.MODID, "textures/gui/energy.png");
	private static final DecimalFormat COMMA_FORMAT = new DecimalFormat("#,###");

	private RendererForgeEnergy() {
	}

	@Override
	public void draw(GuiGraphics graphics, Minecraft mc, int x, int y) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		graphics.blit(ICON, x, y, 0, 0, 16, 16, 16, 16);
	}

	@Override
	public String name() {
		return I18n.get(ResearchTable.MODID + ".gui.fe");
	}

	@Override
	public String format(long number) {
		return COMMA_FORMAT.format(number);
	}

	public static class Factory implements ConditionRendererFactory<ConditionForgeEnergy> {
		@Override
		public ConditionRenderer<ConditionForgeEnergy> get(ConditionForgeEnergy condition) {
			return RendererForgeEnergy.INSTANCE;
		}
	}

	@Override
	public Font getFont() {
		return Minecraft.getInstance().font;
	}

	@Override
	public List<Component> getTooltip(TooltipFlag flag) {
		return Collections.emptyList();
	}
}
