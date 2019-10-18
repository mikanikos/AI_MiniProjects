import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.util.SimUtilities;
import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.analysis.BinDataSource;
import uchicago.src.sim.analysis.OpenHistogram;


/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {

	// Default Values
	private static final int GRID_SIZE = 20;
	private static final int NUM_INIT_RABBITS = 150;
	private static final int NUM_INIT_GRASS = 100;
	private static final int GRASS_GROWTH_RATE = 50;
	private static final int BIRTH_THRESHOLD = 50;
	private static final int RABBIT_INIT_ENERGY = 30;

	private int gridSize = GRID_SIZE;
	private int numInitRabbits = NUM_INIT_RABBITS;
	private int numInitGrass = NUM_INIT_GRASS;
	private int grassGrowthRate = GRASS_GROWTH_RATE;
	private int birthThreshold = BIRTH_THRESHOLD;
	// Optional parameter, if set lower or equal to 0 a random amount of energy is created for each created rabbit
	private int rabbitInitEnergy = RABBIT_INIT_ENERGY;

	private Schedule schedule;
	private RabbitsGrassSimulationSpace grassSpace;
	private DisplaySurface displaySurface;
	private ArrayList rabbitList;
	private OpenSequenceGraph countParametersInSpace;
	private OpenHistogram agentWealthDistribution;

	class grassInSpace implements DataSource, Sequence {

		public Object execute() {
			return new Double(getSValue());
		}

		public double getSValue() {
			return (double) grassSpace.getTotalGrass();
		}
	}

	class rabbitsInSpace implements DataSource, Sequence {

		public Object execute() {
			return new Double(getSValue());
		}

		public double getSValue() {
			return (double) countLivingRabbits();
		}
	}

	class agentEnergy implements BinDataSource{
		public double getBinValue(Object o) {
			RabbitsGrassSimulationAgent agent = (RabbitsGrassSimulationAgent)o;
			return (double)agent.getEnergy();
		}
	}

	public int getGridSize() {
		return gridSize;
	}
	public void setGridSize(int gridSize) {
		this.gridSize = gridSize;
	}

	public int getNumInitRabbits() {
		return numInitRabbits;
	}
	public void setNumInitRabbits(int numInitRabbits) {
		this.numInitRabbits = numInitRabbits;
	}

	public int getNumInitGrass() {
		return numInitGrass;
	}
	public void setNumInitGrass(int numInitGrass) {
		this.numInitGrass = numInitGrass;
	}

	public int getGrassGrowthRate() {
		return grassGrowthRate;
	}
	public void setGrassGrowthRate(int grassGrowthRate) {
		this.grassGrowthRate = grassGrowthRate;
	}

	public int getBirthThreshold() {
		return birthThreshold;
	}
	public void setBirthThreshold(int birthThreshold) {
		this.birthThreshold = birthThreshold;
	}

	public int getRabbitInitEnergy() { return rabbitInitEnergy; }
	public void setRabbitInitEnergy(int rabbitInitEnergy) { this.rabbitInitEnergy = rabbitInitEnergy; }


    public String getName() {
        return "Rabbit Grass Simulation";
    }

	public Schedule getSchedule() {
		return schedule;
	}

	public String[] getInitParam() {
		// Parameters to be set by users via the Repast UI slider bar
		// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want
		String[] params = {"GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold", "RabbitInitEnergy"};
		return params;
	}

    public void setup() {
		System.out.println("Running setup");

		grassSpace = null;
		rabbitList = new ArrayList();

		schedule = new Schedule(1);

		if (displaySurface != null) {
			displaySurface.dispose();
		}
		displaySurface = null;

		if (countParametersInSpace != null){
			countParametersInSpace.dispose();
		}
		countParametersInSpace = null;

		if (agentWealthDistribution != null){
			agentWealthDistribution.dispose();
		}
		agentWealthDistribution = null;

		displaySurface = new DisplaySurface(this, "Rabbit Grass Simulation Window 1");
		countParametersInSpace = new OpenSequenceGraph("Amount Of Grass and Rabbits In Space",this);
		agentWealthDistribution = new OpenHistogram("Rabbit Energy", 8, 0);

		registerDisplaySurface("Rabbit Grass Simulation Window 1", displaySurface);
		this.registerMediaProducer("Plot", countParametersInSpace);
    }

    public void begin() {
        buildModel();
        buildSchedule();
        buildDisplay();

		displaySurface.display();
		countParametersInSpace.display();
		agentWealthDistribution.display();
    }

    public void buildModel() {
		System.out.println("Running BuildModel");
		grassSpace = new RabbitsGrassSimulationSpace(gridSize);

		for (int i = 0; i < numInitRabbits; i++) {
			addNewRabbit();
		}

		grassSpace.spreadGrass(numInitGrass);

		for (int i = 0; i < rabbitList.size(); i++) {
			RabbitsGrassSimulationAgent rabbit = (RabbitsGrassSimulationAgent) rabbitList.get(i);
			rabbit.report();
		}
    }

    public void buildSchedule() {
		System.out.println("Running BuildSchedule");

		class RabbitsGrassSimulationStep extends BasicAction {
			public void execute() {
				SimUtilities.shuffle(rabbitList);
				for (int i = 0; i < rabbitList.size(); i++){
					RabbitsGrassSimulationAgent rabbit = (RabbitsGrassSimulationAgent)rabbitList.get(i);
					rabbit.step();
				}

				removeDeadRabbits();
				addNewBornRabbits();
				grassSpace.spreadGrass(grassGrowthRate);

				displaySurface.updateDisplay();
			}
		}
		schedule.scheduleActionBeginning(0, new RabbitsGrassSimulationStep());


		class RabbitsGrassSimulationCountLiving extends BasicAction {
			public void execute(){
				countLivingRabbits();
			}
		}
		schedule.scheduleActionAtInterval(10, new RabbitsGrassSimulationCountLiving());


		class RabbitsGrassSimulationUpdateGrassInSpace extends BasicAction {
			public void execute(){
				countParametersInSpace.step();
			}
		}
		schedule.scheduleActionAtInterval(10, new RabbitsGrassSimulationUpdateGrassInSpace());


		class RabbitsGrassSimulationUpdateAgentWealth extends BasicAction {
			public void execute(){
				agentWealthDistribution.step();
			}
		}
		schedule.scheduleActionAtInterval(10, new RabbitsGrassSimulationUpdateAgentWealth());
    }

    public void buildDisplay() {
		System.out.println("Running BuildDisplay");

		ColorMap map = new ColorMap();

		// different green variations of a grass cell corresponds to different amount of energy
		for(int i = 1; i<16; i++){
			map.mapColor(i, new Color(0, (int)(i * 8 + 127), 0));
		}
		map.mapColor(0, Color.black);

		Value2DDisplay displayGrass = new Value2DDisplay(grassSpace.getCurrentGrassSpace(), map);
		Object2DDisplay displayRabbits = new Object2DDisplay(grassSpace.getCurrentRabbitSpace());
		displayRabbits.setObjectList(rabbitList);

		displaySurface.addDisplayableProbeable(displayGrass, "Grass");
		displaySurface.addDisplayableProbeable(displayRabbits, "Rabbits");

		countParametersInSpace.addSequence("Grass In Space", new grassInSpace());
		countParametersInSpace.addSequence("Rabbits In Space", new rabbitsInSpace());
		agentWealthDistribution.createHistogramItem("Rabbit Energy",rabbitList,new agentEnergy());
	}

	private void addNewRabbit(){
		RabbitsGrassSimulationAgent agent = new RabbitsGrassSimulationAgent(rabbitInitEnergy);
		rabbitList.add(agent);
		grassSpace.addRabbit(agent);
	}

	private void removeDeadRabbits(){
		for (int i = (rabbitList.size() - 1); i >= 0 ; i--) {
			RabbitsGrassSimulationAgent agent = (RabbitsGrassSimulationAgent)rabbitList.get(i);
			if (agent.getEnergy() < 1) {
				grassSpace.removeRabbitAt(agent.getX(), agent.getY());
				rabbitList.remove(i);
			}
		}
	}

	private void addNewBornRabbits(){
		for (int i = (rabbitList.size() - 1); i >= 0 ; i--) {
			RabbitsGrassSimulationAgent agent = (RabbitsGrassSimulationAgent)rabbitList.get(i);
			if (agent.getEnergy() > birthThreshold) {
				addNewRabbit();
				agent.setEnergy(agent.getEnergy() / 2);
			}
		}
	}

	private int countLivingRabbits(){
		int livingRabbits = 0;
		for (int i = 0; i < rabbitList.size(); i++) {
			RabbitsGrassSimulationAgent rabbit = (RabbitsGrassSimulationAgent)rabbitList.get(i);
			if (rabbit.getEnergy() > 0)
				livingRabbits++;
		}
		System.out.println("Number of living rabbits is: " + livingRabbits);

		return livingRabbits;
	}

	public static void main(String[] args) {

    	System.out.println("Rabbit skeleton");

		SimInit init = new SimInit();
		RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
		// Do "not" modify the following lines of parsing arguments
		if (args.length == 0) // by default, you don't use parameter file nor batch mode
			init.loadModel(model, "", false);
		else
			init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));

	}

}
