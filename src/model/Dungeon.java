package model;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Material;

import utils.Direction;
import utils.Vector;

public class Dungeon {
	private ArrayList<Rectangle> rooms = new ArrayList<Rectangle>(100);
	private Random random = new Random();
	private int width;
	private int height;
	private int currentRegion = -1;
	private final double WIND_PERCENT = 0.25;
	private static final double EXTRA_CONNECTOR_CHANCE = 0.08;
	private Cell[][] cells;
	private Integer[][] regionsTable;
	private Logger log = Logger.getLogger("Minelab");

	public static int roomsTrialLimit = 300;

	public Dungeon(int width, int height) {
		if (width % 2 == 0 || height % 2 == 0) {
			log.warning("Dungeon size should be odd, this dungeon will have some flaws");
		}
		
		this.width = width;
		this.height = height;
		cells = new Cell[width][height];
		regionsTable = new Integer[width][height];
	}
	
	public Dungeon generate() {
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				Cell cell = new Cell(x, y);
				cells[x][y] = cell;
			}
		}

		fillRooms(roomsTrialLimit);
		fillTunnels();
		connectRegions();
		removeDeadEnds();
		
		return this;
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

	private void fillRooms(int trialLimit) {

		for (int i = 0; i < trialLimit; i++) {
			Rectangle room = createRectangle();
			boolean isOverlap = false;

			for (Iterator<Rectangle> r = rooms.iterator(); r.hasNext();) {
				if (r.next().intersects(room)) {
					isOverlap = true;
					break;
				}
			}

			if (!isOverlap) {
				rooms.add(room);
				markRegion();
				carve(room);
			}
		}

	}

	private void fillTunnels() {
		Rectangle bounds = this.getRectangle();

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

	private void growMaze(Cell start) {
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

	private void connectRegions() {
		Map<Point, Set<Integer>> connectorRegions = new HashMap<Point, Set<Integer>>();

		Rectangle bound = this.getRectangle();
		int x1 = (int) (bound.getMinX() + 1);
		int y1 = (int) (bound.getMinY() + 1);
		int x2 = (int) (bound.getMaxX() - 1);
		int y2 = (int) (bound.getMaxX() - 1);

		for (int x = x1; x < x2; x++) {
			for (int y = y1; y < y2; y++) {
				if (getCell(x, y).getMaterial() != Material.STONE) {
					continue;
				}

				Point pos = new Point(x, y);
				Set<Integer> regions = new HashSet<Integer>();
				for (Vector dir : Direction.CARDINAL) {
					Point regionPos = dir.add(pos);
					Integer region = regionsTable[regionPos.x][regionPos.y];
					if (region != null) {
						regions.add(region);
					}
				}

				if (regions.size() < 2) {
					continue;
				}

				connectorRegions.put(pos, regions);
			}
		}

		List<Point> connectors = new ArrayList<Point>();
		Iterator<Point> connectorPointIt = connectorRegions.keySet().iterator();
		while (connectorPointIt.hasNext()) {
			connectors.add(connectorPointIt.next());
		}
		log.info("connector points:" + connectors.size());

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
			Iterator<Integer> it = connectorRegions.get(connector).iterator();
			List<Integer> merged = new ArrayList<Integer>();
			while (it.hasNext()) {
				Integer region = it.next();
				merged.add(mergedRegions.get(region));
			}

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
					if (random.nextDouble() < EXTRA_CONNECTOR_CHANCE) {
						addJunction(pos);
					}
					
					connectorIt.remove();
				}
			}

		}

	}
	
	public void removeDeadEnds() {
	    boolean done = false;

	    log.info("Removing dead ends");
	    while (!done) {
	    	done = true;

	      	Rectangle bounds = this.getRectangle();

			for (int y = 1; y < bounds.height - 1; y++) {
				for (int x = 1; x < bounds.width - 1; x++) {
					Point pos = new Point(x, y);
					Cell currentCell = getCell(pos); 
					if (currentCell.getMaterial() == Material.STONE) {
						continue;
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
			        	continue;
			        }

			        done = false;       
			        currentCell.setMaterial(Material.STONE);
				}
			}
	    }
	}


	private void addJunction(Point pos) {
		double rate = random.nextDouble(); 
		if (rate < 0.95) {
			getCell(pos).setMaterial(Material.AIR);
		} else {
			getCell(pos).setMaterial(Material.DARK_OAK_DOOR);
		}
		
	}

	private void carve(Rectangle rect) {
		int x1 = (int) rect.getMinX();
		int y1 = (int) rect.getMinY();
		int x2 = (int) rect.getMaxX();
		int y2 = (int) rect.getMaxY();
		for (int x = x1; x < x2; x++) {
			for (int y = y1; y < y2; y++) {
				carve(getCell(x,y));
			}
		}
	}

	private void carve(Cell cell) {
		cell.setMaterial(Material.AIR);
		Point pos = cell.getPosition();
		regionsTable[pos.x][pos.y] = currentRegion;
	}

	private boolean canCarve(Cell cell, Vector direction) {
		Vector twoSteps = direction.multiply(2);
		Vector threeSteps = direction.multiply(3);
		Point pos = cell.getPosition();

		// check if in bounds
		if (!getRectangle().contains(threeSteps.add(pos))) {
			return false;
		}

		Point nextPos = twoSteps.add(pos);
		Cell nextCell = getCell(nextPos.x, nextPos.y);

		return nextCell.getMaterial() == Material.STONE;
	}

	private Rectangle createRectangle() {
		final int MIN_LENGTH = 5;
		final int MAX_LENGTH = 13;

		int width = random.nextInt(MAX_LENGTH - MIN_LENGTH) + MIN_LENGTH;
		int height = random.nextInt(MAX_LENGTH - MIN_LENGTH) + MIN_LENGTH;

		width = width % 2 == 0 ? width + 1 : width;
		height = height % 2 == 0 ? height + 1 : height;
		int x = (random.nextInt(this.getWidth() - width) / 2) * 2 + 1;
		int y = (random.nextInt(this.getHeight() - height) / 2) * 2 + 1;
		return new Rectangle(x, y, width, height);
	}

	public void markRegion() {
		currentRegion++;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public Rectangle getRectangle() {
		return new Rectangle(0, 0, this.width, this.height);
	}
}
