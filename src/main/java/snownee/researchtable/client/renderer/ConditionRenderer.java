package snownee.researchtable.client.renderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.TooltipFlag;
import snownee.researchtable.core.ICondition;

public abstract class ConditionRenderer<T extends ICondition<?>> {
	private static final Map<Class<?>, ConditionRendererFactory<?>> MAP = new HashMap<>();

	public static <T extends ICondition<?>> void register(Class<T> clazz, ConditionRendererFactory<T> renderer) {
		MAP.put(clazz, renderer);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public static <T extends ICondition<?>> ConditionRenderer<T> get(T condition) {
		ConditionRendererFactory<T> factory = (ConditionRendererFactory<T>) MAP.get(condition.getClass());
		if (factory != null) {
			return factory.get(condition);
		}
		return null;
	}

	public abstract void draw(GuiGraphics graphics, Minecraft mc, int x, int y);

	public abstract String name();

	public abstract String format(long number);

	public abstract Font getFont();

	public abstract List<net.minecraft.network.chat.Component> getTooltip(TooltipFlag flag);

	public interface ConditionRendererFactory<T extends ICondition<?>> {
		ConditionRenderer<T> get(T condition);
	}
}
