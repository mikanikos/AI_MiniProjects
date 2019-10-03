import java.util.*;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class Reactive implements ReactiveBehavior {

    private Random random;
    private double pPickup;
    private int numActions;
    private Agent myAgent;
    private Map<AgentState, City> bestStateAction;


    @Override
    public void setup(Topology topology, TaskDistribution td, Agent agent) {

        // Reads the discount factor from the agents.xml file.
        // If the property is not present it defaults to 0.95
        Double discount = agent.readProperty("discount-factor", Double.class,
                0.95);

        this.random = new Random();
        this.pPickup = discount;
        this.numActions = 0;
        this.myAgent = agent;

        List<AgentState> states = new ArrayList<>();
        Map<AgentState, Double> vectorV = new HashMap<>();
        Map<AgentState, Double> prevVectorV = new HashMap<>();
        bestStateAction = new HashMap<>();

        for (City c1 : topology.cities()) {
            states.add(new AgentState(c1));
            for (City c2 : topology.cities()) {
                if (!c1.equals(c2))
                    states.add(new AgentState(c1,c2));
            }
        }

        for (AgentState s : states) {
            vectorV.put(s, 0.0);
            prevVectorV.put(s, 0.0);
        }


        boolean running = true;
        double stop_threshold = 1E-8;

        while (running) {
            running = false;
            // iterate over all states
            for (AgentState s : states) {
                Set<City> actionsPerState= new HashSet<>();
                actionsPerState.addAll(s.getCurrentCity().neighbors());
                if (s.getDeliveryCity() != null)
                    actionsPerState.add(s.getDeliveryCity());
                // iterate over all actions
                for (City c : actionsPerState) {
                    double reward = - agent.vehicles().get(0).costPerKm() * s.getCurrentCity().distanceTo(c);
                    if (s.getDeliveryCity() != null && s.getDeliveryCity().equals(c))
                        reward += td.reward(s.getCurrentCity(), c);
                    double transition = 0.0;
                    for (AgentState nextState : states) {
                        if (nextState.getCurrentCity().equals(c)) {
                            transition += td.probability(nextState.getCurrentCity(), nextState.getDeliveryCity()) * vectorV.get(nextState).doubleValue();
                        }
                    }
                    // compute Q(s,a) and update V(s) at once
                    double qEntry = reward + discount * transition;
                    if (vectorV.get(s) < qEntry) {
                        prevVectorV.put(s, vectorV.get(s).doubleValue());
                        vectorV.put(s,qEntry);
                        bestStateAction.put(s, c);
                    }
                }
            }
            for (AgentState s : vectorV.keySet()) {
                if (vectorV.get(s).doubleValue() - prevVectorV.get(s).doubleValue() > stop_threshold) {
                    running = true;
                }
            }
        }
    }

    @Override
    public Action act(Vehicle vehicle, Task availableTask) {
        Action action;
        AgentState state;

        if (availableTask != null) {
            state = new AgentState(vehicle.getCurrentCity(), availableTask.deliveryCity);
        } else {
            state = new AgentState(vehicle.getCurrentCity());
        }

        City c = bestStateAction.get(state);
        if (availableTask != null && c.equals(availableTask.deliveryCity)) {
            action = new Pickup(availableTask);
        } else {
            action = new Move(c);
        }

        if (numActions >= 1) {
            System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
        }
        numActions++;

        return action;
    }

    class AgentState {

        private City currentCity;
        private City deliveryCity;

        public City getCurrentCity() {
            return currentCity;
        }

        public City getDeliveryCity() {
            return deliveryCity;
        }
        
        public AgentState(City currentCity, City deliveryCity) {
            this.currentCity = currentCity;
            this.deliveryCity = deliveryCity;
        }

        public AgentState(City currentCity) {
            this.currentCity = currentCity;
            this.deliveryCity = null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AgentState state = (AgentState) o;
            return currentCity.equals(state.currentCity) &&
                    Objects.equals(deliveryCity, state.deliveryCity);
        }

        @Override
        public int hashCode() {
            return Objects.hash(currentCity, deliveryCity);
        }

    }

}
