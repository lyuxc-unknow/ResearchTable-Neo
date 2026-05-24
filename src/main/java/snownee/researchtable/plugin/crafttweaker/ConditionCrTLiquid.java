package snownee.researchtable.plugin.crafttweaker;

import java.util.function.Supplier;

import com.blamejared.crafttweaker.api.fluid.IFluidStack;

import net.neoforged.neoforge.fluids.FluidStack;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.core.ConditionType;
import snownee.researchtable.core.ConditionTypes;
import snownee.researchtable.core.ICondition;

public class ConditionCrTLiquid implements ICondition<FluidStack> {
	final FluidStack fluid;
	final long count;

	public ConditionCrTLiquid(IFluidStack ingredient) {
		this(ingredient, ingredient.getAmount());
	}

	public ConditionCrTLiquid(IFluidStack ingredient, long count) {
		this.count = count;
		FluidStack raw = ingredient.<FluidStack>getInternal().copy();
		raw.setAmount(1);
		this.fluid = raw;
	}

	private ConditionCrTLiquid(FluidStack fluid, long count) {
		this.fluid = fluid;
		this.count = count;
	}

	@Override
	public long matches(FluidStack e) {
		if (e != null && !e.isEmpty() && FluidStack.isSameFluidSameComponents(fluid, e)) {
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

	public FluidStack getFluid() {
		return fluid;
	}

	public static final ConditionType<ConditionCrTLiquid> TYPE = ConditionType.register(
			net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ResearchTable.MODID, "crt_fluid"),
			(buf, c) -> {
				FluidStack.OPTIONAL_STREAM_CODEC.encode(buf, c.fluid);
				buf.writeVarLong(c.count);
			},
			buf -> {
				FluidStack f = FluidStack.OPTIONAL_STREAM_CODEC.decode(buf);
				long count = buf.readVarLong();
				return new ConditionCrTLiquid(f, count);
			});

	@Override
	public ConditionType<ConditionCrTLiquid> getType() {
		return TYPE;
	}
}
