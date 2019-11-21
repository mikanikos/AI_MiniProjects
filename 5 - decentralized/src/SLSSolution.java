import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

/**
 * Represent a possible solution of the SLS algorithm
 * @author Piccione Andrea, Juppet Quentin
 */
public class SLSSolution {
	private List<Vehicle> vehicles;
	private Map<Vehicle, List<SLSState>> vehicleNextStates;
	private double cost = 0.0;

	public SLSSolution(List<Vehicle> vehicles) {
		this.vehicles = vehicles;
		vehicleNextStates = new HashMap<Vehicle, List<SLSState>>();
		
		for(Vehicle v : vehicles) {
			vehicleNextStates.put(v, new ArrayList<SLSState>());
		}
	}
	
	//Copy
	public SLSSolution(SLSSolution solution) {
		this.vehicles = solution.vehicles;
		this.cost = solution.cost;
		this.vehicleNextStates = new HashMap<Vehicle, List<SLSState>>(solution.vehicleNextStates);
		for(Entry<Vehicle, List<SLSState>> entry : solution.vehicleNextStates.entrySet()) {
			vehicleNextStates.put(entry.getKey(), new ArrayList<SLSState>(entry.getValue()));
		}
	}

	public List<Vehicle> getVehicles(){
		return vehicles;
	}
	
	public List<SLSState> getNextStates(Vehicle v){
		return vehicleNextStates.get(v);
	}
	
	public void setNextStates(Vehicle v, List<SLSState> nextStates) {
		vehicleNextStates.put(v, nextStates);
	}

	public List<Plan> getPlans(Map<Integer, Task> intToTask) {
		List<Plan> plans = new ArrayList<Plan>();

		for(Vehicle v : vehicles) {
			City currentCity = v.getCurrentCity();
			List<SLSState> nextStates = getNextStates(v);
			Plan plan = new Plan(currentCity);

			for(SLSState s : nextStates) {
				City targetCity = s.getCity();

				for(City c : currentCity.pathTo(targetCity)) {
					plan.appendMove(c);
				}
				currentCity = targetCity;

				if(s.isPickup()) {
					plan.appendPickup(intToTask.get(s.getTask().id));
				}else {
					plan.appendDelivery(intToTask.get(s.getTask().id));
				}
			}
			plans.add(plan);
		}

		return plans;
	}
	
	public void updateCost() {
		cost = 0.0;
		for(Vehicle v : vehicles) {
			City currentCity = v.getCurrentCity();
			List<SLSState> nextStates = getNextStates(v);
			
			for(SLSState s : nextStates) {
				City sCity = s.getCity();
				cost += currentCity.distanceTo(sCity) * v.costPerKm();
				currentCity = sCity;
			}
		}
	}

	public double getCost() {
		return cost;
	}
	
	public List<Task> getTasks(){
		List<Task> tasks = new ArrayList<Task>();
		for(Vehicle v : vehicles) {
			for(SLSState s : vehicleNextStates.get(v)) {
				if(s.isPickup()) {
					tasks.add(s.getTask());
				}
			}
		}
		return tasks;
	}
	
	public void printPath(Vehicle v) {
		List<SLSState> nextStates = getNextStates(v);
		
		for(SLSState s : nextStates) {
			System.out.print(s.getTask().id +" ");
		}
		System.out.println();
	}
}
