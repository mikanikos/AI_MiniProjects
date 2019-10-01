import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveAgent implements ReactiveBehavior {

	private HashMap<ReactiveState, ReactiveAction> bestActionForState;
	private HashMap<ReactiveState, Double> bestScoreForState;
	private HashSet<ReactiveState> possibleStates;
	private HashMap<City, ArrayList<ReactiveState>> accessibleStatesForCity;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		//Init accessible states for a city
		accessibleStatesForCity = new HashMap<Topology.City, ArrayList<ReactiveState>>();
		for(City city : topology) {
			accessibleStatesForCity.put(city, new ArrayList<ReactiveState>());
		}

		//Init possible states
		possibleStates = new HashSet<ReactiveState>();
		bestActionForState = new HashMap<ReactiveState, ReactiveAction>();
		bestScoreForState = new HashMap<ReactiveState, Double>();
		for(City from : topology) {
			addState(from, null); // When in from and no task
			possibleStates.add(new ReactiveState(from, null)); 
			for(City to : topology) {
				if(from != to)
					addState(from, to); // When in from and there is a task for to
			}
		}

		int costPerKm = agent.vehicles().get(0).costPerKm();

		boolean keepUpdating = true;

		while(keepUpdating) {
			keepUpdating = false;

			for(ReactiveState state : possibleStates) {
				// Can either move to a neighbor ...
				for(City neighbor : state.getFrom()) {
					double reward = 0 // No gain since no pick up
							- state.getFrom().distanceTo(neighbor) * costPerKm; // Cost

					double accumulatedScore = 0;
					for(ReactiveState accessibleState : accessibleStatesForCity.get(neighbor)) {
						accumulatedScore += td.probability(accessibleState.getFrom(), accessibleState.getTo())
								* bestScoreForState.get(accessibleState);
					}
					double score = reward + discount * accumulatedScore;

					if(score > bestScoreForState.get(state)) {
						bestScoreForState.put(state, score);
						bestActionForState.put(state, new ReactiveAction(neighbor, false));
						keepUpdating = true;
					}
				}
				
				// ...or pick up (if there is a task)
				if(state.getTo() != null) { // If there is a task
					double reward = td.reward(state.getFrom(), state.getTo())
							- state.getFrom().distanceTo(state.getTo()) * costPerKm; // Cost

					double accumulatedScore = 0;
					for(ReactiveState accessibleState : accessibleStatesForCity.get(state.getTo())) {
						accumulatedScore += td.probability(accessibleState.getFrom(), accessibleState.getTo())
								* bestScoreForState.get(accessibleState);
					}
					double score = reward + discount * accumulatedScore;

					if(score > bestScoreForState.get(state)) {
						bestScoreForState.put(state, score);
						bestActionForState.put(state, new ReactiveAction(state.getTo(), true));
						keepUpdating = true;
					}
				}
			}
		}
	}

	private void addState(City from, City to) {
		ReactiveState state = new ReactiveState(from, to);
		possibleStates.add(state);
		bestScoreForState.put(state, 0.0);
		accessibleStatesForCity.get(from).add(state);
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		ReactiveState state;
		if (availableTask == null) {
			state = new ReactiveState(vehicle.getCurrentCity(), null);
		} else {
			state = new ReactiveState(vehicle.getCurrentCity(), availableTask.deliveryCity);
		}

		Action action;
		ReactiveAction bestAction = bestActionForState.get(state);
		if(bestAction.isPickingUp()) {
			action = new Pickup(availableTask);
		}else {
			action = new Move(bestAction.getDestination());
		}

		return action;
	}
}
