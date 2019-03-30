package main;

// Author: Kevin Wang

public class ByteMap {
	
	// Variables determine map resolution.
	protected int xPoints;
	protected int yPoints;
	
	// Final variables allow conversion of map point into degree position.
	protected double initLongitude;
	protected double initLatitude;
	protected double longitude;
	protected double latitude;
	
	protected byte[][] byteMap;
	
	public ByteMap(double initLongitude, double initLatitude, double longitude, double latitude, int xPoints, int yPoints) {
		this.initLongitude = initLongitude;
		this.initLatitude = initLatitude;
		this.longitude = longitude;
		this.latitude = latitude;
		this.xPoints = xPoints;
		this.yPoints = yPoints;
		byteMap = new byte[xPoints][yPoints];
	}
	
	// Primary Interface Methods
	
	public void setPointWithResolution(double longitude, double latitude, byte value, int resolution) {
		// remove remainder to put into grid.
		int xInit = (assureRangeX(convertLong(longitude)) / resolution) * resolution;
		int yInit = (assureRangeY(convertLat(latitude)) / resolution) * resolution;
		for (int x = xInit; x < xInit + resolution; x++) {
			for (int y = yInit; y < yInit + resolution; y++) {
				byteMap[x][y] = value;
			}
		}
	}
	
	public int getTotalInArea(double startLong, double startLat, double longitude, double latitude) {
		int total = 0;
		int xStart = assureRangeX(convertLong(startLong));
		int yStart = assureRangeY(convertLat(startLat));
		int xEnd = assureRangeX(convertLong(startLong + longitude));
		int yEnd = assureRangeY(convertLat(startLat + latitude));
		for (int x = xStart; x < xEnd; x++) {
			for (int y = yStart; y < yEnd; y++) {
				total += byteMap[x][y];
			}
		}
		return total;
	}
	
	public int getTotalInArea(int xStart, int yStart, int xLength, int yLength) {
		int total = 0;
		for (int x = assureRangeX(xStart); x < assureRangeX(xStart + xLength); x++) {
			for (int y = assureRangeY(yStart); y < assureRangeY(yStart + yLength); y++) {
				total += byteMap[x][y];
			}
		}
		return total;
	}
	
	public void setPointIncrementAdjacentThreshold(double longitude, double latitude, byte value, byte valueAdjacent, byte valueThreshold) {
		int xPt = assureRangeX(convertLong(longitude));
		int yPt = assureRangeY(convertLat(latitude));
		for (int x = xPt - 1; x < xPt + 2; x++) {
			for (int y = yPt - 1; y < yPt + 2; y++) {
				if (byteMap[x][y] < valueThreshold) byteMap[x][y] += valueAdjacent;
			}
		}
		byteMap[xPt][yPt] = value;
	}
	
	public void setPoint(double longitude, double latitude, byte value) {
		byteMap[assureRangeX(convertLong(longitude))][assureRangeY(convertLat(latitude))] = value;
	}
	
	public void setArea(double startLong, double startLat, double endLong, double endLat, byte value) {
		for (int x = assureRangeX(convertLong(startLong)); x < assureRangeX(convertLong(endLong)); x++) {
			for (int y = assureRangeY(convertLat(startLat)); y < assureRangeY(convertLat(endLat)); y++) {
				byteMap[x][y] = value;
			}
		}
	}
	
	public void setAreaIfBelow(double startLong, double startLat, double endLong, double endLat, byte value, byte below) {
		for (int x = assureRangeX(convertLong(startLong)); x < assureRangeX(convertLong(endLong)); x++) {
			for (int y = assureRangeY(convertLat(startLat)); y < assureRangeY(convertLat(endLat)); y++) {
				if (byteMap[x][y] < below) byteMap[x][y] = value;
			}
		}
	}
	
	public void setArea(int xStart, int yStart, int xEnd, int yEnd, byte value) {
		for (int x = assureRangeX(xStart); x < assureRangeX(xEnd); x++) {
			for (int y = assureRangeY(yStart); y < assureRangeY(yEnd); y++) {
				byteMap[x][y] = value;
			}
		}
	}
	
	public void incrementBelowValue(byte value) {
		for (int x = 0; x < xPoints; x++) {
			for (int y = 0; y < yPoints; y++) {
				if (byteMap[x][y] < value) byteMap[x][y] += 1;
			}
		}
	}
	
	public void decrementAboveValue(byte value) {
		for (int x = 0; x < xPoints; x++) {
			for (int y = 0; y < yPoints; y++) {
				if (byteMap[x][y] > value) byteMap[x][y] -= 1;
			}
		}
	}
	
	public void resize(int xChange, int yChange) {
		byte[][] newMap = new byte[xPoints + Math.abs(xChange)][yPoints + Math.abs(yChange)];
		int xOffset = 0;
		int yOffset = 0;
		double longPerX = longitude / (double) xPoints;
		double latPerY = latitude / (double) yPoints;
		if (xChange < 0) {
			xOffset = xChange;
			initLongitude += longPerX * (double) xChange;
		}
		if (yChange < 0) {
			yOffset = yChange;
			initLatitude += latPerY * (double) yChange;
		}
		for (int x = 0; x < xPoints; x++) {
			for (int y = 0; y < yPoints; y++) {
				newMap[x + xOffset][y + yOffset] = byteMap[x][y];
			}
		}
		xPoints += xChange;
		yPoints += yChange;
		longitude += longPerX * (double) Math.abs(xChange);
		latitude += latPerY * (double) Math.abs(yChange);
		byteMap = newMap;
	}
	
	// Utility Methods
	
	protected int assureRangeX(int x) {
		if (x < 0) x = 0;
		if (x >= xPoints) x = xPoints - 1;
		return x;
	}
	
	protected int assureRangeY(int y) {
		if (y < 0) y = 0;
		if (y >= yPoints) y = yPoints - 1;
		return y;
	}

	protected int convertLong(double longitude) {
		return (int) (((longitude - initLongitude) / this.longitude) * xPoints);
	}
	
	protected int convertLat(double latitude) {
		return (int) (((latitude - initLatitude) / this.latitude) * yPoints);
	}
	
	protected double convertXPoint(int xPoint) {
		return (((double) xPoint / (double) xPoints) * this.longitude) + initLongitude;
	}
	
	protected double convertYPoint(int yPoint) {
		return (((double) yPoint / (double) yPoints) * this.latitude) + initLatitude;
	}
	
	// Getters, Setters
	
	public byte[][] getByteMap() {
		return byteMap;
	}
	
	public int getXPoints() {
		return xPoints;
	}
	
	public int getYPoints() {
		return yPoints;
	}
	
	public double getInitLongitude() {
		return initLongitude;
	}
	
	public double getInitLatitude() {
		return initLatitude;
	}
	
	public double getLongitude() {
		return longitude;
	}
	
	public double getLatitude() {
		return latitude;
	}
	
}
