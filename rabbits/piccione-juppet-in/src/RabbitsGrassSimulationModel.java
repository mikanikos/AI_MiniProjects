import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.reflector.RangePropertyDescriptor;
import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;
import uchicago.src.sim.engine.SimInit;

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author Piccione Andrea, Juppet Quentin
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {		

	private static int DEFAULT_GRID_SIZE = 20;
	private static int DEFAULT_NUM_INIT_RABBITS = 5;
	private static int DEFAULT_NUM_INIT_GRASS = 25;
	private static int DEFAULT_GRASS_GROWTH_RATE = 10;
	private static int DEFAULT_BIRTH_THRESHOLD = 75;

	private int gridSize = DEFAULT_GRID_SIZE;
	private int numInitRabbits = DEFAULT_NUM_INIT_RABBITS;
	private int numInitGrass = DEFAULT_NUM_INIT_GRASS;
	private int grassGrowthRate = DEFAULT_GRASS_GROWTH_RATE;
	private int birthThreshold = DEFAULT_BIRTH_THRESHOLD;

	private Schedule schedule;

	private RabbitsGrassSimulationSpace space;

	private DisplaySurface displaySurface;
	private OpenSequenceGraph amountOfRabbitAndGrass;

	private ArrayList<RabbitsGrassSimulationAgent> rabbitList;

	public static void main(String[] args) {
		SimInit init = new SimInit();
		RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
		// Do "not" modify the following lines of parsing arguments
		if (args.length == 0) // by default, you don't use parameter file nor batch mode 
			init.loadModel(model, "", false);
		else
			init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));
	}

	public String getName() {
		return "Rabbit grass simulation";
	}

	public void setup() {
		space = null;
		rabbitList = new ArrayList<>();
		schedule = new Schedule(1);

		if(displaySurface != null)
			displaySurface.dispose();
		displaySurface = new DisplaySurface(this, getName());
		registerDisplaySurface(displaySurface.getName(), displaySurface);
		
		if(amountOfRabbitAndGrass != null)
			amountOfRabbitAndGrass.dispose();
		amountOfRabbitAndGrass = new OpenSequenceGraph("Amount of rabbit and grass", this);
		registerMediaProducer(amountOfRabbitAndGrass.getTitle(), amountOfRabbitAndGrass);
		
		descriptors.put("GridSize", new RangePropertyDescriptor("GridSize", 0, 50, 10));
		descriptors.put("NumInitRabbits", new RangePropertyDescriptor("NumInitRabbits", 0, 100, 20));
		descriptors.put("NumInitGrass", new RangePropertyDescriptor("NumInitGrass", 0, 100, 20));
		descriptors.put("GrassGrowthRate", new RangePropertyDescriptor("GrassGrowthRate", 0, 50, 10));
		descriptors.put("BirthThreshold", new RangePropertyDescriptor("BirthThreshold", 0, 100, 20));
		descriptors.put("RabbitInitEnergy", new RangePropertyDescriptor("RabbitInitEnergy", 0, 100, 20));
		descriptors.put("EatGrassEnergy", new RangePropertyDescriptor("EatGrassEnergy", 0, 100, 20));
		descriptors.put("MoveEnergy", new RangePropertyDescriptor("MoveEnergy", 0, 100, 20));
		descriptors.put("StarvingEnergy", new RangePropertyDescriptor("StarvingEnergy", 0, 100, 20));
	}
	
	public void begin(){
		buildModel();
		buildSchedule();
		buildDisplay();

		displaySurface.display();
		amountOfRabbitAndGrass.display();
	}

	public void buildModel(){
		space = new RabbitsGrassSimulationSpace(gridSize);
		space.spreadGrass(numInitGrass);
		rabbitList.addAll(space.spreadRabbits(numInitRabbits));
	}

	public void buildSchedule(){
		class SimulationStep extends BasicAction {
			public void execute() {
				int nbOfBirth = 0;
				SimUtilities.shuffle(rabbitList);
				for(int i = 0; i < rabbitList.size(); i++){
					RabbitsGrassSimulationAgent rabbit = rabbitList.get(i);
					rabbit.step();

					if(rabbit.getEnergy() >= birthThreshold)
					{
						rabbit.addToEnergy(-birthThreshold);
						++nbOfBirth;
					}
					if(rabbit.getEnergy() < 0)
					{
						space.removeRabbitAt(rabbit.getX(), rabbit.getY());
						rabbitList.remove(rabbit);
						--i;
					}
				}

				space.spreadGrass(grassGrowthRate);
				rabbitList.addAll(space.spreadRabbits(nbOfBirth));

				displaySurface.updateDisplay();
			}
		}

		schedule.scheduleActionBeginning(0, new SimulationStep());

		class AmoutOfRabbitAndGrassChartStep extends BasicAction {
			public void execute(){
				amountOfRabbitAndGrass.step();
			}
		}

		schedule.scheduleActionAtInterval(10, new AmoutOfRabbitAndGrassChartStep());
	}
	

	public void buildDisplay(){
		ColorMap grassMap = new ColorMap();

		grassMap.mapColor(0, Color.WHITE); //No grass
		grassMap.mapColor(1, Color.GREEN); //Grass

		Value2DDisplay displayGrass = new Value2DDisplay(space.getGrassGrid(), grassMap);

		Object2DDisplay displayRabbits = new Object2DDisplay(space.getRabbitGrid());
		displayRabbits.setObjectList(rabbitList);

		displaySurface.addDisplayable(displayGrass, "Grass");
		displaySurface.addDisplayableProbeable(displayRabbits, "Rabbits");
		
		class RabbitInSpace implements DataSource, Sequence {
			public Object execute() {
				return new Double(getSValue());
			}

			public double getSValue() {
				return (double)space.getRabbitNb();
			}
		}
		amountOfRabbitAndGrass.addSequence("Rabbit number", new RabbitInSpace(), Color.GRAY);
		
		class GrassInSpace implements DataSource, Sequence {
			public Object execute() {
				return new Double(getSValue());
			}

			public double getSValue() {
				return (double)space.getGrassNb();
			}
		}
		amountOfRabbitAndGrass.addSequence("Grass number", new GrassInSpace(), Color.GREEN);
	}

	
	public Schedule getSchedule() {
		return schedule;
	}

	public String[] getInitParam() {
		// Parameters to be set by users via the Repast UI slider bar
		// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want 
		String[] params = { "GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold", "RabbitInitEnergy", "EatGrassEnergy", "MoveEnergy", "StarvingEnergy"};
		return params;
	}

	public int getGridSize() {
		return gridSize;
	}

	public void setGridSize(int newSize) {
		gridSize = newSize;
	}

	public int getNumInitRabbits() {
		return numInitRabbits;
	}

	public void setNumInitRabbits(int newNum) {
		numInitRabbits = newNum;
	}

	public int getNumInitGrass() {
		return numInitGrass;
	}

	public void setNumInitGrass(int newNum) {
		numInitGrass = newNum;
	}

	public int getGrassGrowthRate() {
		return grassGrowthRate;
	}

	public void setGrassGrowthRate(int newRate) {
		grassGrowthRate = newRate;
	}

	public int getBirthThreshold() {
		return birthThreshold;
	}

	public void setBirthThreshold(int newThreshold) {
		birthThreshold = newThreshold;
	}

	public int getRabbitInitEnergy() {
		return RabbitsGrassSimulationAgent.INIT_ENERGY;
	}
	
	public void setRabbitInitEnergy(int energy) {
		RabbitsGrassSimulationAgent.INIT_ENERGY = energy;
	}

	public int getEatGrassEnergy() {
		return RabbitsGrassSimulationAgent.EAT_ENERGY;
	}
	
	public void setEatGrassEnergy(int energy) {
		RabbitsGrassSimulationAgent.EAT_ENERGY = energy;
	}

	public int getMoveEnergy() {
		return RabbitsGrassSimulationAgent.MOVE_ENERGY;
	}
	
	public void setMoveEnergy(int energy) {
		RabbitsGrassSimulationAgent.MOVE_ENERGY = energy;
	}

	public int getStarvingEnergy() {
		return RabbitsGrassSimulationAgent.STARVING_ENERGY;
	}
	
	public void setStarvingEnergy(int energy) {
		RabbitsGrassSimulationAgent.STARVING_ENERGY = energy;
	}
}
