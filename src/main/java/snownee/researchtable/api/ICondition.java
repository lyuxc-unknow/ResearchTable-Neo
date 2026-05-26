package snownee.researchtable.api;

import java.util.function.Supplier;

import snownee.researchtable.core.ConditionType;

public interface ICondition<T> {
	Supplier<Class<T>> getMatchType();

	long matches(T e);

	long getGoal();

	ConditionType<? extends ICondition<T>> getType();
}
