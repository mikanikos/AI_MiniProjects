import java.util.List;

import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;

/**
 * Class that implements the centralized agent
 * @author Piccione Andrea, Juppet Quentin
 */
public class CentralizedAgent implements CentralizedBehavior{

    @Override
    public void setup(Topology topology, TaskDistribution distribution,
            Agent agent) {
    }

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		return SLS.Solve(vehicles, tasks);
	}

}
