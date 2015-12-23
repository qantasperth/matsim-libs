package floetteroed.opdyts.logging;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.trajectorysampling.SamplingStage;
import floetteroed.utilities.statisticslogging.Statistic;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class SurrogateObjectiveFunctionValue<U extends DecisionVariable>
		implements Statistic<SamplingStage<U>> {

	@Override
	public String label() {
		return "Surrogate Objective Function Value";
	}

	@Override
	public String value(final SamplingStage<U> samplingStage) {
		return Double.toString(samplingStage
				.getSurrogateObjectiveFunctionValue());
	}

}
