package minelab.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.Material;

import minelab.utils.Direction;
import minelab.utils.IteratableRectangle;
import minelab.utils.Vector;

public class WidePathDungeon extends BasicDungeon {
	private static double WIND_PERCENT = 0.1;
	private static double EXTRA_CONNECTOR_CHANCE = 0;
	private static int ROOMS_TRIAL_LIMIT = 10;
	private List<Cell> expandableCells = new ArrayList<Cell>();

	public WidePathDungeon(int width, int height) {
		super(width, height);
	}
	
	public Dungeon generate() {
		(new IteratableRectangle(0, 0, this.width, this.height)).getPoints().stream().forEach(
			(point) -> {
				cells[point.x][point.y] = new Cell(point);
			}
		);

		fillRooms(ROOMS_TRIAL_LIMIT);
		fillTunnels();
		connectRegions(EXTRA_CONNECTOR_CHANCE);
		removeDeadEnds();
		expandTunnels();
		
		return (Dungeon) this;
	}

	public Cell[][] getCells() {
		return cells;
	}

	public Cell getCell(int x, int y) {
		return cells[x][y];
	}

	public Cell getCell(Point pos) {
		return cells[pos.x][pos.y];
	}

	protected void fillRooms(int trialLimit) {

		for (int i = 0; i < trialLimit; i++) {
			IteratableRectangle room = randomRectangle(5, 13);
			boolean isOverlap = rooms.stream().anyMatch((r) -> r.intersects(room));
			if (!isOverlap) {
				rooms.add(room);
				markRegion();
				carve(room);
			}
		}

	}

	@Override
	protected void fillTunnels() {
		IteratableRectangle bounds = this.getBounds();
		log.info("Digging tunnels");
		
		for (int y = 3; y < bounds.height - 2; y += 4) {
			for (int x = 3; x < bounds.width - 2; x += 4) {
				Point pos = new Point(x, y);
				Cell cell = getCell(pos);
				pos.translate(1, 0);
				Cell cell2 = getCell(pos);
				pos.translate(-2, 0);
				Cell cell3 = getCell(pos);
				pos.translate(1, 1);
				Cell cell4 = getCell(pos);
				pos.translate(0, -2);
				Cell cell5 = getCell(pos);

				if (cell.getMaterial() != Material.STONE && 
						((cell2.getMaterial() != Material.STONE && cell3.getMaterial() != Material.STONE) || 
							(cell4.getMaterial() != Material.STONE && cell5.getMaterial() != Material.STONE))) {
					continue;
				}

				growMaze(cell);

			}
		}
	}

	protected void growMaze(Cell start) {
		List<Cell> cells = new ArrayList<Cell>();
		Vector lastDir = null;
	
		markRegion();
		carve(start);
		cells.add(start);
	
		while (!cells.isEmpty()) {
			Cell cell = cells.get(cells.size() - 1);
	
			List<Vector> unmadeCells = new ArrayList<Vector>();
			for (Vector dir : Direction.CARDINAL) {
				if (canCarve(cell, dir)) {
					unmadeCells.add(dir);
				}
			}
	
			if (!unmadeCells.isEmpty()) {
				Vector dir;
				if (unmadeCells.contains(lastDir) && random.nextDouble() > WIND_PERCENT) {
					dir = lastDir;
				} else {
					int idx = random.nextInt(unmadeCells.size());
					dir = unmadeCells.get(idx);
				}
	
				Point next = dir.add(cell.getPosition());
				carve(getCell(next));
				expandableCells.add(getCell(next));
	
				Point nextSecond = dir.multiply(2).add(cell.getPosition());
				Cell nextSecondCell = getCell(nextSecond);
				carve(nextSecondCell);
				expandableCells.add(nextSecondCell);
	
				cells.add(nextSecondCell);
				lastDir = dir;
			} else {
				cells.remove(cells.size() - 1);
				lastDir = null;
			}
		}
	}

	protected boolean canCarve(Cell cell, Vector direction) {
		Vector twoSteps = direction.multiply(2);
		Vector threeSteps = direction.multiply(5);
		Point pos = cell.getPosition();
	
		// check if in bounds
		if (!getBounds().contains(threeSteps.add(pos))) {
			return false;
		}
	
		Point nextPos = twoSteps.add(pos);
		Cell nextCell = getCell(nextPos.x, nextPos.y);
		Cell nextCell2;
		Cell nextCell3;
		Cell nextCell4;
		Cell nextCell5;
		
		if (direction.getX() == 0) {
			nextCell2 = getCell(nextPos.x + 1, nextPos.y);
			nextCell3 = getCell(nextPos.x - 1, nextPos.y);
			nextCell4 = getCell(nextPos.x + 2, nextPos.y);
			nextCell5 = getCell(nextPos.x - 2, nextPos.y);
		} else {
			nextCell2 = getCell(nextPos.x, nextPos.y + 1);
			nextCell3 = getCell(nextPos.x, nextPos.y - 1);
			nextCell4 = getCell(nextPos.x, nextPos.y + 2);
			nextCell5 = getCell(nextPos.x, nextPos.y - 2);
		}
		
		
		return nextCell.getMaterial() == Material.STONE && 
				nextCell2.getMaterial() == Material.STONE && 
				nextCell3.getMaterial() == Material.STONE && 
				nextCell4.getMaterial() == Material.STONE && 
				nextCell5.getMaterial() == Material.STONE;
	}

	protected void removeDeadEnds() {
	    boolean done = false;
	
	    log.info("Removing dead ends");
	    while (!done) {
	    	IteratableRectangle bounds = this.getBounds();
	    	done = !bounds.getPoints().stream().anyMatch(
	    		(pos) -> {
	    			Cell currentCell = getCell(pos); 
	    			if (currentCell.getMaterial() == Material.STONE) {
	    				return false;
	    			}
	    			// If it only has one exit, it's a dead end.
	    	        int exits = 0;
	    	        for (Vector dir : Direction.CARDINAL) {
	    	        	Cell suroundCell = getCell(dir.add(pos));
	    	        	if (suroundCell.getMaterial() != Material.STONE) {
	    				  exits++;
	    	        	}
	    	        }

	    	        if (exits != 1) {
	    	        	return false;
	    	        }

	    	        currentCell.setMaterial(Material.STONE);
	    	        expandableCells.remove(currentCell);
	    	        return true; 
	    		} 
	    	);
	    }

	}
	
	protected void expandTunnels() {
		expandableCells.stream().forEach(
			(cell) -> {
				if (cell.getMaterial() != Material.AIR) {
					return;
				}
				List<Cell> candidates = getSurroundingCells(cell).stream().filter((c) -> {
					return c.getMaterial() == Material.STONE;
				}).collect(Collectors.toList());
				
				boolean hasDoor = getSurroundingCells(cell).stream().anyMatch((c) -> {
					boolean isDoor = getSurroundingCells(c).stream().anyMatch(
						(cc) -> cc.getMaterial() == Material.DARK_OAK_DOOR 
					);
					
					return isDoor || c.getMaterial() != Material.STONE && c.getMaterial() != Material.AIR;
				});
				
				if (!hasDoor) {
					candidates.stream().forEach((c) -> {
						c.setMaterial(Material.AIR);
					});
				}
			} 
		);
		
	}

	protected void addJunction(Point pos) {
		double rate = random.nextDouble(); 
		if (rate < 0.95) {
			getCell(pos).setMaterial(Material.AIR);
		} else {
			getCell(pos).setMaterial(Material.DARK_OAK_DOOR);
		}
		
	}
}
