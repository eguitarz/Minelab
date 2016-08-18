package minelab.utils;

public enum Direction {
	NORTH, EAST, WEST, SOUTH;
	
	public static Vector[] CARDINAL = {getVector(NORTH), getVector(EAST), getVector(SOUTH), getVector(WEST)};
	
	public static Vector getVector(Direction direction) {
		switch (direction) {
			case NORTH: return new Vector(0, 1);
			case EAST: return new Vector(1, 0);
			case WEST: return new Vector(-1, 0);
			case SOUTH: return new Vector(0, -1);
			default: return null;
		}
	}
}
