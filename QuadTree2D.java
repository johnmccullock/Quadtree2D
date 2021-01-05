package main.game.gui.core.util;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Vector;

/**
 * A generic quadtree data structure for holding objects extending Point2D.
 * 
 * Version 2.2 replaces Rect2D with the simpler Point2D.
 * Version 2.1.1 replaces floats with doubles.  Also, deletion methods were removed.
 * Version 2.1 includes a size field, and query function based on a circle.
 * 
 * @author John McCullock
 * @version 2.2 2018-11-08
 * @param <T> any class that extends Point2D
 */
public class QuadTree2D<T extends Point2D>
{
	private Node<T> mRoot = null;
	private int mCapacity = 1; // bucket capacity.
	private int mSize = 0;
	
	public QuadTree2D(int x, int y, int width, int height, int capacity)
	{
		this.mCapacity = capacity;
		this.mRoot = new Node<T>(x, y, width, height);
		return;
	}
	
	public boolean insert(T item)
	{
		return this.mRoot.insert(item);
	}
	
	public int getSize()
	{
		this.mSize = 0;
		this.mRoot.getSize();
		return this.mSize;
	}
	
	public Vector<T> query(int x, int y, int width, int height)
	{
		return this.mRoot.query(x, y, width, height);
	}
	
	/**
	 * Inverts the search results to include everything not in the specified parameters.
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @return
	 */
	public Vector<T> inverseQuery(int x, int y, int width, int height)
	{
		return this.mRoot.inverseQuery(x, y, width, height);
	}
	
	/**
	 * Similar to the normal quadtree query, but uses a circle instead of a rectangular range to search for items.
	 * @param centerX int x-coordinate at the center of the query range.
	 * @param centerY int y-coordinate at the center of the query range.
	 * @param radius int radius value ranging out from the center coordinates.
	 * @return Vector{@literal <}T{@literal >} containing item found within the circle.
	 */
	public Vector<T> query(int centerX, int centerY, int radius)
	{
		return this.mRoot.query(centerX - radius, centerY - radius, radius * 2, radius * 2, centerX, centerY, radius);
	}
	
	public void clear()
	{
		this.mRoot.clear();
		return;
	}
	
	public Rectangle getBounds()
	{
		return new Rectangle(this.mRoot.x, this.mRoot.y, this.mRoot.width, this.mRoot.height);
	}
	
	/**
	 * Creates rectangles illustrating the size and location of this node and its child nodes.
	 * Useful mostly for debugging.
	 * @return Vector{@literal <}Rectangle{@literal >}
	 */
	public Vector<Rectangle> getCellBounds()
	{
		return this.mRoot.getCellBounds();
	}
	
	/**
	 * Finds the distance between two points using Pythagorean Theorem.
	 * @param x1 double x value for first point.
	 * @param y1 double y value for first point.
	 * @param x2 double x value for second point.
	 * @param y2 double y value for second point.
	 * @return double distance between first and second point.
	 */
	private static double distance(final double x1, final double y1, final double x2, final double y2)
 	{
		return Math.sqrt(((x2 - x1) * (x2 - x1)) + ((y2 - y1) * (y2 - y1)));
 	}
	
	@SuppressWarnings("hiding")
	private class Node<T extends Point2D>
	{
		public int x = 0;
		public int y = 0;
		public int width = 0;
		public int height = 0;
		public Node<T> northWest = null;
		public Node<T> northEast = null;
		public Node<T> southWest = null;
		public Node<T> southEast = null;
		public Vector<T> bucket = null;
		
		public Node(int x, int y, int width, int height)
		{
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.bucket = new Vector<T>();
			return;
		}
		
		private boolean contains(double x, double y)
		{
			if((x >= this.x) && (x <= this.x + this.width) && (y >= this.y) && (y <= this.y + this.height)){
				return true;
			}else{
				return false;
			}
		}
		
		/**
		 * Determines if a rectangle intersects with this node's bounds.
		 * @param x double
		 * @param y double
		 * @param width double
		 * @param height double
		 * @return boolean true if intersection occurs, false otherwise.
		 */
		private boolean intersects(double x, double y, double width, double height)
		{
			/*
			 * This bit of code used for determining intersection might seem a little odd at first, 
			 * so try to avoid modifying it to look like normal intersection code.
			 * 
			 * Normally, both vertical and horizontal are compared at the same time to determine intersection.
			 * But because of the nature of quadtree traversal, vertical and horizontal need to be considered
			 * individually.
			 */
			if(x >= this.x && x <= this.x + this.width){
				return true;
			}
			if(this.x >= x && this.x <= x + width){
				return true;
			}
			if(x + width >= this.x && x + width <= this.x + this.width){
				return true;
			}
			if(this.x + this.width >= x && this.x + this.width <= x + width){
				return true;
			}
			if(y >= this.y && y <= this.y + this.height){
				return true;
			}
			if(this.y >= y && this.y <= y + height){
				return true;
			}
			if(y + height >= this.y && y + height <= this.y + this.height){
				return true;
			}
			if(this.y + this.height >= y && this.y + this.height <= y + height){
				return true;
			}
			return false;
		}
		
