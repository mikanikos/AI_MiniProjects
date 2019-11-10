package auction;

//the list of imports

import java.util.*;
import java.util.stream.Collectors;

import centralized.SLS;
import centralized.SLSSolution;
import logist.LogistSettings;
import logist.Measures;
import logist.agent.AgentImpl;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.config.ParserException;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 */
@SuppressWarnings("unused")
public class AuctionAgent implements AuctionBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private Random random;
    private Vehicle vehicle;
    private City currentCity;

    private static final double INIT_FACTOR_DISCOUNT = 0.1;
    private static final double MARGINAL_COST_INCREASE_FACTOR = 0.2;
    private static final double PROB_THRESHOLD = 0.4;
    private static final int ROUND_THRESHOLD = 3;



    private List<Task> tasksWon;
    private Map<Integer, EnemyAgent> enemies;
    private long bidTimeout;
    private long planTimeout;
    private SLSSolution currentSolution;
    private SLSSolution possibleSolution;
    private List<Long> bidHistory;
    private int round;
    private double bidRate;
    private Map<PickupDelivery, Integer> expectedLoadBetweenCities;
    private Map<PickupDelivery, Double> expectedTaskProbability;
    private Map<City, Double> expectedProbabilityDeliveryIsPickup;

    @Override
    public void setup(Topology topology, TaskDistribution distribution,
                      Agent agent) {

        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
        this.vehicle = agent.vehicles().get(0);
        this.currentCity = vehicle.homeCity();

        long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
        this.random = new Random(seed);

        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config/settings_auction.xml");
        } catch (ParserException e) {
            e.printStackTrace();
        }

        assert ls != null;
        bidTimeout = ls.get(LogistSettings.TimeoutKey.BID);
        planTimeout = ls.get(LogistSettings.TimeoutKey.PLAN);

        this.tasksWon = new ArrayList<>();
        this.currentSolution = new SLSSolution(agent.vehicles());
        this.round = 0;
        this.bidRate = 1.0;
        this.bidHistory = new ArrayList<>();
        this.enemies = new HashMap<>();
        this.expectedLoadBetweenCities = new HashMap<>();
        this.expectedTaskProbability = new HashMap<>();
        this.expectedProbabilityDeliveryIsPickup = new HashMap<>();
    }

    private void analyzeDistribution(Topology topology, TaskDistribution distribution) {
        List<City> cities = topology.cities();

//        Map<PickupDelivery, Integer> expectedLoadBetweenCities = new HashMap<>();
//        Map<PickupDelivery, Double> expectedTaskProbability = new HashMap<>();
//        Map<City, Double> expectedProbabilityDeliveryIsPickup = new HashMap<>();
        double cityProbability = 1.0 / cities.size();

        for (City c1 : cities) {
            double probDeliveryPickup = 0;
            for (City c2 : cities) {
                PickupDelivery pair = new PickupDelivery(c1, c2);

                expectedLoadBetweenCities.put(pair, distribution.weight(c1, c2));
                expectedTaskProbability.put(pair, distribution.probability(c1, c2));

                probDeliveryPickup += distribution.probability(c2, c1) * cityProbability;
            }
            expectedProbabilityDeliveryIsPickup.put(c1, probDeliveryPickup);
        }

        //expectedLoadBetweenCities = expectedLoadBetweenCities.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, HashMap::new));
        //expectedTaskProbability = expectedTaskProbability.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, HashMap::new));
        //expectedProbabilityDeliveryIsPickup = expectedProbabilityDeliveryIsPickup.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, HashMap::new));
    }


    @Override
    public void auctionResult(Task previous, int winner, Long[] bids) {

        for (Long b : bids)
            System.out.println(b);


        for (int i = 0; i < bids.length; i++) {
            if (i != agent.id()) {
                if (round == 0) {
                    enemies.putIfAbsent(i, new EnemyAgent(agent.vehicles()));
                }
                enemies.get(i).addBid(bids[i]);
            } else {
                bidHistory.add(bids[i]);
            }
        }

        if (agent.id() == winner) {
            System.out.println("We won!!!");

            tasksWon.add(previous);
            // we won, increase request for next time
            this.bidRate += 0.1;

        } else {
            System.out.println("Winner round " + round + " : " + winner);

            enemies.get(winner).getTasksWon().add(previous);
            // we lost, decrease request for next time
            this.bidRate -= 0.1;
        }

        this.round++;
    }

    @Override
    public Long askPrice(Task task) {

        System.out.println("Asking price for task: " + task.id);

        // check if there's a vehicle which can take the task and take the less expensive one
        Vehicle selectedVehicle = this.vehicle;
        for (Vehicle v : this.agent.vehicles()) {
            if (task.weight <= v.capacity() && selectedVehicle.costPerKm() > v.costPerKm()) {
                selectedVehicle = v;
            }
        }

        // no vehicle found, so no bid
        if (task.weight > selectedVehicle.capacity()) {
            return null;
        }

        // compute marginal cost for opponents
        long planTimeout = bidTimeout;
        double bestEnemyBid = Double.MAX_VALUE;
        if (round != 0) {
            planTimeout /= (enemies.values().size() + 1);
            for (EnemyAgent e : enemies.values()) {
                System.out.println("Computing enemy plan");
                e.computeMarginalCost(task, planTimeout, MARGINAL_COST_INCREASE_FACTOR);
                double bid = e.getCurrentMarginalCost();
                if (bid < bestEnemyBid) {
                    bestEnemyBid = bid;
                }
            }
        }

        System.out.println("Computing my plan");

        List<Task> possibleTasks = new ArrayList<>(tasksWon);
        possibleTasks.add(task);

        SLSSolution solution = SLS.Solve(this.agent.vehicles(), possibleTasks, planTimeout);
        double marginal_cost = solution.getCost(this.agent.vehicles()) - currentSolution.getCost(this.agent.vehicles());

        // increase it a bit on order to make profit
        marginal_cost *= Math.min(1 + MARGINAL_COST_INCREASE_FACTOR, bidRate);

        // cost from pickup to delivery city (doesn't count moving to pickup)
        double minimalBid = task.pathLength() * selectedVehicle.costPerKm();

        // if first rounds or still need to get first tasks, try to ask for a low price so that I can get well started
        if (this.round + tasksWon.size() < ROUND_THRESHOLD * 2 || tasksWon.size() < this.round / 2) {
            marginal_cost -= marginal_cost * INIT_FACTOR_DISCOUNT;
        }

        // random trials just to look at the effects, still need to refine strategy
        if (expectedProbabilityDeliveryIsPickup.getOrDefault(task.deliveryCity, 0.0) > PROB_THRESHOLD) {
            marginal_cost -= marginal_cost * MARGINAL_COST_INCREASE_FACTOR;
        }

        if (expectedTaskProbability.getOrDefault(new PickupDelivery(task.pickupCity, task.deliveryCity), 0.0) > PROB_THRESHOLD) {
            marginal_cost -= marginal_cost * MARGINAL_COST_INCREASE_FACTOR;
        }

        if (bestEnemyBid < marginal_cost && tasksWon.size() < this.round / 2) {
            marginal_cost = minimalBid + marginal_cost * MARGINAL_COST_INCREASE_FACTOR;
        }

        if (this.round > 7 && getEnemyWithMoreTasksWon().getTasksWon().size() > tasksWon.size() + 5) {
            marginal_cost = minimalBid;
        }

        return (long) Math.max(marginal_cost, minimalBid);
    }

    public EnemyAgent getEnemyWithMoreTasksWon() {
        EnemyAgent top = null;
        int maxTasks = Integer.MIN_VALUE;

        for (EnemyAgent e : enemies.values()) {
            if (e.getTasksWon().size() > maxTasks) {
                maxTasks = e.getTasksWon().size();
                top = e;
            }
        }

        return top;

    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {

        System.out.println("Agent " + agent.id() + " has tasks " + tasks);

        List<Task> listTask = new ArrayList<>();
        for (Task t : tasks) {
            listTask.add(t);
        }

        return SLS.Solve(vehicles, listTask, this.planTimeout).getPlans(vehicles);
    }

    class PickupDelivery {

        private City pickup;
        private City delivery;

        public PickupDelivery(City pickup, City delivery) {
            this.pickup = pickup;
            this.delivery = delivery;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PickupDelivery that = (PickupDelivery) o;
            return Objects.equals(pickup, that.pickup) &&
                    Objects.equals(delivery, that.delivery);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pickup, delivery);
        }
    }
}
