package snownee.researchtable.plugin.minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.api.ICondition;
import snownee.researchtable.core.ConditionDisplays;
import snownee.researchtable.core.ConditionType;
import snownee.researchtable.core.ConditionTypes;
import snownee.researchtable.core.FluidDisplayCondition;

public class ConditionFluid implements ICondition<FluidStack>, FluidDisplayCondition {
	@Nullable
	private final FluidIngredient ingredient;
	private final long count;
	private final List<FluidStack> displayFluids;

	public ConditionFluid(FluidIngredient ingredient, long count) {
		this.ingredient = ingredient;
		this.count = Math.max(0, count);
		this.displayFluids = ConditionDisplays.copyFluids(List.of(ingredient.getStacks()));
	}

	public ConditionFluid(FluidStack fluid, long count) {
		this(FluidIngredient.single(fluid), count);
	}

	private ConditionFluid(List<FluidStack> displayFluids, long count) {
		this.ingredient = null;
		this.count = Math.max(0, count);
		this.displayFluids = ConditionDisplays.copyFluids(displayFluids);
	}

	@Override
	public long matches(FluidStack e) {
		if (e == null || e.isEmpty() || ingredient == null) {
			return 0;
		}
		return ingredient.test(e) ? e.getAmount() : 0;
	}

	@Override
	public long getGoal() {
		return count;
	}

	@Override
	public Supplier<Class<FluidStack>> getMatchType() {
		return ConditionTypes.FLUID;
	}

	@Override
	public List<FluidStack> getDisplayFluids() {
		return ConditionDisplays.copyFluids(displayFluids);
	}

	public static final ConditionType<ConditionFluid> TYPE = ConditionType.register(
			ResearchTable.id("fluid"),
			(buf, c) -> {
				ByteBufCodecs.collection(ArrayList::new, FluidStack.OPTIONAL_STREAM_CODEC)
						.encode(buf, new ArrayList<>(c.displayFluids));
				buf.writeVarLong(c.count);
			},
			buf -> {
				ArrayList<FluidStack> fluids = ByteBufCodecs.collection(ArrayList::new, FluidStack.OPTIONAL_STREAM_CODEC).decode(buf);
				long count = buf.readVarLong();
				return new ConditionFluid(fluids, count);
			});

	@Override
	public ConditionType<ConditionFluid> getType() {
		return TYPE;
	}
}
