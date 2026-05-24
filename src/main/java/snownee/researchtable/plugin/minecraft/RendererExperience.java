package snownee.researchtable.plugin.minecraft;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.client.renderer.ConditionRenderer;

public class RendererExperience extends ConditionRenderer<ConditionExperience> {
	private static final RendererExperience INSTANCE = new RendererExperience();
	private static final ItemStack ICON = new ItemStack(Items.EXPERIENCE_BOTTLE);
	private static final DecimalFormat COMMA_FORMAT = new DecimalFormat("#,###");

	private RendererExperience() {
	}

	@Override
	public void draw(GuiGraphics graphics, Minecraft mc, int x, int y) {
		graphics.renderItem(ICON, x, y);
	}

	@Override
	public String name() {
		return I18n.get(ResearchTable.MODID + ".gui.xp");
	}

	@Override
	public String format(long number) {
		return COMMA_FORMAT.format(number);
	}

	@Override
	public Font getFont() {
		return Minecraft.getInstance().font;
	}

	@Override
	public List<Component> getTooltip(TooltipFlag flag) {
		return Collections.emptyList();
	}

	public static class Factory implements ConditionRendererFactory<ConditionExperience> {
		@Override
		public ConditionRenderer<ConditionExperience> get(ConditionExperience condition) {
			return RendererExperience.INSTANCE;
		}
	}
}
