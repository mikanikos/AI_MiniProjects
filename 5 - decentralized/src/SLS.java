import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.task.Task;

/**
 * Static class that compute a solution using the SLS algorithm
 * @author Piccione Andrea, Juppet Quentin
 */
public class SLS {
	// ADD MARGIN JUST TO BE SURE TO FINISH IN TIME
	private static final int TIMEOUT_MARGIN = 500;
	private static final double PROBABILITY = 0.5;
	
	private static SLSSolution bestSolution;

	public static SLSSolution Solve(SLSSolution currentSolution, Random random, long timeout) {
		long startTime = System.currentTimeMillis();

		bestSolution = currentSolution;

		long totalChoose = 0;
		long totalLocal = 0;

		long endTime = System.currentTimeMillis();
		while(System.currentTimeMillis() - startTime < timeout - TIMEOUT_MARGIN) {
			List<SLSSolution> neighbours = chooseNeighbours(currentSolution, random);

			long time1 = System.currentTimeMillis();
			totalChoose += time1 - endTime;

			currentSolution = localChoice(currentSolution, neighbours, random);

			endTime = System.currentTimeMillis();
			totalLocal += endTime - time1;
			
			//System.out.println(endTime - startTime);
		}

		for(Vehicle v : bestSolution.getVehicles()) {
			//System.out.println(v.name());
			//bestSolution.printPath(v);
		}

		//System.out.println(totalChoose + " " + totalLocal);
		//System.out.println("Best cost: " + bestSolution.getCost());

		return bestSolution;
	}

	public static SLSSolution addTaskInSolution(SLSSolution solution, Task task) {
		SLSSolution newSolution = new SLSSolution(solution);

		Vehicle biggestVehicle = null;
		for(Vehicle v : newSolution.getVehicles()) {
			if(biggestVehicle == null || biggestVehicle.capacity() < v.capacity()) {
				biggestVehicle = v;
			}
		}

		List<SLSState> nextStates = newSolution.getNextStates(biggestVehicle);
		
		SLSState pickupState = new SLSState(true, task);

		nextStates.add(pickupState);
		nextStates.add(pickupState.getComplement());

		newSolution.updateCost();
		
		return newSolution;
	}

	private static List<SLSSolution> chooseNeighbours(SLSSolution currentSolution, Random random){
		List<SLSSolution> neighbours = new ArrayList<SLSSolution>();

		List<SLSState> nextStates;
		Vehicle vi;
		List<Vehicle> vehicles = currentSolution.getVehicles();
		do{
			vi = vehicles.get(random.nextInt(vehicles.size()));
			nextStates = currentSolution.getNextStates(vi);
		}while(nextStates.isEmpty());

		SLSState s;
		do{
			s = nextStates.get(random.nextInt(nextStates.size()));
		}while(!s.isPickup());

		//Move a state to any position of any vehicle
		for(Vehicle vj : vehicles) {
			if(vj != vi) {
				List<SLSState> vjNextStates = currentSolution.getNextStates(vj);
				for(int i = 0; i <= vjNextStates.size(); ++i) {
					for(int j = i; j <= vjNextStates.size(); ++j){
						SLSSolution newNeighbour = changingVehicle(currentSolution, s, vi, vj, i, j);
						if(newNeighbour != null)
							neighbours.add(newNeighbour);
					}
				}
			}
		}

		//Reorder state in one vehicle
		if(nextStates.size() > 2) {
			for(int i = 0; i < nextStates.size(); ++i) {
				for(int j = i + 1; j < nextStates.size(); ++j){
					SLSSolution newNeighbour = changingTaskOrder(currentSolution, vi, i, j);
					if(newNeighbour != null)
						neighbours.add(newNeighbour);
				}
			}
		}

		return neighbours;
	}

	private static SLSSolution changingVehicle(SLSSolution currentSolution, SLSState pickupState, Vehicle vi, Vehicle vj, int pickupPos, int deliverPos) {
		SLSSolution newNeighbour = new SLSSolution(currentSolution);
		SLSState deliverState = pickupState.getComplement();

		List<SLSState> viNextStates = newNeighbour.getNextStates(vi);
		List<SLSState> vjNextStates = newNeighbour.getNextStates(vj);

		//Remove from vehicle vi
		viNextStates.remove(pickupState);
		viNextStates.remove(deliverState);
		
		//Build new next state of vehicle vj
		List<SLSState> beforePickup = vjNextStates.subList(0, pickupPos);
		List<SLSState> betweenPnD = vjNextStates.subList(pickupPos, deliverPos);
		List<SLSState> afterDelivery = vjNextStates.subList(deliverPos, vjNextStates.size());
		
		vjNextStates = new ArrayList<SLSState>();
		vjNextStates.addAll(beforePickup);
		vjNextStates.add(pickupState);
		vjNextStates.addAll(betweenPnD);
		vjNextStates.add(deliverState);
		vjNextStates.addAll(afterDelivery);
		
		newNeighbour.setNextStates(vj, vjNextStates);
		
		if(!isWeightValidForVehicle(newNeighbour, vi)) 
			return null;

		if(!isWeightValidForVehicle(newNeighbour, vj)) 
			return null;

		return newNeighbour;
	}
	
	private static SLSSolution changingTaskOrder(SLSSolution currentSolution, Vehicle v, int i, int j) {
		SLSSolution newNeighbour = new SLSSolution(currentSolution);
		
		List<SLSState> nextStates = newNeighbour.getNextStates(v);
		SLSState si = nextStates.get(i);
		SLSState sj = nextStates.get(j);

		if(si.isPickup()) {
			int deliveryTime = nextStates.indexOf(si.getComplement());
			int newPickupTime = j;

			//Prevent a pickup to happened after a delivery
			if(deliveryTime <= newPickupTime)
				return null;
		}
		if(!sj.isPickup()) {
			int newDeliveryTime = i;
			int pickupTime = nextStates.indexOf(sj.getComplement());

			//Prevent a delivery to happened before a pickup
			if(newDeliveryTime <= pickupTime)
				return null;
		}
		
		//Switch the states
		nextStates.set(j, si);
		nextStates.set(i, sj);

		if(!isWeightValidForVehicle(newNeighbour, v)) 
			return null;

		return newNeighbour;
	}

	private static boolean isWeightValidForVehicle(SLSSolution solution, Vehicle v) {
		int weight = 0;
		List<SLSState> nextStates = solution.getNextStates(v);
		for(SLSState s : nextStates) {
			if(s.isPickup()) {
				weight += s.getTask().weight;
				if(weight > v.capacity())
					return false; 
			}else {
				weight -= s.getTask().weight;
			}
		}
		return true;
	}

	private static SLSSolution localChoice(SLSSolution currentSolution, List<SLSSolution> neighbours, Random random) {
		double p = random.nextDouble();

		List<SLSSolution> bestSolutions = null;
		double minCost = 0;
		for(SLSSolution solution : neighbours) {
			solution.updateCost();

			if(bestSolutions == null || minCost > solution.getCost()) {
				bestSolutions = new ArrayList<SLSSolution>();
				bestSolutions.add(solution);
				minCost = solution.getCost();
			}else if(minCost == solution.getCost()) {
				bestSolutions.add(solution);
			}
		}

		if(minCost < bestSolution.getCost()) {
			bestSolution = bestSolutions.get(random.nextInt(bestSolutions.size()));
		}
		
		if(p > PROBABILITY)
			return currentSolution;

		return bestSolutions.get(random.nextInt(bestSolutions.size()));
	}
}
