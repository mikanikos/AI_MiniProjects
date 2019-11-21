import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import logist.LogistPlatform;
import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * The auction agent
 * @author Piccione Andrea, Juppet Quentin
 */
public class AuctionAgent implements AuctionBehavior{

	private static final double PREDICTION_ERROR_MEAN_NUMBER = 5;
	private static final double FIRST_ROUNDS_LIMIT = 5;
	private static final double SAFETY_BID_FROM_ENNEMY_FIRST_ROUNDS = 0.75;
	private static final double SAFETY_BID_FROM_ENNEMY = 0.9;
	private static final double TASK_INTEREST_PROBA_THRESHOLD = 0.09;
	private static final double TASK_INTEREST_REDUCE_FACTOR = 0.8;
	private static final double ERROR_OUTLIER_VALUE = 0.33;

	private Topology topology;
	private TaskDistribution distribution;
	private Random random;
	private Agent agent;

	private Map<Integer, SLSSolution> currentSolutions;
	private Map<Integer, SLSSolution> possibleSolutions;
	private Map<Integer, Long> availableMoney;
	private List<Double> ennemyPredictionError;
	private double meanEnnemyPredictionError = 1.0;
	private double ennemyEstimatedBid;
	private int ennemyId;
	private double highestCostPerKm;
	private int currentRound = 0;
	
	private long minimumEnemyBid = 500;
	
	private long bidTimeout;
	private long planTimeout;

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
		ennemyPredictionError = new ArrayList<Double>();

		bidTimeout = LogistPlatform.getSettings().get(LogistSettings.TimeoutKey.BID);
		planTimeout = LogistPlatform.getSettings().get(LogistSettings.TimeoutKey.PLAN);
		
		SLSSolution initialSolution = new SLSSolution(agent.vehicles());
		
		ennemyId = 1 - agent.id();
		
		for(int id = 0; id <= 1; ++id) {
			currentSolutions.put(id, initialSolution);
			availableMoney.put(id, 0L);
		}

		highestCostPerKm = 0;
		for(Vehicle v : agent.vehicles()) {
			if(v.costPerKm() > highestCostPerKm)
				highestCostPerKm = v.costPerKm();
		}

	}

	@Override
	public Long askPrice(Task task) {
		double myMarginalCost = 0;
		double ennemyMarginalCost = 0;

		for(int id = 0; id <= 1; ++id) {
			SLSSolution currentSolution = currentSolutions.get(id);
			SLSSolution possibleSolution = SLS.addTaskInSolution(currentSolution, task);
			possibleSolution = SLS.Solve(possibleSolution, random, bidTimeout / 2);

			possibleSolutions.put(id, possibleSolution);
			
			double marginalCost = possibleSolution.getCost() - currentSolution.getCost();
			
			double maxProbInterest = 0.0;
			for(Task t : currentSolution.getTasks()) {
				double p = distribution.probability(t.deliveryCity, task.pickupCity);
				if(p > maxProbInterest)
					maxProbInterest = p;
			}

			if(maxProbInterest > TASK_INTEREST_PROBA_THRESHOLD) {
				marginalCost *= TASK_INTEREST_REDUCE_FACTOR;
			}
			
			if(id == ennemyId) {
				ennemyMarginalCost = marginalCost;
				ennemyMarginalCost *= meanEnnemyPredictionError;

				if(ennemyMarginalCost < minimumEnemyBid)
					ennemyMarginalCost = minimumEnemyBid;
				
				if(ennemyMarginalCost > 0) {
					ennemyEstimatedBid = marginalCost;
				}else {
					ennemyEstimatedBid = -1;
				}
			}else {
				myMarginalCost = marginalCost;
			}
		}
		
		System.out.println("Task from " + task.pickupCity + " and to " + task.deliveryCity);
		System.out.println("My marginal cost " + myMarginalCost + " and estimate " + ennemyMarginalCost);

		if(ennemyMarginalCost <= myMarginalCost && ennemyEstimatedBid > 0) {
			double marginalCostDif = myMarginalCost - (ennemyMarginalCost  * SAFETY_BID_FROM_ENNEMY);
			
			double moneyDif = (availableMoney.get(agent.id()) - currentSolutions.get(agent.id()).getCost())
							- (availableMoney.get(ennemyId) - currentSolutions.get(ennemyId).getCost());
			
			if(moneyDif > marginalCostDif) {
				myMarginalCost -= marginalCostDif;
			}
		}else { //Try to get has much money has possible
			double newMarginalCost;
			
			if(currentRound < FIRST_ROUNDS_LIMIT) {
				newMarginalCost = ennemyMarginalCost * SAFETY_BID_FROM_ENNEMY_FIRST_ROUNDS;
			}else {
				newMarginalCost = ennemyMarginalCost * SAFETY_BID_FROM_ENNEMY;
			}
			
			if(newMarginalCost > myMarginalCost)
				myMarginalCost = newMarginalCost;
		}

		long bid = (long)Math.ceil(myMarginalCost);

		if(bid <= 0)
			bid = 1;
		
		return bid;
	}

	@Override
	public void auctionResult(Task lastTask, int lastWinner, Long[] lastOffers) {
		System.out.println("Winner is " + lastWinner + " my bid " + lastOffers[agent.id()] + " ennemy bid " + lastOffers[ennemyId]);
		currentSolutions.put(lastWinner, possibleSolutions.get(lastWinner));

		//Update winner money
		Long winnerMoney = availableMoney.get(lastWinner);
		winnerMoney += lastOffers[lastWinner];
		availableMoney.put(lastWinner, winnerMoney);
		
		
		if(lastOffers[ennemyId] < minimumEnemyBid)
			minimumEnemyBid = lastOffers[ennemyId];
		
		//Update ennemy error
		if(ennemyEstimatedBid > 0) {
			double error = lastOffers[ennemyId] / ennemyEstimatedBid;
			
			if(error > ERROR_OUTLIER_VALUE && error < 1/ERROR_OUTLIER_VALUE)
				ennemyPredictionError.add(error);
			
			if(ennemyPredictionError.size() > PREDICTION_ERROR_MEAN_NUMBER) {
				ennemyPredictionError.remove(0);
			}
			
			meanEnnemyPredictionError = 0.0;
			for(Double e : ennemyPredictionError) {
				meanEnnemyPredictionError += e;
			}
			meanEnnemyPredictionError /= ennemyPredictionError.size();
		}
		
		++currentRound;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		Map<Integer, Task> intToTask = new HashMap<Integer, Task>();
		for(Task task : tasks) {
			intToTask.put(task.id, task);
		}
		
		SLSSolution finalSolution = SLS.Solve(currentSolutions.get(agent.id()), random, planTimeout);
		
		return finalSolution.getPlans(intToTask);
	}
}
