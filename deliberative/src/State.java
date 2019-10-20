import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.*;

public class State {

    private Topology.City currentCity;
    private TaskSet tasksTaken;
    private TaskSet tasksLeft;
    private Plan plan;
    private Vehicle vehicle;

    public Topology.City getCurrentCity() {
        return currentCity;
    }

    public TaskSet getTasksTaken() {
        return tasksTaken;
    }

    public TaskSet getTasksLeft() {
        return tasksLeft;
    }

    public Plan getPlan() {
        return plan;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public State(Topology.City currentCity, TaskSet tasksTaken, TaskSet tasksLeft, Plan plan, Vehicle vehicle) {
        this.currentCity = currentCity;
        this.tasksTaken = tasksTaken;
        this.tasksLeft = tasksLeft;
        this.plan = plan;
        this.vehicle = vehicle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return Objects.equals(currentCity, state.currentCity) &&
                Objects.equals(tasksTaken, state.tasksTaken) &&
                Objects.equals(tasksLeft, state.tasksLeft);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentCity, tasksTaken, tasksLeft);
    }

    public State getStateCopy() {
        Plan planCopy = new Plan(this.vehicle.homeCity());
        for (Action a : this.plan) {
            planCopy.append(a);
        }
        return new State(this.vehicle.getCurrentCity(), TaskSet.copyOf(this.tasksTaken), TaskSet.copyOf(this.tasksLeft), planCopy, this.getVehicle());

    }

    public List<State> getSuccessiveStates() {
        List<State> successors = new ArrayList<>();
        successors.addAll(computeNextStates(tasksLeft, true));
        successors.addAll(computeNextStates(tasksTaken, false));
        return successors;
    }

    public List<State> computeNextStates(TaskSet tasks, boolean isPickup) {

        List<State> successors = new ArrayList<>();

        for (Task t : tasks) {

            // create next state from the current one with its history
            State newState = getStateCopy();

            Topology.City targetCity;
            if (isPickup) {
                targetCity = t.pickupCity;
            } else {
                targetCity = t.deliveryCity;
            }

            List<Topology.City> pathToTarget = currentCity.pathTo(targetCity);
            pathToTarget.add(0, this.currentCity);

            // go through all the cities from the current one to the target one
            for (Topology.City c : pathToTarget) {

                if (!c.equals(this.currentCity))
                    newState.getPlan().appendMove(c);

                // if there's a task to deliver on the way, deliver it
                for (Task t1 : this.tasksTaken) {
                    if (t1.deliveryCity.equals(c)) {
                        newState.getPlan().appendDelivery(t1);
                        newState.getTasksTaken().remove(t1);
                    }
                }

                // if there's a task to pick up on the way and there's space available, pick it up
                for (Task t1 : this.tasksLeft) {
                    if (t1.pickupCity.equals(c) && newState.getVehicle().capacity() >= newState.getTasksTaken().weightSum() + t1.weight) {
                        newState.getPlan().appendPickup(t1);
                        newState.getTasksLeft().remove(t1);
                        newState.getTasksTaken().add(t1);
                    }
                }
            }
            //System.out.println(newState. + "/" + vehicle.capacity());
            newState.currentCity = targetCity;
            successors.add(newState);
        }
        return successors;

    }
}
