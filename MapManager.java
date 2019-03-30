package main;

import java.util.ArrayList;
import java.util.List;

public class MapManager {
	
	private ByteMap heatMap;
	private ByteMap pathMap;
	List<ByteMap> searchMaps = new ArrayList<>();
	
	private int[] resolutions;
	
	// Variables determine map resolution.
	protected int xPoints;
	protected int yPoints;
	
	public MapManager(double initLongitude, double initLatitude, double longitude, double latitude, int xPoints, int yPoints, int[] resolutions) {
		this.xPoints = xPoints;
		this.yPoints = yPoints;
		this.resolutions = resolutions;
		heatMap = new ByteMap(initLongitude, initLatitude, longitude, latitude, xPoints, yPoints);
		pathMap = new ByteMap(initLongitude, initLatitude, longitude, latitude, xPoints/resolutions[1], yPoints/resolutions[1]);
		for (int i = 0; i < resolutions.length; i++) {
			searchMaps.add(new ByteMap(initLongitude, initLatitude, longitude, latitude, xPoints/resolutions[i], yPoints/resolutions[i]));
		}
	}
	
	public void tick() {
		for (int i = 0; i < searchMaps.size(); i++) {
			searchMaps.get(i).incrementBelowValue((byte) -20);
			searchMaps.get(i).decrementAboveValue((byte) 50);
		}
	}
	
	/*
	public int getHeatTotal(double longitude, double latitude) {
		byte[][] byteMap = heatMap.getByteMap();
		int xStart = heatMap.convertLong(longitude) - 4;
		int yStart = heatMap.convertLat(latitude) - 4;
		int total = 0;
		for (int x = xStart; x < xStart + 8; x++) {
			for (int y = yStart; y < yStart + 8; y++) {
				total += byteMap[x][y];
			}
		}
	}
	*/
	
	public void setHazard(double longitude, double latitude, int resIndex) {
		checkResize(heatMap.convertLong(longitude), heatMap.convertLat(latitude));
		heatMap.setPoint(longitude, latitude, (byte) 100);
		pathMap.setPoint(longitude, latitude, (byte) 100);
		searchMaps.get(resIndex).setPointIncrementAdjacentThreshold(longitude, latitude, (byte) -100, (byte) 20, (byte) 60); // Only search around.
		for (int i = resIndex + 1; i < searchMaps.size(); i++) {
			searchMaps.get(i).setArea(longitude, latitude, resolutions[resIndex], resolutions[resIndex], (byte) 100);
		}
	}
	
	public void setSafe(double longitude, double latitude, int resIndex) {
		heatMap.setArea(longitude, latitude, resolutions[resIndex], resolutions[resIndex], (byte) -100);
		pathMap.setPoint(longitude, latitude, (byte) -100);
		searchMaps.get(resIndex).setPoint(longitude, latitude, (byte) -50);
		for (int i = resIndex + 1; i < searchMaps.size(); i++) {
			searchMaps.get(i).setAreaIfBelow(longitude, latitude, resolutions[resIndex], resolutions[resIndex], (byte) -50, (byte) 80);
		}
	}
	
	public void resize(int xChange, int yChange) {
		xPoints += Math.abs(xChange);
		yPoints += Math.abs(yChange);
		heatMap.resize(xChange * resolutions[0], yChange * resolutions[0]);
		for (int i = 0; i < searchMaps.size(); i++) {
			searchMaps.get(i).resize(xChange * resolutions[resolutions.length - 1 - i], yChange * resolutions[resolutions.length - 1 - i]);
		}
	}
	
	private void checkResize(int x, int y) {
		int xChange = 0;;
		int yChange = 0;
		if (x < 15)  {
			xChange = -1;// - x/resolutions[0];
		} else if (x >= xPoints - 15) {
			xChange = 1;// + (x-xPoints)/resolutions[0];
		}
		if (y < 15) {
			yChange = -1;// - y/resolutions[0];
		} else if (y >= yPoints - 15) {
			yChange = 1;// + (y-yPoints)/resolutions[0];
		}
		if (xChange != 0 || yChange != 0) {
			resize(xChange, yChange);
		}
	}
	
	public ByteMap getHeatMap() {
		return heatMap;
	}
	
	public ByteMap getPathMap() {
		return pathMap;
	}
	
	public ByteMap getSearchMap(int resIndex) {
		return searchMaps.get(resIndex);
	}
	
}
