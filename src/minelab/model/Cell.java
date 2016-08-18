package minelab.model;

import java.awt.Point;

import org.bukkit.Material;

public class Cell {
	private Material material = Material.STONE;
	private Point position;
	
	public Cell(int x, int y) {
		this(new Point(x, y));
	}
	
	public Cell(Point pos) {
		this.position = pos;
	}
	
	public Material getMaterial() {
		return material;
	}
	
	public void setMaterial(Material material) {
		this.material = material;
	}
	
	public Point getPosition() {
		return (Point) position.clone();
	}
	
	public void setLocation(Point location) {
		this.position = location;
	}
}
