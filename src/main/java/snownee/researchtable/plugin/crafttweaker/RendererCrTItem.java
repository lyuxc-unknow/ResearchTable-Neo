package snownee.researchtable.plugin.crafttweaker;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.client.renderer.ConditionRenderer;

public class RendererCrTItem extends ConditionRenderer<ConditionCrTItem> {
	private static final DecimalFormat COMMA = new DecimalFormat("#,###");

	private final List<ItemStack> stacks;
	@Nullable
	private final String name;

	public RendererCrTItem(ConditionCrTItem condition) {
		stacks = new ArrayList<>(condition.getDisplayItems());
		name = condition.getCustomName();
	}

	private ItemStack getStack() {
		if (stacks.isEmpty()) {
			return ItemStack.EMPTY;
		}
		long ticks = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
		int index = (int) ((ticks / 30) % stacks.size());
		return stacks.get(index);
	}

	@Override
	public void draw(GuiGraphics graphics, Minecraft mc, int x, int y) {
		if (!stacks.isEmpty()) {
			graphics.renderItem(getStack(), x, y);
		}
	}

	@Override
	public String name() {
		if (name != null) {
			return I18n.get(name);
		}
		if (!stacks.isEmpty()) {
			return getStack().getHoverName().getString();
		}
		return I18n.get(ResearchTable.MODID + ".gui.unknown_item");
	}

	@Override
	public String format(long number) {
		return COMMA.format(number);
	}

	public static class Factory implements ConditionRendererFactory<ConditionCrTItem> {
		@Override
		public ConditionRenderer<ConditionCrTItem> get(ConditionCrTItem condition) {
			return new RendererCrTItem(condition);
		}
	}

	@Override
	public Font getFont() {
		return Minecraft.getInstance().font;
	}

	@Override
	public List<Component> getTooltip(TooltipFlag flag) {
		if (!stacks.isEmpty()) {
			return new ArrayList<>(getStack().getTooltipLines(Item.TooltipContext.EMPTY, null, flag));
		}
		return Collections.emptyList();
	}
}
