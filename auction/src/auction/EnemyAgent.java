package auction;

import centralized.SLS;
import centralized.SLSSolution;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;

import java.util.ArrayList;
import java.util.List;

public class EnemyAgent {

    private List<Vehicle> vehicles;
    private List<Task> tasksWon;
    private SLSSolution currentSolution;
    private double currentMarginalCost;
    private List<Long> bidHistory;

    public double getCurrentMarginalCost() {
        return currentMarginalCost;
    }

    public List<Task> getTasksWon() {
        return tasksWon;
    }


    public EnemyAgent(List<Vehicle> vehicles) {
        this.vehicles = new ArrayList<>(vehicles);
        this.tasksWon = new ArrayList<>();
        this.currentSolution = new SLSSolution(this.vehicles);
        this.bidHistory = new ArrayList<>();
    }


    public void computeMarginalCost(Task task, long timeout, double factor) {
        ArrayList<Task> enemyTasks = new ArrayList<>(tasksWon);
        enemyTasks.add(task);

        SLSSolution otherSolution = SLS.Solve(this.vehicles, enemyTasks, timeout);
        this.currentMarginalCost = otherSolution.getCost(this.vehicles) - this.currentSolution.getCost(this.vehicles) * (1 + factor);
    }

    public void addBid(Long bid) {
        this.bidHistory.add(bid);
    }

}
