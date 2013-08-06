/* *********************************************************************** *
 * project: org.matsim.*
 * MobsimDataProvider.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.withinday.mobsim;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimLink;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNetwork;

/**
 * Provides Mobsim related data such as the Agents or QVehicles.
 * 
 * @author cdobler
 */
public class MobsimDataProvider implements MobsimInitializedListener {

	private final Map<Id, MobsimAgent> agents = new HashMap<Id, MobsimAgent>(); 
	private final Map<Id, MobsimVehicle> vehicles = new HashMap<Id, MobsimVehicle>();

	private NetsimNetwork netsimNetwork;
	
	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		
		QSim sim = (QSim) e.getQueueSimulation();
		this.netsimNetwork = sim.getNetsimNetwork();
		
		// collect all agents
		this.agents.clear();
		for (MobsimAgent mobsimAgent : sim.getAgents()) {
			this.agents.put(mobsimAgent.getId(), mobsimAgent);
		}
		
		// collect all vehicles
		this.vehicles.clear();
		for (NetsimLink netsimLink : netsimNetwork.getNetsimLinks().values()) {
			for (MobsimVehicle mobsimVehicle : netsimLink.getAllVehicles()) {
				this.vehicles.put(mobsimVehicle.getId(), mobsimVehicle);
			}
		}
	}
	
	public Map<Id, MobsimAgent> getAgents() {
		return this.agents;
	}
	
	public MobsimAgent getAgent(Id agentId) {
		return this.agents.get(agentId);
	}
	
	public Map<Id, MobsimVehicle> getVehicles() {
		return this.vehicles;
	}
	
	public MobsimVehicle getVehicle(Id vehicleId) {
		return this.vehicles.get(vehicleId);
	}
	
	public Collection<MobsimVehicle> getEnrouteVehiclesOnLink(Id linkId) {
		return this.netsimNetwork.getNetsimLink(linkId).getAllNonParkedVehicles();
	}
	
	public MobsimVehicle getDriversVehicle(Id driverId) {
		MobsimAgent mobsimAgent = this.agents.get(driverId);
		if (mobsimAgent == null) return null;
		
		DriverAgent driver = (DriverAgent) mobsimAgent;
		return driver.getVehicle();
	}
	
	public MobsimAgent getVehiclesDriver(Id vehicleId) {
		MobsimVehicle mobsimVehicle = this.vehicles.get(vehicleId);
		if (mobsimVehicle == null) return null;
		else return mobsimVehicle.getDriver();
	}
}
