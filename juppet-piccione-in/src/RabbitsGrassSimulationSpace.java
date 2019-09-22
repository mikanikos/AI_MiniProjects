import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

public class RabbitsGrassSimulationSpace {

    private Object2DGrid grassSpace;
    private Object2DGrid rabbitSpace;

    public Object2DGrid getCurrentGrassSpace() { return grassSpace; }
    public Object2DGrid getCurrentRabbitSpace() { return rabbitSpace; }


    public RabbitsGrassSimulationSpace(int gridSize){
        grassSpace = new Object2DGrid(gridSize, gridSize);
        rabbitSpace = new Object2DGrid(gridSize, gridSize);

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                grassSpace.putObjectAt(i,j,new Integer(0));
            }
        }
    }

    public void spreadGrass(int numGrass) {
        for(int i = 0; i < numGrass; i++){
            int x;
            int y;

            // Choose coordinates
            do {
                x = (int) (Math.random() * (grassSpace.getSizeX()));
                y = (int) (Math.random() * (grassSpace.getSizeY()));
            }
            while (isCellTaken(x,y));

            // Get the value of the object at those coordinates
            int value = getGrassAt(x,y);

            // Replace the Integer object with another one with the new value
            grassSpace.putObjectAt(x,y,new Integer(value + 1));
        }
    }

    public int getGrassAt (int x, int y) {
        if (grassSpace.getObjectAt(x,y) != null)
            return ((Integer) grassSpace.getObjectAt(x,y)).intValue();
        else
            return 0;
    }

    public boolean isCellTaken(int x, int y) {
        if (rabbitSpace.getObjectAt(x, y) != null)
            return true;
        else
            return false;
    }

    public boolean addRabbit(RabbitsGrassSimulationAgent rabbit) {
        boolean retVal = false;
        int count = 0;
        int countLimit = 10 * rabbitSpace.getSizeX() * rabbitSpace.getSizeY();

        while ((retVal==false) && (count < countLimit)) {
            int x = (int)(Math.random()*(rabbitSpace.getSizeX()));
            int y = (int)(Math.random()*(rabbitSpace.getSizeY()));
            if (isCellTaken(x,y) == false) {
                rabbitSpace.putObjectAt(x,y,rabbit);
                rabbit.setXY(x,y);
                rabbit.setGrassSpace(this);
                retVal = true;
            }
            count++;
        }
        return retVal;
    }

    public void removeRabbitAt(int x, int y) {
        rabbitSpace.putObjectAt(x, y, null);
    }

    public int takeGrassAt(int x, int y) {
        int grass = getGrassAt(x, y);
        grassSpace.putObjectAt(x, y, new Integer(0));
        return grass;
    }

    public boolean moveRabbitAt(int x, int y, int newX, int newY){
        boolean retVal = false;
        if (!isCellTaken(newX, newY)) {
            RabbitsGrassSimulationAgent cda = (RabbitsGrassSimulationAgent) rabbitSpace.getObjectAt(x, y);
            removeRabbitAt(x,y);
            cda.setXY(newX, newY);
            rabbitSpace.putObjectAt(newX, newY, cda);
            retVal = true;
        }
        return retVal;
    }

}
