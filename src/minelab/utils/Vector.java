package minelab.utils;

import java.awt.Point;

public class Vector {
	
	private double x;
    private double y;

	public Vector(int x, int y) {
        this.x = x;
        this.y = y;
    }
	
	public Vector(double x, double y) {
        this.x = x;
        this.y = y;
    }
	
	public Point add(Point point) {
		return new Point(point.x + (int)x, point.y + (int)y);
	}
	
	public Vector add(Vector vec) {
        return new Vector(x + vec.x, y + vec.y);
    }
	
	public Vector subtract(Vector vec) {
        return new Vector(x - vec.x, y - vec.y);
    }
	
	public Vector multiply(Vector vec) {
        return new Vector(x * vec.x, y * vec.y);
    }
	
	public Vector divide(Vector vec) {
        return new Vector(x / vec.x, y / vec.y);
    }
	
	public Vector multiply(int m) {
        return new Vector(x * m, y * m);
    }
	
	public Vector divide(int m) {
        return new Vector(x / m, y / m);
    }
	
	public double length() {
        return Math.sqrt(square(x) + square(y));
    }
	
	public double square(double n) {
		return Math.pow(n, 2);
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}
}
