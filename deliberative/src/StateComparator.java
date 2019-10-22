import logist.task.Task;

import java.util.*;

/**
 * A comparator for the state of the A-star algorithm.
 * @author Piccione Andrea, Juppet Quentin
 */
public class StateComparator implements Comparator<State> {
    @Override
    public int compare(State s1, State s2) {

        double totalCost1 = (heuristicEstimate(s1) + s1.getPlan().totalDistance()) * s1.getVehicle().costPerKm();
        double totalCost2 = (heuristicEstimate(s2) + s2.getPlan().totalDistance()) * s2.getVehicle().costPerKm();

        //System.out.println(totalCost1 + " " + totalCost2);

        return Double.compare(totalCost1, totalCost2);
    }


    // using distance metric as heuristic function
    private double heuristicEstimate(State s) {
        double minimumDistance = 0;
        
        //For taken task we need at least to go the delivery city
        for (Task t : s.getTasksTaken()) {
        	double curDistance = s.getCurrentCity().distanceTo(t.deliveryCity);
            if(curDistance > minimumDistance) {
            	minimumDistance = curDistance;
            }
        }
        
        //For task left, we need at least to go to pick it up and then deliver it
        for (Task t : s.getTasksLeft()) {
        	double curDistance = s.getCurrentCity().distanceTo(t.pickupCity);
        	curDistance += t.pathLength(); //Minimum path between delivery and pickup city
            if(curDistance > minimumDistance) {
            	minimumDistance = curDistance;
            }
        }

        return minimumDistance;
    }

}
