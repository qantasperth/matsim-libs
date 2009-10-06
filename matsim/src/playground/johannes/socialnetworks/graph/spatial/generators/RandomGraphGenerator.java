/* *********************************************************************** *
 * project: org.matsim.*
 * RandomGraphGenerator.java
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
package playground.johannes.socialnetworks.graph.spatial.generators;

import java.io.IOException;

import playground.johannes.socialnetworks.graph.generators.ErdosRenyiGenerator;
import playground.johannes.socialnetworks.graph.spatial.SpatialEdge;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraph;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraphAnalyzer;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraphFactory;
import playground.johannes.socialnetworks.graph.spatial.SpatialGrid;
import playground.johannes.socialnetworks.graph.spatial.SpatialVertex;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.graph.spatial.io.SpatialGraphMLWriter;

/**
 * @author illenberger
 *
 */
public class RandomGraphGenerator {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Population2SpatialGraph reader = new Population2SpatialGraph();
		SpatialGraph graph = reader.read("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/plans/plans.0.01.xml");
		
		ErdosRenyiGenerator<SpatialGraph, SpatialVertex, SpatialEdge> generator = new ErdosRenyiGenerator<SpatialGraph, SpatialVertex, SpatialEdge>(new SpatialGraphFactory());
		graph = generator.generate(graph, 0.0001, 4711);
		
		SpatialGraphAnalyzer.analyze(graph, "/Users/fearonni/vsp-work/work/socialnets/mcmc/", false, SpatialGrid.readFromFile("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/zrh100km/popdensity/popdensity.1000.xml"));
		SpatialGraphMLWriter writer = new SpatialGraphMLWriter();
		writer.write(graph, "/Users/fearonni/vsp-work/work/socialnets/mcmc/graph.graphml");
	}

}
