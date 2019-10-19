

/* import table */

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeAgent implements DeliberativeBehavior {

    enum Algorithm {BFS, ASTAR}

    /* Environment */
    Topology topology;
    TaskDistribution td;

    /* the properties of the agent */
    Agent agent;
    int capacity;

    /* the planning class */
    Algorithm algorithm;

    @Override
    public void setup(Topology topology, TaskDistribution td, Agent agent) {
        this.topology = topology;
        this.td = td;
        this.agent = agent;

        // initialize the planner
        int capacity = agent.vehicles().get(0).capacity();
        String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");

        // Throws IllegalArgumentException if algorithm is unknown
        algorithm = Algorithm.valueOf(algorithmName.toUpperCase());

    }

    @Override
    public Plan plan(Vehicle vehicle, TaskSet tasks) {
        Plan plan;

        long start, end;

        // Compute the plan with the selected algorithm.
        switch (algorithm) {
            case ASTAR:
                System.out.println("ASTAR");
                start = System.currentTimeMillis();
                plan = aStarPlan(vehicle, tasks);
                end = System.currentTimeMillis();
                break;
            case BFS:
                System.out.println("BFS");
                start = System.currentTimeMillis();
                plan = bfsPlan(vehicle, tasks);
                end = System.currentTimeMillis();
                break;
            default:
                throw new AssertionError("Should not happen.");
        }
        System.out.println((end-start + " milliseconds to generate the plan"));
        return plan;
    }


	private Plan aStarPlan(Vehicle vehicle, TaskSet tasks) {
		State initialState = new State(vehicle.getCurrentCity(), vehicle.getCurrentTasks(), tasks, new Plan(vehicle.getCurrentCity()), vehicle);
		Queue<State> queue = new PriorityQueue<>(new StateComparator());

		queue.add(initialState);

		return search(queue);
	}

    private Plan bfsPlan(Vehicle vehicle, TaskSet tasks) {
        State initialState = new State(vehicle.getCurrentCity(), vehicle.getCurrentTasks(), tasks, new Plan(vehicle.getCurrentCity()), vehicle);
        Queue<State> queue = new ArrayDeque<>();

        queue.add(initialState);

        return search(queue);
    }

    private Plan search(Queue<State> queue) {

		List<State> visited = new ArrayList<>();

		State n;
		while ((n = queue.poll()) != null) {
			if (n.getTasksLeft().isEmpty() && n.getTasksTaken().isEmpty()) {
				return n.getPlan();
			}

			boolean condition = true;
			if (algorithm == Algorithm.ASTAR) {
                State finalN = n;
                List<State> nStates = visited.stream().filter(x -> x.equals(finalN)).collect(Collectors.toList());
                if (!nStates.isEmpty()) {
                    State minimumState = Collections.min(nStates, new StateComparator());
                    condition = n.getPlan().totalDistance() < minimumState.getPlan().totalDistance();
                }
			}

            if (!visited.contains(n) && condition) {
				visited.add(n);
				queue.addAll(n.getSuccessiveStates());
			}
			//System.out.println(n.getTasksLeft().size() + " " + n.getTasksTaken().size());

		}

		throw new IllegalStateException("Queue empty before computing optimal plan");

	}


    private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        for (Task task : tasks) {
            // move: current city => pickup location
            for (City city : current.pathTo(task.pickupCity))
                plan.appendMove(city);

            plan.appendPickup(task);

            // move: pickup location => delivery location
            for (City city : task.path())
                plan.appendMove(city);

            plan.appendDelivery(task);

            // set current city
            current = task.deliveryCity;
        }
        return plan;
    }

    @Override
    public void planCancelled(TaskSet carriedTasks) {

        if (!carriedTasks.isEmpty()) {
            // This cannot happen for this simple agent, but typically
            // you will need to consider the carriedTasks when the next
            // plan is computed.
        }
    }
}
