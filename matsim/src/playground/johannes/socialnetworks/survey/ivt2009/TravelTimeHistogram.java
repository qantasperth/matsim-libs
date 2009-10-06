/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeHistogram.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.survey.ivt2009;

import gnu.trove.TDoubleIntHashMap;
import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TDoubleObjectIterator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;

import playground.johannes.socialnetworks.graph.spatial.SpatialGraphStatistics;
import playground.johannes.socialnetworks.graph.spatial.SpatialGrid;
import playground.johannes.socialnetworks.graph.spatial.SpatialVertex;
import playground.johannes.socialnetworks.spatial.TravelTimeMatrix;
import playground.johannes.socialnetworks.spatial.Zone;
import playground.johannes.socialnetworks.spatial.ZoneLayer;
import playground.johannes.socialnetworks.statistics.Distribution;
import playground.johannes.socialnetworks.survey.ivt2009.spatial.SampledSpatialGraph;
import playground.johannes.socialnetworks.survey.ivt2009.spatial.SampledSpatialGraphMLReader;
import playground.johannes.socialnetworks.survey.ivt2009.spatial.SampledSpatialVertex;

/**
 * @author illenberger
 *
 */
public class TravelTimeHistogram {

	public static void main(String args[]) throws FileNotFoundException, IOException {
		Config config = Gbl.createConfig(new String[]{args[0]});
//		ScenarioLoader loader = new ScenarioLoader(config);
//		loader.loadScenario();
//		ScenarioImpl data = loader.getScenario();
//		NetworkLayer network = (NetworkLayer) data.getNetwork();
//		
		String output = config.getParam("tthistogram", "output");
//		/*
//		 * load events
//		 */
//		int binSize = Integer.parseInt(config.getParam("tthistogram", "binsize")); // one day
//		int maxTime = 60*60*24;
//		
//		final TravelTimeCalculator ttCalculator = new TravelTimeCalculator(network, binSize, maxTime, new TravelTimeCalculatorConfigGroup());
//		EventsImpl events = new EventsImpl();
//		events.addHandler(ttCalculator);
//		EventsReaderTXTv1 eReader = new EventsReaderTXTv1(events);
//		eReader.readFile(config.getParam("tthistogram", "eventsfile"));
//		/*
//		 * init dijkstra
//		 */
//		Dijkstra router = new Dijkstra(network, new TravelCost() {
//			
//			public double getLinkTravelCost(Link link, double time) {
//				return ttCalculator.getLinkTravelTime(link, 28800);
//			}
//		}, ttCalculator);
		/*
		 * read graph
		 */
		SampledSpatialGraphMLReader reader = new SampledSpatialGraphMLReader();
		SampledSpatialGraph graph = reader.readGraph(config.getParam("tthistogram", "graph"));
		/*
		 * read zones
		 */
		ZoneLayer zoneLayer = ZoneLayer.createFromShapeFile("");
		TravelTimeMatrix matrix = TravelTimeMatrix.createFromFile(new HashSet<Zone>(zoneLayer.getZones()), "");
//		Population2SpatialGraph pop2graph = new Population2SpatialGraph();
//		SpatialGraph graph2 = pop2graph.read("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/plans/plans.0.04.xml");
//		double bounds[] = graph2.getBounds();
		/*
		 * read grid
		 */
		SpatialGrid<Double> grid = SpatialGrid.readFromFile(config.getParam("tthistogram", "densityfile"));
		/*
		 * get sampled partition
		 */
		Set<? extends SampledSpatialVertex> sbPartition = SnowballPartitions.createSampledPartition(graph.getVertices());
		TDoubleObjectHashMap<?> partitions = SpatialGraphStatistics.createDensityPartitions(sbPartition, grid, 2000);
		
		new File(output + "rhoPartitions").mkdirs();
		
		Distribution fastest = new Distribution();
		Distribution fastestNorm = new Distribution();
		int counter=0;
//		int starttime = Integer.parseInt(config.getParam("tthistogram", "starttime"));
		TDoubleObjectIterator<?> it = partitions.iterator();
		for(int i = 0; i < partitions.size(); i++) {
			it.advance();
			Set<SampledSpatialVertex> partition = (Set<SampledSpatialVertex>) it.value();
			
			Distribution rhoDistr = new Distribution();

			for(SpatialVertex v : partition) {
				Zone z_i = zoneLayer.getZone(v.getCoordinate());
				
				TDoubleIntHashMap n_i = new TDoubleIntHashMap();
				for(SpatialVertex v2 : v.getNeighbours()) {
					Zone z_j = zoneLayer.getZone(v2.getCoordinate());
					double tt = matrix.getTravelTime(z_i, z_j);
					double bin = tt/300;
					n_i.adjustOrPutValue(bin, 1, 1);

				}

							
				for(SpatialVertex v2 : v.getNeighbours()) {
					Zone z_j = zoneLayer.getZone(v2.getCoordinate());

					double tt = matrix.getTravelTime(z_i, z_j);
					rhoDistr.add(tt);
					fastest.add(tt);
					fastestNorm.add(tt, 1/n_i.get(tt/300));
				}
				
				counter++;
				System.out.println(String.format("Processed %1$s of %2$s vertices.", counter, graph.getVertices().size()));
			}
			
			Distribution.writeHistogram(rhoDistr.absoluteDistribution(60), output + "rhoPartitions/traveltime." + it.key() + ".txt");
			Distribution.writeHistogram(rhoDistr.absoluteDistributionLog2(60), output + "rhoPartitions/traveltime.log2." + it.key() + ".txt");
			
		}
				
		Distribution.writeHistogram(fastest.absoluteDistribution(60), output + "traveltime.txt");
		Distribution.writeHistogram(fastest.absoluteDistributionLog2(60), output + "traveltime.log2.txt");
		Distribution.writeHistogram(fastestNorm.absoluteDistributionLog2(60), output + "traveltime.norm.log2.txt");
	}
}
