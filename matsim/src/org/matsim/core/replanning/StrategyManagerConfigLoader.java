/* *********************************************************************** *
 * project: org.matsim.*
 * StrategyManagerConfigLoader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.replanning;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.BasicScenario;
import org.matsim.core.api.experimental.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.replanning.modules.ChangeLegMode;
import org.matsim.core.replanning.modules.ExternalModule;
import org.matsim.core.replanning.modules.PlanomatModule;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.ReRouteDijkstra;
import org.matsim.core.replanning.modules.ReRouteLandmarks;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanForRemovalSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.replanning.selectors.PathSizeLogitSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.locationchoice.LocationChoice;

/**
 * Loads the strategy modules specified in the config-file. This class offers
 * backwards-compatibility to the old StrategyManager where the complete class-
 * names were given in the configuration.
 *
 * @author mrieser
 */
public class StrategyManagerConfigLoader {

	private static final Logger log = Logger.getLogger(StrategyManagerConfigLoader.class);

	/**
	 * Reads and instantiates the strategy modules specified in the config-object.
	 *
	 * @param controler the {@link Controler} that provides miscellaneous data for the replanning modules
	 * @param config the {@link Config} object containing the configuration for the strategyManager
	 * @param manager the {@link StrategyManager} to be configured according to the configuration
	 */
	public static void load(final Controler controler, final Config config, final StrategyManager manager) {

		NetworkLayer network = controler.getNetwork();
		TravelCost travelCostCalc = controler.getTravelCostCalculator();
		TravelTime travelTimeCalc = controler.getTravelTimeCalculator();

		manager.setMaxPlansPerAgent(config.strategy().getMaxAgentPlanMemorySize());

		int externalCounter = 0;

		for (StrategyConfigGroup.StrategySettings settings : config.strategy().getStrategySettings()) {
			double rate = settings.getProbability();
			if (rate == 0.0) {
				continue;
			}
			String classname = settings.getModuleName();

			if (classname.startsWith("org.matsim.demandmodeling.plans.strategies.")) {
				classname = classname.replace("org.matsim.demandmodeling.plans.strategies.", "");
			}
			PlanStrategy strategy = null;
			if (classname.equals("KeepLastSelected")) {
				strategy = new PlanStrategy(new KeepSelected());
			} else if (classname.equals("ReRoute") || classname.equals("threaded.ReRoute")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				strategy.addStrategyModule(new ReRoute(controler));
			} else if (classname.equals("ReRoute_Dijkstra")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				strategy.addStrategyModule(new ReRouteDijkstra(config.plansCalcRoute(), network, travelCostCalc, travelTimeCalc));
			} else if (classname.equals("ReRoute_Landmarks")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				PreProcessLandmarks preProcessRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost(config.charyparNagelScoring()));
				preProcessRoutingData.run(network);
				strategy.addStrategyModule(new ReRouteLandmarks(network, travelCostCalc, travelTimeCalc, preProcessRoutingData));
			} else if (classname.equals("TimeAllocationMutator") || classname.equals("threaded.TimeAllocationMutator")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				strategy.addStrategyModule(new TimeAllocationMutator());
			} else if (classname.equals("TimeAllocationMutator7200_ReRouteLandmarks")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				strategy.addStrategyModule(new TimeAllocationMutator(7200));
				PreProcessLandmarks preProcessRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost(config.charyparNagelScoring()));
				preProcessRoutingData.run(network);
				strategy.addStrategyModule(new ReRouteLandmarks(network, travelCostCalc, travelTimeCalc, preProcessRoutingData));
			} else if (classname.equals("ExternalModule")) {
				externalCounter++;
				strategy = new PlanStrategy(new RandomPlanSelector());
				String exePath = settings.getExePath();
				strategy.addStrategyModule(new ExternalModule(exePath, "ext" + externalCounter, controler.getNetwork()));
			} else if (classname.equals("Planomat")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				PlanStrategyModule planomatStrategyModule = new PlanomatModule(controler, controler.getEvents(), controler.getNetwork(), controler.getScoringFunctionFactory(), controler.getTravelCostCalculator(), controler.getTravelTimeCalculator());
				strategy.addStrategyModule(planomatStrategyModule);
			} else if (classname.equals("PlanomatReRoute")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				PlanStrategyModule planomatStrategyModule = new PlanomatModule(controler, controler.getEvents(), controler.getNetwork(), controler.getScoringFunctionFactory(), controler.getTravelCostCalculator(), controler.getTravelTimeCalculator());
				strategy.addStrategyModule(planomatStrategyModule);
				strategy.addStrategyModule(new ReRoute(controler));
			} else if (classname.equals("BestScore")) {
				strategy = new PlanStrategy(new BestPlanSelector());
			} else if (classname.equals("SelectExpBeta")) {
				strategy = new PlanStrategy(new ExpBetaPlanSelector());
			} else if (classname.equals("ChangeExpBeta")) {
				strategy = new PlanStrategy(new ExpBetaPlanChanger());
			} else if (classname.equals("SelectRandom")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
			} else if (classname.equals("ChangeLegMode")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				strategy.addStrategyModule(new ChangeLegMode(config));
				strategy.addStrategyModule(new ReRoute(controler));
			} else if (classname.equals("SelectPathSizeLogit")) {
				strategy = new PlanStrategy(new PathSizeLogitSelector());
//				// JH
			} else if (classname.equals("KSecLoc")){
//				strategy = new PlanStrategy(new RandomPlanSelector());
//				PlanStrategyModule socialNetStrategyModule= new RandomFacilitySwitcherK(network, travelCostCalc, travelTimeCalc);
//				strategy.addStrategyModule(socialNetStrategyModule);
				log.warn("jhackney: No replanning module available in the core for keywords KSecLoc, FSecLoc,SSecloc. The modules have moved to the playground.");
			} else if (classname.equals("FSecLoc")){
//				strategy = new PlanStrategy(new RandomPlanSelector());
//				PlanStrategyModule socialNetStrategyModule= new RandomFacilitySwitcherF(network, travelCostCalc, travelTimeCalc, facilities);
//				strategy.addStrategyModule(socialNetStrategyModule);
				log.warn("jhackney: No replanning module available in the core for keywords KSecLoc, FSecLoc,SSecloc. The modules have moved to the playground.");
			} else if (classname.equals("SSecLoc")){
//				strategy = new PlanStrategy(new RandomPlanSelector());
//				PlanStrategyModule socialNetStrategyModule= new SNPickFacilityFromAlter(network,travelCostCalc,travelTimeCalc);
//				strategy.addStrategyModule(socialNetStrategyModule);
				log.warn("jhackney: No replanning module available in the core for keywords KSecLoc, FSecLoc,SSecloc. The modules have moved to the playground.");
//				// JH
			} else if (classname.equals("LocationChoice")) {
				strategy = new PlanStrategy(new ExpBetaPlanSelector());
				strategy.addStrategyModule(new LocationChoice(controler.getNetwork(), controler, ((ScenarioImpl)controler.getScenarioData()).getKnowledges()));
				strategy.addStrategyModule(new ReRoute(controler));
				strategy.addStrategyModule(new TimeAllocationMutator());
				/* not really happy about the following line. Imagine what happens if everybody does 
				 * this, that one doesn't know at the end which removal-strategy is really used.
				 * The removal strategy must not be tight to a replanning-strategy, but is a general
				 * option that should be set somewhere else.  marcel/9jun2009/CLEANUP
				 */
				// not yet working correctly:
				// compreh. tests needed
				// ah/27jun2009
				// manager.setPlanSelectorForRemoval(new ExpBetaPlanForRemovalSelector());
			}
			//if none of the strategies above could be selected we try to load the class by name
			else {
				//classes loaded by name must not be part of the matsim core
				if (classname.startsWith("org.matsim")) {
					log.error("Strategies in the org.matsim package must not be loaded by name!");
				}
				else {
					try {
						Class<? extends PlanStrategy> klas = (Class<? extends PlanStrategy>) Class.forName(classname);
						Class[] args = new Class[1];
						args[0] = BasicScenario.class;
						Constructor<? extends PlanStrategy> c = null;
						try{
							c = klas.getConstructor(args);
						} catch(NoSuchMethodException e){
							log.warn("Cannot find Constructor in PlanStrategy " + classname + " with single argument of type BasicScenario. " +
									"This is not fatal, trying to find other constructor, however a constructor expecting BasicScenario as " +
									"single argument is recommented!" );
						}
						if (c == null){
							args[0] = Controler.class;
							c = klas.getConstructor(args);
						}
						strategy = c.newInstance(controler);
						log.info("Loaded PlanStrategy from class " + classname);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (InstantiationException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (SecurityException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			}

			if (strategy == null) {
				Gbl.errorMsg("Could not initialize strategy named " + classname);
			}

			manager.addStrategy(strategy, rate);

			// now check if this modules should be disabled after some iterations
			if (settings.getDisableAfter() >= 0) {
				int maxIter = settings.getDisableAfter();
				if (maxIter >= config.controler().getFirstIteration()) {
					manager.addChangeRequest(maxIter + 1, strategy, 0.0);
				} else {
					/* The controler starts at a later iteration than this change request is scheduled for.
					 * make the change right now.					 */
					manager.changeStrategy(strategy, 0.0);
				}
			}
		}
	}

}
