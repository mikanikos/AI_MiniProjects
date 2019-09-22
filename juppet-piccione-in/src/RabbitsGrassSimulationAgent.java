import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;

import javax.imageio.ImageIO;

/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	private int x;
	private int y;
	private int energy;
	private static int IDNumber = 0;
	private int ID;
	private int vX;
	private int vY;

	private RabbitsGrassSimulationSpace grassSpace;

	public int getX() { return x; }
	public int getY() { return y; }

	public void setXY(int newX, int newY){
		x = newX;
		y = newY;
	}

	public int getEnergy(){
		return energy;
	}
	public void setEnergy(int energy) { this.energy = energy; }

	public String getID(){
		return "A-" + ID;
	}

	public void setGrassSpace(RabbitsGrassSimulationSpace grassSpace) { this.grassSpace = grassSpace; }

	private void setVxVy() {
		vX = 0;
		vY = 0;
		while (((vX == 0) && (vY == 0)) || ((vX != 0) && (vY != 0))) {
			vX = (int)Math.floor(Math.random() * 3) - 1;
			vY = (int)Math.floor(Math.random() * 3) - 1;
		}
	}

	public RabbitsGrassSimulationAgent(int initEnergy) {
		x = -1;
		y = -1;

		// random energy in the range (5,50)
		if (initEnergy <= 0)
			energy = (int)(Math.random() * ((50 - 5) + 1)) + 5;
		else
			energy = initEnergy;

		setVxVy();
		IDNumber++;
		ID = IDNumber;
	}

	public void report(){
		System.out.println(getID() + " at " + x + ", " + y + " has " + getEnergy() + " energy");
	}

	public void draw(SimGraphics arg0) {
		String imageName = "rabbit-icon.png";
		BufferedImage myPicture = null;
		try {
			myPicture = ImageIO.read(this.getClass().getResource(imageName));
			arg0.drawImageToFit(myPicture);
		} catch (IOException e) {
			arg0.drawFastOval(Color.white);
		}
	}

	public void step() {

		setVxVy();
		int newX = x + vX;
		int newY = y + vY;

		Object2DGrid grid = grassSpace.getCurrentRabbitSpace();
		newX = (newX + grid.getSizeX()) % grid.getSizeX();
		newY = (newY + grid.getSizeY()) % grid.getSizeY();

		if (tryMove(newX, newY)) {
			energy += grassSpace.takeGrassAt(x, y);
		}
		else {
			setVxVy();
		}
		energy--;
	}

	private boolean tryMove(int newX, int newY){
		return grassSpace.moveRabbitAt(x, y, newX, newY);
	}

}
