package snownee.researchtable.plugin.kubejs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.api.ICondition;
import snownee.researchtable.core.ConditionDisplays;
import snownee.researchtable.core.ConditionType;
import snownee.researchtable.core.ConditionTypes;
import snownee.researchtable.core.FluidDisplayCondition;

public class ConditionKubeJSFluid implements ICondition<FluidStack>, FluidDisplayCondition {
	@Nullable
	private final FluidIngredient ingredient;
	@Nullable
	private final FluidStack exactFluid;
	private final long count;
	private final List<FluidStack> displayFluids;

	public ConditionKubeJSFluid(FluidStack fluid, long count) {
		this.ingredient = null;
		this.exactFluid = fluid.copy();
		this.exactFluid.setAmount(1);
		this.count = Math.max(0, count);
		this.displayFluids = ConditionDisplays.copyFluids(List.of(fluid));
	}

	public ConditionKubeJSFluid(SizedFluidIngredient ingredient) {
		this.ingredient = ingredient.ingredient();
		this.exactFluid = null;
		this.count = Math.max(0, ingredient.amount());
		this.displayFluids = ConditionDisplays.copyFluids(List.of(ingredient.getFluids()));
	}

	public ConditionKubeJSFluid(FluidIngredient ingredient, long count, List<FluidStack> displayFluids) {
		this.ingredient = ingredient;
		this.exactFluid = null;
		this.count = Math.max(0, count);
		this.displayFluids = ConditionDisplays.copyFluids(displayFluids);
	}

	private ConditionKubeJSFluid(List<FluidStack> displayFluids, long count) {
		this.ingredient = null;
		this.exactFluid = null;
		this.count = Math.max(0, count);
		this.displayFluids = ConditionDisplays.copyFluids(displayFluids);
	}

	@Override
	public long matches(FluidStack e) {
		if (e == null || e.isEmpty()) {
			return 0;
		}
		if (exactFluid != null) {
			return FluidStack.isSameFluidSameComponents(exactFluid, e) ? e.getAmount() : 0;
		}
		if (ingredient != null && ingredient.test(e)) {
			return e.getAmount();
		}
		return 0;
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

	public static final ConditionType<ConditionKubeJSFluid> TYPE = ConditionType.register(
			ResearchTable.id("kjs_fluid"),
			(buf, c) -> {
				ByteBufCodecs.collection(ArrayList::new, FluidStack.OPTIONAL_STREAM_CODEC)
						.encode(buf, new ArrayList<>(c.displayFluids));
				buf.writeVarLong(c.count);
			},
			buf -> {
				ArrayList<FluidStack> fluids = ByteBufCodecs.collection(ArrayList::new, FluidStack.OPTIONAL_STREAM_CODEC).decode(buf);
				long count = buf.readVarLong();
				return new ConditionKubeJSFluid(fluids, count);
			});

	@Override
	public ConditionType<ConditionKubeJSFluid> getType() {
		return TYPE;
	}
}
