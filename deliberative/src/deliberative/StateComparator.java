package deliberative;

import logist.task.Task;
import logist.topology.Topology;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class StateComparator implements Comparator<State> {
    @Override
    public int compare(State s1, State s2) {

        double totalCost1 = (heuristicEstimate(s1) + s1.getPlan().totalDistance()) * s1.getVehicle().costPerKm();
        double totalCost2 = (heuristicEstimate(s2) + s2.getPlan().totalDistance()) * s2.getVehicle().costPerKm();

        return Double.compare(totalCost1, totalCost2);
    }


    // using distance metric as heuristic function
    private double heuristicEstimate(State s) {

        Set<Topology.City> citiesLeft = new HashSet<>();

        for (Task t : s.getTasksTaken()) {
            citiesLeft.add(t.deliveryCity);
        }
        for (Task t : s.getTasksLeft()) {
            citiesLeft.add(t.deliveryCity);
            citiesLeft.add(t.pickupCity);
        }

        Topology.City currentCity = s.getCurrentCity();
        Topology.City targetCity = null;

        double estimateDistance = 0;

        while (!citiesLeft.isEmpty()) {
            double minimumDistance = Double.MAX_VALUE;
            for (Topology.City c : citiesLeft) {
                double distance = currentCity.distanceTo(c);
                if (distance < minimumDistance) {
                    minimumDistance = distance;
                    targetCity = c;
                }
            }
            currentCity = targetCity;
            citiesLeft.remove(targetCity);
            estimateDistance += minimumDistance;
        }

        return estimateDistance;
    }

}
