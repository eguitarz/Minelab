package utils;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class IteratableRectangle extends Rectangle {
	private static final long serialVersionUID = -5722998564879273876L;

	public IteratableRectangle(Rectangle rectangle) {
		super(rectangle);
	}
	
	public IteratableRectangle(int x, int y, int width, int height) {
		super(x, y, width, height);
	}
	
	public IteratableRectangle inflate(int magnitude) {
		this.x -= magnitude;
		this.y -= magnitude;
		this.width += magnitude * 2;
		this.height += magnitude * 2;
		
		return this;
	}

	public List<Point> getPoints() {
		List<Point> points = new ArrayList<Point>();
		for (int px=x; px<width+x; px++) {
			for (int py=y; py<height+y; py++) {
				points.add(new Point(px, py));
			}
		}
		
		return points;
	}
	
	public IteratableRectangle intersection(IteratableRectangle ir) {
		return new IteratableRectangle(super.intersection(ir));
	}
}
