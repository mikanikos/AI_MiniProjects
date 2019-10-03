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

public class GreedyAgent implements ReactiveBehavior {

    private int numActions;
    private Agent myAgent;

    @Override
    public void setup(Topology topology, TaskDistribution td, Agent agent) {

        this.numActions = 0;
        this.myAgent = agent;
    }

    @Override
    public Action act(Vehicle vehicle, Task availableTask) {
        Action action;

        if (availableTask == null) {
            City currentCity = vehicle.getCurrentCity();

            City closestCity = currentCity.neighbors().get(0);
            for (City c : currentCity.neighbors()) {
                if (currentCity.distanceTo(c) < currentCity.distanceTo(closestCity)) {
                    closestCity = c;
                }
            }
            action = new Move(closestCity);
        } else {
            action = new Pickup(availableTask);
        }

        if (numActions >= 1) {
            System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
        }
        numActions++;

        return action;
    }
}
