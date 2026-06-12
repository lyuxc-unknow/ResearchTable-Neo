package snownee.researchtable.core;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public final class ConditionDisplays {
	private ConditionDisplays() {
	}

	public static List<ItemStack> copyItems(Collection<ItemStack> items) {
		return items.stream()
				.filter(Objects::nonNull)
				.filter(stack -> !stack.isEmpty())
				.map(ItemStack::copy)
				.toList();
	}

	public static List<FluidStack> copyFluids(Collection<FluidStack> fluids) {
		return fluids.stream()
				.filter(Objects::nonNull)
				.filter(stack -> !stack.isEmpty())
				.map(FluidStack::copy)
				.toList();
	}
}
