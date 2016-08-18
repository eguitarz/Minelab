package minelab.model;

import minelab.utils.IteratableRectangle;

public interface Dungeon {
	public Dungeon generate();
	public Cell[][] getCells();
	public IteratableRectangle getBounds();
}
