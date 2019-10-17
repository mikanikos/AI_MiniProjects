import java.util.Comparator;

import logist.task.Task;
import logist.task.TaskSet;

public class AStarStateComparator implements Comparator<DeliberativeState> {

	private TaskSet tasks;
	private int costPerKm;
	
	public AStarStateComparator(TaskSet tasks, int costPerKm) {
		this.tasks = tasks;
		this.costPerKm = costPerKm;
	}
	
	@Override
	public int compare(DeliberativeState s1, DeliberativeState s2) {
		
		double minDistance1 = 0;
		double minDistance2 = 0;
		
		for(Task task : tasks) {
			switch(s1.getPossibleAction(task)) {
			case PickUp:
				double pickUpMinDistance = s1.getPosition().distanceTo(task.pickupCity) + task.pathLength();
				if(pickUpMinDistance > minDistance1)
					minDistance1 = pickUpMinDistance;
				break;
			case Deliver:
				double deliverMinDistance = s1.getPosition().distanceTo(task.deliveryCity);
				if(deliverMinDistance > minDistance1)
					minDistance1 = deliverMinDistance;

				break;
			case None:
				//Do nothing
				break;
			}
			
			switch(s2.getPossibleAction(task)) {
			case PickUp:
				double pickUpMinDistance = s2.getPosition().distanceTo(task.pickupCity) + task.pathLength();
				if(pickUpMinDistance > minDistance2)
					minDistance2 = pickUpMinDistance;
				break;
			case Deliver:
				double deliverMinDistance = s2.getPosition().distanceTo(task.deliveryCity);
				if(deliverMinDistance > minDistance2)
					minDistance2 = deliverMinDistance;

				break;
			case None:
				//Do nothing
				break;
			}
		}
		
		double f1 = s1.getCost() //g
				+ minDistance1 * costPerKm; // + h
		double f2 = s2.getCost() //g
				+ minDistance2 * costPerKm; // + h
		return Double.compare(f1, f2);
	}

}