		public boolean insert(T item)
		{
			if(!this.contains(item.getX(), item.getY())){
				return false;
			}
			
			// If there's room for an item, insert it and return true.
			if(this.bucket.size() < mCapacity){
				this.bucket.add(item);
				return true;
			}
			
			// If there isn't enough room, subdivide.
			if(this.northWest == null){
				this.subdivide();
			}
			
			if(this.northWest.insert(item)) { return true; }
			if(this.northEast.insert(item)) { return true; }
			if(this.southWest.insert(item)) { return true; }
			if(this.southEast.insert(item)) { return true; }
			
			return false;
		}
		
		private void subdivide()
		{
			/*
			 *  Use Math.floor() instead of Math.round().  Rounding errors could cause boundary overlap.
			 */
			int halfWidth = (int)Math.floor(this.width / 2.0);
			int halfHeight = (int)Math.floor(this.height / 2.0);
			this.northWest = new Node<T>(this.x, this.y, halfWidth, halfHeight);
			this.northEast = new Node<T>(this.x + halfWidth, this.y, this.width - halfWidth, halfHeight);
			this.southWest = new Node<T>(this.x, this.y + halfHeight, halfWidth, this.height - halfHeight);
			this.southEast = new Node<T>(this.x + halfWidth, this.y + halfHeight, this.width - halfWidth, this.height - halfHeight);
			return;
		}
		
		public void getSize()
		{
			mSize += this.bucket.size();
			
			if(this.northEast != null){
				this.northEast.getSize();
				this.northWest.getSize();
				this.southEast.getSize();
				this.southWest.getSize();
			}
			return;
		}
		
		public Vector<T> query(int x, int y, int width, int height)
		{
			Vector<T> results = new Vector<T>();
			
			if(!this.intersects(x, y, width, height)){
				return results;
			}
			
			for(int i = 0; i < this.bucket.size(); i++)
			{
				if(this.bucket.get(i).getX() > x && this.bucket.get(i).getX() < x + width && this.bucket.get(i).getY() > y && this.bucket.get(i).getY() < y + height){
					results.add(this.bucket.get(i));
				}
			}
			
			if(this.northWest == null){
				return results;
			}
			
			results.addAll(this.northWest.query(x, y, width, height));
			results.addAll(this.northEast.query(x, y, width, height));
			results.addAll(this.southWest.query(x, y, width, height));
			results.addAll(this.southEast.query(x, y, width, height));
			
			return results;
		}
		
		public Vector<T> inverseQuery(int x, int y, int width, int height)
		{
			Vector<T> results = new Vector<T>();
			
			//if(!this.intersects(x, y, width, height)){
			//	return results;
			//}
			
			for(int i = 0; i < this.bucket.size(); i++)
			{
				if(this.bucket.get(i).getX() < x || this.bucket.get(i).getX() > x + width || this.bucket.get(i).getY() < y || this.bucket.get(i).getY() > y + height){
					results.add(this.bucket.get(i));
				}
			}
			
			if(this.northWest == null){
				return results;
			}
			
			results.addAll(this.northWest.query(x, y, width, height));
			results.addAll(this.northEast.query(x, y, width, height));
			results.addAll(this.southWest.query(x, y, width, height));
			results.addAll(this.southEast.query(x, y, width, height));
			
			return results;
		}
		
		public Vector<T> query(int x, int y, int width, int height, int centerX, int centerY, double radius)
		{
			Vector<T> results = new Vector<T>();
			
			if(!this.intersects(x, y, width, height)){
				return results;
			}
			
			for(int i = 0; i < this.bucket.size(); i++)
			{
				if(this.bucket.get(i).getX() < x || this.bucket.get(i).getX() > x + width && this.bucket.get(i).getY() < y || this.bucket.get(i).getY() > y + height){
					continue;
				}
				if(distance(this.bucket.get(i).getX(), this.bucket.get(i).getY(), centerX, centerY) <= radius){
					results.add(this.bucket.get(i));
				}
			}
			
			if(this.northWest == null){
				return results;
			}
			
			results.addAll(this.northWest.query(x, y, width, height, centerX, centerY, radius));
			results.addAll(this.northEast.query(x, y, width, height, centerX, centerY, radius));
			results.addAll(this.southWest.query(x, y, width, height, centerX, centerY, radius));
			results.addAll(this.southEast.query(x, y, width, height, centerX, centerY, radius));
			
			return results;
		}
		
		public void clear()
		{
			this.bucket.clear();
			this.northWest = null;
			this.northEast = null;
			this.southWest = null;
			this.southEast = null;
			return;
		}
		
		/**
		 * Creates rectangles illustrating the size and location of this node and its child nodes.
		 * Useful mostly for debugging.
		 * @return Vector{@literal <}Rectangle{@literal >}
		 */
		public Vector<Rectangle> getCellBounds()
		{
			Vector<Rectangle> results = new Vector<Rectangle>();
			results.add(new Rectangle(this.x, this.y, this.width, this.height));
			
			if(this.northWest == null){
				return results;
			}
			
			results.addAll(this.northWest.getCellBounds());
			results.addAll(this.northEast.getCellBounds());
			results.addAll(this.southWest.getCellBounds());
			results.addAll(this.southEast.getCellBounds());
			return results;
		}
		
		
	}
}
