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

public class BasicDungeon implements Dungeon {
	private static double WIND_PERCENT = 0.25;
	private static double EXTRA_CONNECTOR_CHANCE = 0.04;
	private static int ROOMS_TRIAL_LIMIT = 300;
	
	protected ArrayList<IteratableRectangle> rooms = new ArrayList<IteratableRectangle>(100);
	protected Random random = new Random();
	protected int currentRegion = -1;
	protected Cell[][] cells;
	//protected Map<Integer, Cell[][]> layers;
	protected Integer[][] regionsTable;
	protected Logger log = Logger.getLogger("Minelab");
	protected int width;
	protected int height;
	
	public BasicDungeon(int width, int height) {
		if (width % 2 == 0) {
			width += 1;
			log.warning("Padding width to odd: " + width);
		}
		
		if (height % 2 == 0) {
			height += 1;
			log.warning("Padding width to odd: " + height);
		}
		
		this.width = width;
		this.height = height;
		cells = new Cell[width][height];
		regionsTable = new Integer[width][height];
		//layers = new HashMap<Integer, Cell[][]>();
	}
	
	public Dungeon generate() {
		(new IteratableRectangle(0, 0, width, height)).getPoints().stream().forEach(
			(point) -> {
				cells[point.x][point.y] = new Cell(point);
			}
		);

		fillRooms(ROOMS_TRIAL_LIMIT);
		fillTunnels();
		connectRegions(EXTRA_CONNECTOR_CHANCE);
		removeDeadEnds();
		
		return this;
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

	protected void fillTunnels() {
		IteratableRectangle bounds = this.getBounds();
		log.info("Digging tunnels");
		
		for (int y = 1; y < bounds.height; y += 2) {
			for (int x = 1; x < bounds.width; x += 2) {
				Point pos = new Point(x, y);
				Cell cell = getCell(pos);

				if (cell.getMaterial() != Material.STONE) {
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

				Point nextSecond = dir.multiply(2).add(cell.getPosition());
				Cell nextSecondCell = getCell(nextSecond);
				carve(nextSecondCell);

				cells.add(nextSecondCell);
				lastDir = dir;
			} else {
				cells.remove(cells.size() - 1);
				lastDir = null;
			}
		}
	}

	protected void connectRegions(double extraConnectorChance) {
		Map<Point, Set<Integer>> connectorRegions = new HashMap<Point, Set<Integer>>();

		IteratableRectangle bound = this.getBounds();
		bound.inflate(-1).getPoints().stream().forEach((pos) -> {
			if (getCell(pos).getMaterial() != Material.STONE) {
				return;
			}

			Set<Integer> regions = new HashSet<Integer>();
			for (Vector dir : Direction.CARDINAL) {
				Point regionPos = dir.add(pos);
				Integer region = regionsTable[regionPos.x][regionPos.y];
				if (region != null) {
					regions.add(region);
				}
			}

			if (regions.size() < 2) {
				return;
			}

			connectorRegions.put(pos, regions);
		});
		
		List<Point> connectors = new ArrayList<Point>();
		Iterator<Point> connectorPointIt = connectorRegions.keySet().iterator();
		while (connectorPointIt.hasNext()) {
			connectors.add(connectorPointIt.next());
		}

		Map<Integer, Integer> mergedRegions = new HashMap<Integer, Integer>();
		Set<Integer> openRegions = new HashSet<Integer>();
		for (int i = 0; i <= currentRegion; i++) {
			mergedRegions.put(i, i);
			openRegions.add(i);
		}

		while (openRegions.size() > 1) {
			int randBound = connectors.size();
			if (randBound == 0) {
				break;
			}
			Point connector = connectors.get(random.nextInt(randBound));

			addJunction(connector);

			// usually 2 regions
			List<Integer> merged = connectorRegions.get(connector).stream().map(
				region -> mergedRegions.get(region)
			).collect(Collectors.toList());

			Integer dest = merged.remove(0);

			for (int i = 0; i <= currentRegion; i++) {
				if (merged.contains(mergedRegions.get(i))) {
					mergedRegions.put(i, dest);
				}
			}

			openRegions.removeAll(merged);
			
			for (Iterator<Point> connectorIt = connectors.iterator(); connectorIt.hasNext();) {
				Point pos = connectorIt.next();
				if (connector.distance(pos) < 3) {
					connectorIt.remove();
					continue;
				}

				// If the connector no long spans different regions, we don't
				// need it.
				Iterator<Integer> connectorRegionIt = connectorRegions.get(pos).iterator();
				Set<Integer> toBeRemovedRegions = new HashSet<Integer>();
				while (connectorRegionIt.hasNext()) {
					Integer connectorRegion = connectorRegionIt.next();
					toBeRemovedRegions.add(mergedRegions.get(connectorRegion));
				}

				if (toBeRemovedRegions.size() <= 1) {
					// This connecter isn't needed, but connect it occasionally
					// so that the
					// dungeon isn't singly-connected.
					if (random.nextDouble() < extraConnectorChance) {
						addJunction(pos);
					}
					
					connectorIt.remove();
				}
			}

		}
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
	    	        return true; 
	    		} 
	    	);
	    }

	}

	protected void addJunction(Point pos) {
		double rate = random.nextDouble(); 
		if (rate < 0.95) {
			getCell(pos).setMaterial(Material.AIR);
		} else {
			getCell(pos).setMaterial(Material.DARK_OAK_DOOR);
		}
		
	}

	protected void carve(IteratableRectangle rect) {
		rect.getPoints().stream().forEach((pos) -> {
			carve(getCell(pos));
		});
	}

	protected void carve(Cell cell) {
		cell.setMaterial(Material.AIR);
		Point pos = cell.getPosition();
		regionsTable[pos.x][pos.y] = currentRegion;
	}

	protected boolean canCarve(Cell cell, Vector direction) {
		Vector twoSteps = direction.multiply(2);
		Vector threeSteps = direction.multiply(3);
		Point pos = cell.getPosition();

		// check if in bounds
		if (!getBounds().contains(threeSteps.add(pos))) {
			return false;
		}

		Point nextPos = twoSteps.add(pos);
		Cell nextCell = getCell(nextPos.x, nextPos.y);

		return nextCell.getMaterial() == Material.STONE;
	}

	protected IteratableRectangle randomRectangle(int min, int max) {

		int width = random.nextInt(max - min) + min;
		int height = random.nextInt(max - min) + min;

		width = width % 2 == 0 ? width + 1 : width;
		height = height % 2 == 0 ? height + 1 : height;
		int x = (random.nextInt(this.getWidth() - width) / 2) * 2 + 1;
		int y = (random.nextInt(this.getHeight() - height) / 2) * 2 + 1;
		return new IteratableRectangle(x, y, width, height);
	}

	protected void markRegion() {
		currentRegion++;
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

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public IteratableRectangle getBounds() {
		return new IteratableRectangle(0, 0, width, height);
	}
	
	protected List<Cell> getSurroundingCells(Cell cell) {
		List<Cell> cells = new ArrayList<Cell>();
		Point pos = cell.getPosition();
		pos.translate(1, 0); // east
		cells.add(getCell(pos));
		pos.translate(0, -1); // south east
		cells.add(getCell(pos));
		pos.translate(-1, 0); // south
		cells.add(getCell(pos));
		pos.translate(-1, 0); // south west
		cells.add(getCell(pos));
		pos.translate(0, 1); // west
		cells.add(getCell(pos));
		pos.translate(0, 1); // north west
		cells.add(getCell(pos));
		pos.translate(1, 0); // north
		cells.add(getCell(pos));
		pos.translate(1, 0); // north east
		cells.add(getCell(pos));
		
		return cells;
	}
	
	protected List<Cell> getCardinalCells(Cell cell) {
		List<Cell> cells = new ArrayList<Cell>();
		Point pos = cell.getPosition();
		pos.translate(1, 0); // east
		cells.add(getCell(pos));
		pos.translate(-1, -1); // south
		cells.add(getCell(pos));
		pos.translate(-1, 1); // west
		cells.add(getCell(pos));
		pos.translate(1, 1); // north
		cells.add(getCell(pos));
		
		return cells;
	}
}
