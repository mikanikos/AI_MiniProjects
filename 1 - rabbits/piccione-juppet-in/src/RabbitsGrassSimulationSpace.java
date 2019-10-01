import java.util.ArrayList;

import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author Piccione Andrea, Juppet Quentin
 */

public class RabbitsGrassSimulationSpace {

	private Object2DGrid grassGrid;
	private Object2DGrid rabbitGrid;
	
	private int rabbitNb = 0;
	private int grassNb = 0;

	public RabbitsGrassSimulationSpace(int size) {
		grassGrid = new Object2DGrid(size, size);
		rabbitGrid = new Object2DGrid(size, size);

		for(int i = 0; i < size; i++){
			for(int j = 0; j < size; j++){
				grassGrid.putObjectAt(i,j, new Integer(0));
			}
		}
	}

	public void spreadGrass(int numGrass) {
		int maxGrassNb = grassGrid.getSizeX() * grassGrid.getSizeY();
		for(int i = 0; i < numGrass; ++i) {
			if(grassNb < maxGrassNb) {
				int x, y;
				do {
					x = (int)(Math.random()*(grassGrid.getSizeX()));
					y = (int)(Math.random()*(grassGrid.getSizeY()));
				}while(isGrassAt(x, y));

				grassGrid.putObjectAt(x, y, new Integer(1));
				++grassNb;
			}
		}
	}

	public ArrayList<RabbitsGrassSimulationAgent> spreadRabbits(int numRabbits) {
		int maxRabbitNb = rabbitGrid.getSizeX() * rabbitGrid.getSizeY();
		ArrayList<RabbitsGrassSimulationAgent> rabbitList = new ArrayList<>();
		for(int i = 0; i < numRabbits; ++i) {
			if(rabbitNb < maxRabbitNb) {
				int x, y;
				do {
					x = (int)(Math.random()*(rabbitGrid.getSizeX()));
					y = (int)(Math.random()*(rabbitGrid.getSizeY()));
				}while(getRabbitAt(x, y) != null);
				
				RabbitsGrassSimulationAgent rabbit = new RabbitsGrassSimulationAgent(this, x, y);
				rabbitList.add(rabbit);
				rabbitGrid.putObjectAt(x, y, rabbit);
				++rabbitNb;
			}
		}
		return rabbitList;
	}

	public Object2DGrid getGrassGrid() {
		return grassGrid;
	}

	public Object2DGrid getRabbitGrid() {
		return rabbitGrid;
	}
	
	public int getGrassNb() {
		return grassNb;
	}
	
	public int getRabbitNb() {
		return rabbitNb;
	}
	
	public boolean eatGrassAt(int x, int y) {
		if(isGrassAt(x, y)) {
			grassGrid.putObjectAt(x, y, new Integer(0));
			--grassNb;
			return true;
		}
		return false;
	}

	public boolean isGrassAt(int x, int y) {
		Integer isGrass = (Integer)grassGrid.getObjectAt(x, y);
		return isGrass.intValue() == 1;
	}
	
	public RabbitsGrassSimulationAgent getRabbitAt(int x, int y) {
		Object rabbit = rabbitGrid.getObjectAt(x, y);
		if(rabbit == null) return null;
		return (RabbitsGrassSimulationAgent)rabbit;
	}

	public void removeRabbitAt(int x, int y) {
		rabbitGrid.putObjectAt(x, y, null);
		--rabbitNb;
	}
	
	public boolean moveRabbit(int x, int y, int newX, int newY) {
		newX = (newX + rabbitGrid.getSizeX()) % rabbitGrid.getSizeX();
		newY = (newY + rabbitGrid.getSizeY()) % rabbitGrid.getSizeY();
		
		if(getRabbitAt(newX, newY) == null) {
			RabbitsGrassSimulationAgent rabbit = getRabbitAt(x, y);
			rabbitGrid.putObjectAt(x, y, null);
			rabbitGrid.putObjectAt(newX, newY, rabbit);
			rabbit.setXY(newX, newY);
			
			return true;
		}
		return false;
	}
}
