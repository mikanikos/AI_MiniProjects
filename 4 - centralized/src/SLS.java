import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;

public class SLS {
	private static final int MAX_ITERATIONS = 10000;
	private static final double PROBABILITY = 0.5;
	
	private static SLSSolution bestSolution;
	private static double bestCost;

	public static List<Plan> Solve(List<Vehicle> vehicles, TaskSet tasks) {
		long startTime = System.currentTimeMillis();
		SLSSolution currentSolution = selectInitialSolution(vehicles, tasks);

		bestSolution = currentSolution;
		bestCost = bestSolution.getCost(vehicles);

		long totalChoose = 0;
		long totalLocal = 0;

		long endTime = System.currentTimeMillis();
		for(int i = 0; i < MAX_ITERATIONS; ++i) {
			List<SLSSolution> neighbours = chooseNeighbours(currentSolution, vehicles);

			long time1 = System.currentTimeMillis();
			totalChoose += time1 - endTime;

			currentSolution = localChoice(currentSolution, neighbours, vehicles);

			endTime = System.currentTimeMillis();
			totalLocal += endTime - time1;
			
			System.out.println(endTime - startTime);
		}

		for(Vehicle v : vehicles) {
			System.out.println(v.name());
			bestSolution.printPath(v);
		}

		System.out.println(totalChoose + " " + totalLocal);
		System.out.println("Best cost: " + bestCost);

		return bestSolution.getPlans(vehicles);
	}

	private static SLSSolution selectInitialSolution(List<Vehicle> vehicles, TaskSet tasks) {
		SLSSolution initialSolution = new SLSSolution(vehicles);

		Vehicle biggestVehicle = null;
		for(Vehicle v : vehicles) {
			if(biggestVehicle == null || biggestVehicle.capacity() < v.capacity()) {
				biggestVehicle = v;
			}
		}

		List<SLSState> nextStates = initialSolution.getNextStates(biggestVehicle);
		
		for(Task task : tasks) {
			SLSState pickupState = new SLSState(true, task);

			nextStates.add(pickupState);
			nextStates.add(pickupState.getComplement());
		}

		return initialSolution;
	}

	private static List<SLSSolution> chooseNeighbours(SLSSolution currentSolution, List<Vehicle> vehicles){
		List<SLSSolution> neighbours = new ArrayList<SLSSolution>();

		List<SLSState> nextStates;
		Vehicle vi;
		do{
			vi = vehicles.get(new Random().nextInt(vehicles.size()));
			nextStates = currentSolution.getNextStates(vi);
		}while(nextStates.isEmpty());

		SLSState s;
		do{
			s = nextStates.get(new Random().nextInt(nextStates.size()));
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

		/*for(Vehicle vj : vehicles) {
			if(vj != vi) {
				if(s.getTask().weight <= vj.capacity()) {
					neighbours.add(changingVehicle(currentSolution, s, vi, vj));
				}
			}
		}*/

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
	
	private static SLSSolution changingVehicle(SLSSolution currentSolution, SLSState s, Vehicle vi, Vehicle vj) {
		SLSSolution newNeighbour = new SLSSolution(currentSolution);
		SLSState pickupState;
		SLSState deliverState;
		if(s.isPickup()) {
			pickupState = s;
			deliverState = s.getComplement();
		}else {
			pickupState = s.getComplement();
			deliverState = s;
		}

		List<SLSState> viNextStates = newNeighbour.getNextStates(vi);
		List<SLSState> vjNextStates = newNeighbour.getNextStates(vj);

		//Move pickup state
		viNextStates.remove(pickupState);
		vjNextStates.add(pickupState);
		
		//Move deliver state
		viNextStates.remove(deliverState);
		vjNextStates.add(deliverState);

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

	private static SLSSolution localChoice(SLSSolution currentSolution, List<SLSSolution> neighbours, List<Vehicle> vehicles) {
		double p = new Random().nextDouble();

		List<SLSSolution> bestSolutions = null;
		double minCost = 0;
		for(SLSSolution solution : neighbours) {
			double curCost = solution.getCost(vehicles);
			if(bestSolutions == null || minCost > curCost) {
				bestSolutions = new ArrayList<SLSSolution>();
				bestSolutions.add(solution);
				minCost = curCost;
			}else if(minCost == curCost) {
				bestSolutions.add(solution);
			}
		}

		if(minCost < bestCost) {
			bestCost = minCost;
			bestSolution = bestSolutions.get(new Random().nextInt(bestSolutions.size()));
		}
		
		if(p > PROBABILITY)
			return currentSolution;

		return bestSolutions.get(new Random().nextInt(bestSolutions.size()));
	}
}
