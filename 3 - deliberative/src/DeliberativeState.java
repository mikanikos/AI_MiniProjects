import java.util.HashMap;

import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class DeliberativeState {

	enum PossibleAction { PickUp, Deliver, None }
	
	private HashMap<Integer, PossibleAction> taskPossibleActions;
	private City pos;
	private double cost;
	private int weight;
	private DeliberativeState previousState;
	private PossibleAction action;
	private Task task;
	
	//New state
	public DeliberativeState(DeliberativeState previous, Task task, PossibleAction action, int costPerKm) {
		previousState = previous;
		this.action = action;
		this.task = task;
		taskPossibleActions = (HashMap<Integer, PossibleAction>)previous.taskPossibleActions.clone();
		this.cost = previous.cost;
		this.weight = previous.weight;
		
		switch(action) {
		case PickUp:
			pos = task.pickupCity;
			weight += task.weight;
			taskPossibleActions.put(task.id, PossibleAction.Deliver);

			break;
		case Deliver:
			pos = task.deliveryCity;
			weight -= task.weight;
			taskPossibleActions.put(task.id, PossibleAction.None);
			
			break;
		case None:
			break;
		}
		double distance = previous.getPosition().distanceTo(pos);
		cost += distance * costPerKm;
	}
	
	public DeliberativeState(TaskSet tasks, City pos, TaskSet existingTasks){
		previousState = null;
		action = PossibleAction.None;
		
		taskPossibleActions = new HashMap<Integer, PossibleAction>();
		for(Task task : tasks) {
			taskPossibleActions.put(task.id, PossibleAction.PickUp);
		}

		this.pos = pos;
		this.cost = 0;
		this.weight = 0;

		for(Task task : existingTasks) {
			taskPossibleActions.put(task.id, PossibleAction.Deliver);
			weight += task.weight;
		}
	}
	
	public DeliberativeState getPreviousState() {
		return previousState;
	}
	
	public PossibleAction getAction() {
		return action;
	}
	
	public Task getTask() {
		return task;
	}
	
	public PossibleAction getPossibleAction(Task task) {
		return taskPossibleActions.get(task.id);
	}

	public City getPosition() {
		return pos;
	}
	
	public double getCost() {
		return cost;
	}
	
	public int getWeight() {
		return weight;
	}
	
}
