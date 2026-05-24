package snownee.researchtable.core;

import java.util.function.Supplier;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public class ConditionTypes {
	private ConditionTypes() {
	}

	public static final Supplier<Class<ItemStack>> ITEM = () -> ItemStack.class;
	public static final Supplier<Class<FluidStack>> FLUID = () -> FluidStack.class;
	public static final Supplier<Class<Long>> ENERGY = () -> Long.class;
	public static final Supplier<Class<Integer>> EXPERIENCE = () -> Integer.class;
}
