import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author Piccione Andrea, Juppet Quentin
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	public static int INIT_ENERGY = 50;
	public static int EAT_ENERGY = 20;
	public static int MOVE_ENERGY = 1;
	public static int STARVING_ENERGY = 1;

	private static BufferedImage rabbitPicture = null;
	
	private int x;
	private int y;
	private int energy = INIT_ENERGY;
	
	private RabbitsGrassSimulationSpace space;
	
	public RabbitsGrassSimulationAgent(RabbitsGrassSimulationSpace space, int x, int y) {
		this.space = space;
		setXY(x, y);
	}

	public void draw(SimGraphics graphics) {
		String imageName = "rabbit-icon.png";
		try {
			if(rabbitPicture == null)
				rabbitPicture = ImageIO.read(this.getClass().getResource(imageName));
			graphics.drawImageToFit(rabbitPicture);
		} catch (Exception e) {
			graphics.drawFastOval(Color.GRAY);
		}
	}

	public void setXY(int newX, int newY) {
		x = newX;
		y = newY;
	}
	
	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}
	
	public int getEnergy() {
		return energy;
	}
	
	public void addToEnergy(int toAdd) {
		energy += toAdd;
	}

	public void step() {
		move();
		eatGrass();
	}
	
	private void move() {
		int dx = 0;
		int dy = 0;

		int direction = (int)(Math.random() * 4);

		switch(direction) {
		case 0: //Top
			dy = -1;
			break;
		case 1: //Right
			dx = 1;
			break;
		case 2: //Bottom
			dy = 1;
			break;
		case 3: //Left
			dx = -1;
			break;
		}

		int newX = x + dx;
		int newY = y + dy;

		if(space.moveRabbit(x, y, newX, newY)) {
			addToEnergy(-MOVE_ENERGY);
		}
	}

	private void eatGrass() {
		if(space.eatGrassAt(x, y)) {
			addToEnergy(EAT_ENERGY);
		}else {
			addToEnergy(-STARVING_ENERGY);
		}
	}
}
