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

	private HashMap<ReactiveState, City> bestActionForState;
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
		bestActionForState = new HashMap<ReactiveState, City>();
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
		double stop_threshold = 1E-8;

		while(keepUpdating) {
			keepUpdating = false;

			for(ReactiveState state : possibleStates) {
				//Init possible actions for the current state
				//Can either move to a neighbor or pickup and then move to the delivery city
				ArrayList<City> possibleActions = new ArrayList<City>();
				possibleActions.addAll(state.getFrom().neighbors());

				if(state.getTo() != null) { // If there is a task
					possibleActions.add(state.getTo());
				}

				double bestScore = 0;
				City bestAction = null;
				for(City actionCity : possibleActions) {
					double reward = 0;
					if(actionCity.equals(state.getTo())) //Pick up so gain some reward
						reward += td.reward(state.getFrom(), state.getTo());
					reward -= state.getFrom().distanceTo(actionCity) * costPerKm; // Cost

					double accumulatedScore = 0;
					for(ReactiveState accessibleState : accessibleStatesForCity.get(actionCity)) {
						accumulatedScore += td.probability(accessibleState.getFrom(), accessibleState.getTo())
								* bestScoreForState.get(accessibleState);
					}
					double score = reward + discount * accumulatedScore;

					if(bestAction == null || score > bestScore) {
						bestScore = score;
						bestAction = actionCity;
					}
				}

				if(bestScoreForState.get(state) - bestScore > stop_threshold)
					keepUpdating = true;

				bestScoreForState.put(state, bestScore);
				bestActionForState.put(state, bestAction);
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
		City bestAction = bestActionForState.get(state);
		if(availableTask != null && bestAction.equals(availableTask.deliveryCity)) {
			action = new Pickup(availableTask);
		}else {
			action = new Move(bestAction);
		}

		return action;
	}
}
