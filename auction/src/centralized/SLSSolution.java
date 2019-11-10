package centralized;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.topology.Topology.City;

public class SLSSolution {
	private Map<Vehicle, List<SLSState>> vehicleNextStates;

	public SLSSolution(List<Vehicle> vehicles) {
		vehicleNextStates = new HashMap<Vehicle, List<SLSState>>();
		
		for(Vehicle v : vehicles) {
			vehicleNextStates.put(v, new ArrayList<SLSState>());
		}
	}
	
	//Copy
	public SLSSolution(SLSSolution solution) {
		this.vehicleNextStates = new HashMap<Vehicle, List<SLSState>>(solution.vehicleNextStates);
		for(Entry<Vehicle, List<SLSState>> entry : solution.vehicleNextStates.entrySet()) {
			vehicleNextStates.put(entry.getKey(), new ArrayList<SLSState>(entry.getValue()));
		}
	}

	public List<SLSState> getNextStates(Vehicle v){
		return vehicleNextStates.get(v);
	}
	
	public void setNextStates(Vehicle v, List<SLSState> nextStates) {
		vehicleNextStates.put(v, nextStates);
	}

	public List<Plan> getPlans(List<Vehicle> vehicles) {
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
					plan.appendPickup(s.getTask());
				}else {
					plan.appendDelivery(s.getTask());
				}
			}
			plans.add(plan);
		}

		return plans;
	}

	public double getCost(List<Vehicle> vehicles) {
		double cost = 0.0;
		for(Vehicle v : vehicles) {
			City currentCity = v.getCurrentCity();
			List<SLSState> nextStates = getNextStates(v);
			
			for(SLSState s : nextStates) {
				City sCity = s.getCity();
				cost += currentCity.distanceTo(sCity) * v.costPerKm();
				currentCity = sCity;
			}
		}

		return cost;
	}

	public void printPath(Vehicle v) {
		List<SLSState> nextStates = getNextStates(v);
		
		for(SLSState s : nextStates) {
			System.out.print(s.getTask().id +" ");
		}
		System.out.println();
	}
}
