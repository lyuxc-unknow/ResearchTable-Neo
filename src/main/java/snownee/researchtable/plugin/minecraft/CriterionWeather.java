package snownee.researchtable.plugin.minecraft;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.core.CriterionType;
import snownee.researchtable.api.ICriterion;

public class CriterionWeather implements ICriterion {
	public enum Weather {
		CLEAR, RAIN, THUNDER;

		public static Weather of(Level level) {
			if (level.isThundering()) {
				return THUNDER;
			}
			if (level.isRaining()) {
				return RAIN;
			}
			return CLEAR;
		}

		public String key() {
			return ResearchTable.MODID + ".gui.weather." + name().toLowerCase();
		}
	}

	private final Weather weather;

	public CriterionWeather(Weather weather) {
		this.weather = weather;
	}

	@Override
	public boolean matches(Player player, CompoundTag data) {
		return Weather.of(player.level()) == weather;
	}

	@Override
	public String getFailingText(Player player, CompoundTag data) {
		return I18n.get(ResearchTable.MODID + ".gui.needWeather", I18n.get(weather.key()));
	}

	public static final CriterionType<CriterionWeather> TYPE = CriterionType.register(
			ResearchTable.id("weather"),
			(buf, c) -> buf.writeVarInt(c.weather.ordinal()),
			buf -> new CriterionWeather(Weather.values()[Math.floorMod(buf.readVarInt(), Weather.values().length)]));

	@Override
	public CriterionType<CriterionWeather> getType() {
		return TYPE;
	}
}
