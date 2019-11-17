import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class AuctionAgent implements AuctionBehavior{
	private Topology topology;
	private TaskDistribution distribution;
	private Random random;
	private Agent agent;

	private Map<Integer, SLSSolution> currentSolutions;
	private Map<Integer, SLSSolution> possibleSolutions;
	private Map<Integer, Long> availableMoney;
	private Map<Integer, Double> predictionError;
	private Map<Integer, Double> estimatedBid;

	private boolean isFirstRound = true;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		this.distribution = distribution;
		this.topology = topology;
		this.agent = agent;

		long seed = -9019554669489983951L * agent.id();
		this.random = new Random(seed);

		currentSolutions = new HashMap<Integer, SLSSolution>();
		possibleSolutions = new HashMap<Integer, SLSSolution>();
		availableMoney = new HashMap<Integer, Long>();
		predictionError = new HashMap<Integer, Double>();
		estimatedBid = new HashMap<Integer, Double>();

		currentSolutions.put(agent.id(), new SLSSolution(agent.vehicles()));
		availableMoney.put(agent.id(), 0L);
		predictionError.put(agent.id(), 1.0);
	}

	@Override
	public Long askPrice(Task task) {

		double highestCostPerKm = 0;
		for(Vehicle v : agent.vehicles()) {
			highestCostPerKm += v.costPerKm();
		}

		double taskInterest = 0.0;
		double rewardExpectationAfterTask = 0.0;
		for(City c : topology.cities()) {
			double reward = distribution.reward(task.deliveryCity, c) - task.deliveryCity.distanceTo(c) * highestCostPerKm;
			rewardExpectationAfterTask += distribution.probability(task.deliveryCity, c) * reward;

			taskInterest += distribution.probability(c, task.pickupCity);
		}

		double myMarginalCost = 0;
		double minEnemyMarginalCost = 0;

		for(Entry<Integer, SLSSolution> entry : currentSolutions.entrySet()) {
			SLSSolution possibleSolution = SLS.addTaskInSolution(entry.getValue(), task);
			possibleSolution = SLS.Solve(possibleSolution, random);

			possibleSolutions.put(entry.getKey(), possibleSolution);

			double marginalCost = possibleSolution.getCost() - entry.getValue().getCost();
			//marginalCost *= predictionError.get(entry.getKey());
			
			if(entry.getKey() != agent.id()) {
				System.out.println("Agent " + entry.getKey() + " estimate " + marginalCost);
				if(marginalCost > 0) {
					estimatedBid.put(entry.getKey(), marginalCost);
					if(marginalCost < minEnemyMarginalCost || minEnemyMarginalCost == 0) {
						minEnemyMarginalCost = marginalCost;
					}
				}else {
					estimatedBid.put(entry.getKey(), -1.0);
				}
			}else {
				myMarginalCost = marginalCost;
			}
		}

		if(!isFirstRound) { // When has information about enemy (no first round)
			if(minEnemyMarginalCost <= myMarginalCost) {
				double marginalCostDif = myMarginalCost - minEnemyMarginalCost - 1; // Minus 1 to be best

				if(rewardExpectationAfterTask > marginalCostDif) { //If can be interesting to get the task for potential next task
					if(availableMoney.get(agent.id()) > marginalCostDif) { //If has save enough money can afford to earn less this time
						myMarginalCost -= marginalCostDif;
					}
				}
			}else { //Try to get has much money has possible
				myMarginalCost = minEnemyMarginalCost - 1;
			}
		}

		long bid = (long)Math.ceil(myMarginalCost);

		if(bid < 0)
			return null;

		return bid;
	}

	@Override
	public void auctionResult(Task lastTask, int lastWinner, Long[] lastOffers) {
		if(isFirstRound) {
			SLSSolution initialSolution = new SLSSolution(agent.vehicles());

			for(int id = 0; id < lastOffers.length; ++id) {
				currentSolutions.put(id, initialSolution);
				availableMoney.put(id, 0L);
				estimatedBid.put(id, -1.0);
				predictionError.put(id, 1.0);
			}
			currentSolutions.put(lastWinner, possibleSolutions.get(agent.id()));

			isFirstRound = false;
		}else {
			currentSolutions.put(lastWinner, possibleSolutions.get(lastWinner));
		}
		if(lastWinner == agent.id())
			System.out.println("Agent " + lastWinner + " win task " + lastTask.id);

		System.out.println("Agent " + (1 - agent.id()) + " offer " + lastOffers[1 - agent.id()] + " estimated " + estimatedBid.get(1 - agent.id()) + " error " + predictionError.get(1 - agent.id()));

		//Update winner money
		Long winnerMoney = availableMoney.get(lastWinner);
		winnerMoney += lastOffers[lastWinner];
		availableMoney.put(lastWinner, winnerMoney);
		
		for(int id = 0; id < lastOffers.length; ++id) {
			if(id == agent.id()) continue; // Should stay 1 for agent since no error
			if(lastOffers[id] == null) continue; // error should stay same (otherwise infinite error)
			if(estimatedBid.get(id) < 0) continue; // error should stay same (otherwise infinite error)
			
			double error = lastOffers[id] / estimatedBid.get(id);
			predictionError.put(id, error);
		}
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		Map<Integer, Task> intToTask = new HashMap<Integer, Task>();
		for(Task task : tasks) {
			intToTask.put(task.id, task);
		}
		return currentSolutions.get(agent.id()).getPlans(intToTask);
	}
}
