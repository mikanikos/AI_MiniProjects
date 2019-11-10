package centralized;

import java.util.Objects;

import logist.task.Task;
import logist.topology.Topology.City;

public class SLSState {
	private boolean isPickup;
	private Task task;
	
	public SLSState(boolean isPickup, Task task) {
		this.isPickup = isPickup;
		this.task = task;
	}
	
	public Task getTask() {
		return task;
	}
	
	public boolean isPickup() {
		return isPickup;
	}
	
	public City getCity() {
		if(isPickup)
			return task.pickupCity;
		return task.deliveryCity;
	}
	
	public SLSState getComplement() {
		return new SLSState(!isPickup, task);
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;
		
		SLSState s = (SLSState)o;
		
		if(s.isPickup != isPickup) return false;
		if(s.task != task) return false;
		
		return true;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(task, isPickup);
	}
}
