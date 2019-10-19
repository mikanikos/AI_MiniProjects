
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class DeliberativeAgent implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR }

	/* Environment */
	Topology topology;

	/* the properties of the agent */
	Agent agent;

	/* the planning class */
	Algorithm algorithm;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		this.topology = topology;
		this.agent = agent;

		// initialize the planner
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");

		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());		
	}

	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = aStarPlan(vehicle, tasks);
			break;
		case BFS:
			// ...
			plan = bfsPlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return plan;
	}

	private Plan bfsPlan(Vehicle vehicle, TaskSet tasks) {		
		ArrayDeque<DeliberativeState> queue = new ArrayDeque <DeliberativeState>();
		
		//Add initial state
		queue.push(new DeliberativeState(tasks, vehicle.getCurrentCity(), vehicle.getCurrentTasks()));
		
		HashMap<DeliberativeState, Double> minCostForState = new HashMap<DeliberativeState, Double>();

		DeliberativeState bestState = null;

		while(!queue.isEmpty()) {
			DeliberativeState current = queue.pop();

			if(current.getWeight() > vehicle.capacity()) continue;
			
			if(minCostForState.containsKey(current)) {
				double minCost = minCostForState.get(current);
				if(minCost < current.getCost())
					continue; //some path is already faster
			}
			minCostForState.put(current, current.getCost());

			boolean isComplete = true;
			for(Task task : tasks) {
				switch(current.getPossibleAction(task)) {
				case PickUp:
					isComplete = false;

					queue.push(new DeliberativeState(current, task, DeliberativeState.PossibleAction.PickUp, vehicle.costPerKm()));

					break;
				case Deliver:
					isComplete = false;

					queue.push(new DeliberativeState(current, task, DeliberativeState.PossibleAction.Deliver, vehicle.costPerKm()));

					break;
				case None:
					// Do nothing
					break;
				}
			}

			if(isComplete) {
				if(bestState == null || bestState.getCost() > current.getCost()) {
					bestState = current;
					//break;
				}
			}
		}

		return planFromState(bestState);
	}

	private Plan aStarPlan(Vehicle vehicle, TaskSet tasks) {
		AStarStateComparator comparator = new AStarStateComparator(tasks, vehicle.costPerKm());
		PriorityQueue<DeliberativeState> queue = new PriorityQueue <DeliberativeState>(1, comparator);

		//Add initial state
		queue.add(new DeliberativeState(tasks, vehicle.getCurrentCity(), vehicle.getCurrentTasks()));

		DeliberativeState bestState = null;

		while(!queue.isEmpty()) {
			DeliberativeState current = queue.poll();

			if(current.getWeight() > vehicle.capacity()) continue;
			
			boolean isComplete = true;
			for(Task task : tasks) {
				switch(current.getPossibleAction(task)) {
				case PickUp:
					isComplete = false;

					queue.add(new DeliberativeState(current, task, DeliberativeState.PossibleAction.PickUp, vehicle.costPerKm()));

					break;
				case Deliver:
					isComplete = false;

					queue.add(new DeliberativeState(current, task, DeliberativeState.PossibleAction.Deliver, vehicle.costPerKm()));

					break;
				case None:
					// Do nothing
					break;
				}
			}

			if(isComplete) {
				bestState = current;
				break;
			}
		}

		return planFromState(bestState);
	}

	private Plan planFromState(DeliberativeState state) {
		ArrayList<DeliberativeState> statePath = new ArrayList<DeliberativeState>();
		statePath.add(state);

		while(state.getPreviousState() != null) {
			statePath.add(state.getPreviousState());
			state = state.getPreviousState();
		}

		City currentPos = state.getPosition();
		Plan plan = new Plan(currentPos);

		for(int i = statePath.size() - 1; i >= 0; --i) {
			DeliberativeState current = statePath.get(i);
			
			for(City city : currentPos.pathTo(current.getPosition()))
				plan.appendMove(city);
			
			currentPos = current.getPosition();
			
			switch(current.getAction()) {
			case PickUp:
				plan.appendPickup(current.getTask());
				break;
			case Deliver:
				plan.appendDelivery(current.getTask());
				break;
			case None:
				//Do nothing
				break;
			}
		}
		
		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		//Do nothing handled by vehicule.getCurrentTasks in plan
	}


}
