import logist.topology.Topology.City;

/*
 * An action for the value iteration
 * We can either move to some destination without picking up anything
 * or move to a destination by picking up a task
 */
public class ReactiveAction {
	private City destination;
	private boolean isPickingUp;
	
	public ReactiveAction(City destination, boolean isPickingUp) {
		this.destination = destination;
		this.isPickingUp = isPickingUp;
	}
	
	public City getDestination() {
		return destination;
	}
	
	public boolean isPickingUp() {
		return isPickingUp;
	}
}
