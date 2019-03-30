package main;

import java.util.ArrayList;
import java.util.List;

import afrl.cmasi.AbstractGeometry;
import afrl.cmasi.AltitudeType;
import afrl.cmasi.Circle;
import afrl.cmasi.Location3D;
import afrl.cmasi.Polygon;
import afrl.cmasi.Rectangle;
import afrl.cmasi.VehicleActionCommand;
import afrl.cmasi.searchai.HazardZoneDetection;

// Author: Kevin Wang

public class Control {
	
	private final Client client;
	private final List<Drone> droneList;
	private final MapManager mapManager;
	
	// Search Area
	private double initLongitude;
	private double initLatitude;
	private double longitude;
	private double latitude;
	
	// Search Resolutions
	private final int mapXPoints = 90;
	private final int mapYPoints = 90;
	private final int[] resolutions = {6, 3, 1};
	
	private long lastTime = 0;
	boolean first;
	
	public Control(Client client, List<Drone> droneList, double initLongitude, double initLatitude, double longitude, double latitude) {
		this.client = client;
		this.droneList = droneList;
		mapManager = new MapManager(initLongitude, initLatitude, longitude, latitude, mapXPoints, mapYPoints, resolutions);
		first = true;
	}
	
	// Primary Interface Method
	
	public void tick(List<HazardZoneDetection> detectionList, long time) throws Exception {
		mapManager.tick();
		for (HazardZoneDetection hzd : detectionList) {
			Drone d = getDrone(hzd.getDetectingEnitiyID());
			if (d != null) {
				d.updateDetection(hzd);
			} else {
				Location3D location = hzd.getDetectedLocation();
				mapManager.setHazard(location.getLongitude(), location.getLatitude(), 0);
			}
		}
		for (Drone d : droneList) {
			d.tick(time);
		}
		updateSearchResolutions();
		distributeSearch(time);
		if (time - lastTime > 5000 || first) {
			first = false;
			lastTime = time;
			client.addEstimateReports(buildHazardZones());
		}
	}
	
	// Core Methods
	
	private void updateSearchResolutions() {
		
	}
	
	private void distributeSearch(long time) {
		byte[][] pathMap = mapManager.getPathMap().getByteMap();
		for (int resIndex = 0; resIndex < resolutions.length; resIndex++) {
			List<Drone> usingDrones = new ArrayList<>();
			List<int[]> usedNodes = new ArrayList<>();
			ByteMap map = mapManager.getSearchMap(resIndex);
			for (Drone drone : droneList) {
				if (drone.getResolutionIndex() == resIndex) {
					if (drone.onMission()) {
						int[] node = new int[2];
						node[0] = map.convertLong(drone.getTargetLongitude());
						node[1] = map.convertLat(drone.getTargetLatitude());
						usedNodes.add(node);
					} else {
						usingDrones.add(drone);
					}
				}
			}
			if (usingDrones.isEmpty()) continue;
			
			byte[][] searchMap = map.getByteMap();
			
			List<int[]> searchNodes = new ArrayList<>();
			for (int x = 0; x < searchMap.length; x++) {
				for (int y = 0; y < searchMap[0].length; y++) {
					int[] newNode = {x, y, searchMap[x][y]};
					boolean used = false;
					for (int[] node : usedNodes) {
						if (node[0] == newNode[0] && node[1] == newNode[1]) {
							used = true;
							break;
						}
					}
					if (!used) {
						searchNodes.add(newNode);
					}
				}
			}
			
			if (searchNodes.isEmpty()) {
				System.out.println("Error - Coordination system failure, no available nodes at resolution.");
				System.exit(0);
			}
			
			double[][] moveScore = new double[usingDrones.size()][searchNodes.size()];
			for (int i = 0; i < usingDrones.size(); i++) {
				Location3D droneLocation = usingDrones.get(i).avs().getLocation();
				if (droneLocation == null) continue;
				for (int j = 0; j < searchNodes.size(); j++) {
					int[] node = searchNodes.get(j);
					double distance = Math.hypot(droneLocation.getLatitude() - map.convertYPoint(node[1]), droneLocation.getLongitude() - map.convertXPoint(node[0]));
					moveScore[i][j] = (double) node[2] - distance * 100;
				}
				// TODO Move score adjustments based on individual drone distance rankings.
			}
			
			double longPerX = map.getLongitude() / map.getXPoints();
			double longPerY = map.getLatitude() / map.getYPoints();
			int xIndex = 0;
			int yIndex = 0;
			for (int i = 0; i < usingDrones.size(); i++) {
				double scoreMax = -99999;
				for (int j = 0; j < usingDrones.size(); j++) {
					for (int k = 0; k < searchNodes.size(); k++) {
						if (moveScore[j][k] > scoreMax) {
							scoreMax = moveScore[j][k];
							xIndex = j;
							yIndex = k;
						}
					}
				}
				
				int[] node = searchNodes.get(yIndex);
				
				Drone d = usingDrones.get(xIndex);
				d.searchLocation(map.convertXPoint(node[0]) + longPerX/2, map.convertYPoint(node[1]) + longPerY/2, time);
				
				for (int l = 0; l < searchNodes.size(); l++) {
					moveScore[xIndex][l] = -99999;
				}
				for (int m = 0; m < usingDrones.size(); m++) {
					moveScore[m][yIndex] = -99999;
				}
				// TODO Move score adjustment based on future drone positions, to increase separation.
			}
		
		}
	}
	
	
	private List<Polygon> buildHazardZones() {
		List<Polygon> hazardZoneList = new ArrayList<>();
		
		ByteMap heatMap = mapManager.getHeatMap();
		byte[][] byteMap = heatMap.getByteMap();
		List<int[]> edgePoints = new ArrayList<>();
		int[][] checkPositions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
		for (int x = 0; x < mapXPoints; x++) {
			for (int y = 0; y < mapYPoints; y++) {
				if (byteMap[x][y] != 100) continue;
				for (int i = 0; i < checkPositions.length; i++) {
					int[] checkPosition = checkPositions[i];
					if (byteMap[x+checkPosition[0]][y+checkPosition[1]] != 100) {
						int[] edgePoint = {x, y};
						edgePoints.add(edgePoint);
						break;
					}
				}
			}
		}
		if (edgePoints.isEmpty()) return hazardZoneList;
		
		for (int i = 0; i < edgePoints.size(); i++) {
			int[] edgePoint = edgePoints.get(i);
			if (hazardZoneList.isEmpty()) {
				Polygon newZone = new Polygon();
				newZone.getBoundaryPoints().add(getLocation(edgePoint[0], edgePoint[1]));
				hazardZoneList.add(newZone);
				continue;
			}
			
			List<Double> scores = new ArrayList<>();
			int lineSteps = 30;
			for (Polygon zone : hazardZoneList) {
				List<Location3D> boundaryPts = zone.getBoundaryPoints();
				int safeCount = 0;
				for (Location3D point : boundaryPts) {
					double xStep = (double) (edgePoint[0] - heatMap.convertLong(point.getLongitude())) / (double) lineSteps;
					double yStep = (double) (edgePoint[1] - heatMap.convertLat(point.getLatitude())) / (double) lineSteps;
					for (int j = 0; j < lineSteps; j++) {
						int xPos = (int) (edgePoint[0] - (xStep * j));
						int yPos = (int) (edgePoint[1] - (yStep * j));
						if (byteMap[xPos][yPos] < 0) {
							safeCount++;
						}
					}
				}
				
				double score = 0;
				if (safeCount < boundaryPts.size() * 10) {
					Location3D centrePoint = getCentrePoint(boundaryPts);
					double distanceToCentre = Math.hypot(edgePoint[0] - heatMap.convertLong(centrePoint.getLongitude()), edgePoint[1] - heatMap.convertLat(centrePoint.getLatitude()));
					score = boundaryPts.size() + 100 / ((double)(safeCount + 1) * distanceToCentre);
				}
				scores.add(score);
			}
			
			// Determine max scoring polygon to add to.
			int scoreMaxIndex = 0;
			for (int j = 1; j < hazardZoneList.size(); j++) {
				if (scores.get(j) > scores.get(scoreMaxIndex)) {
					scoreMaxIndex = j;
				}
			}
			
			if (scores.get(scoreMaxIndex) > 0) {
				hazardZoneList.get(scoreMaxIndex).getBoundaryPoints().add(getLocation(edgePoint[0], edgePoint[1]));
			} else {
				Polygon newZone = new Polygon();
				newZone.getBoundaryPoints().add(getLocation(edgePoint[0], edgePoint[1]));
				hazardZoneList.add(newZone);
			}
		}
		
		// Merge similar centred hazard zones.
		for (int i = 0; i < hazardZoneList.size(); i++) {
			for (int j = i + 1; j < hazardZoneList.size(); j++) {
				List<Location3D> zone1Pts = hazardZoneList.get(i).getBoundaryPoints();
				List<Location3D> zone2Pts = hazardZoneList.get(j).getBoundaryPoints();
				Location3D centre1 = getCentrePoint(zone1Pts);
				Location3D centre2 = getCentrePoint(zone2Pts);
				if (Math.abs(centre1.getLongitude() - centre2.getLongitude()) < 0.002 && Math.abs(centre1.getLatitude() - centre2.getLatitude()) < 0.002) {
					for (Location3D point : zone2Pts) {
						zone1Pts.add(point);
					}
					hazardZoneList.remove(j);
				}
			}
		}
		
		for (int i = 0; i < hazardZoneList.size(); i++) {
			Polygon hazardZone = hazardZoneList.get(i);
			if (hazardZone.getBoundaryPoints().size() == 1) {
				hazardZoneList.remove(hazardZone);
				i--;
			} else {
				orderPointsAngleCentre(hazardZone);
			}
		}
		
		return hazardZoneList;
	}
	
	// Utility
	
	public Drone newDrone(long ID) {
		return new Drone(client, mapManager, ID);
	}
	
	private Drone getDrone(long ID) {
    	for (Drone d : droneList) {
    		if (d.id() == ID) {
    			return d;
    		}
    	}
    	return null;
    }
	
	private Location3D getLocation(int xPoint, int yPoint) {
		return new Location3D(mapManager.getHeatMap().convertYPoint(yPoint), mapManager.getHeatMap().convertXPoint(xPoint), 0, AltitudeType.AGL);
	}
	
	// Given a polygon, re-orders the boundary points such that they are, by angle, positioned clockwise about their 2D centre.
	private void orderPointsAngleCentre(Polygon polygon) {
		List<Location3D> boundaryPts = polygon.getBoundaryPoints();

		Location3D centrePoint = getCentrePoint(boundaryPts);
		double centreLong = centrePoint.getLongitude();
		double centreLat = centrePoint.getLatitude();
		
		// Calculate angle relative to centre point and add to angleList
		List<Double> angleList = new ArrayList<>();
		for (int i = 0; i < boundaryPts.size(); i++) {
			Location3D point = boundaryPts.get(i);
			angleList.add(Math.atan2(point.getLatitude() - centreLat, point.getLongitude() - centreLong));
		}
		
		if (angleList.size() > 3) {
			for (int i = 0; i < angleList.size(); i++) {
				for (int j = i + 1; j < angleList.size(); j++) {
					double angleThis = angleList.get(i);
					double angleOther = angleList.get(j);
					if (Math.abs(angleThis - angleOther) < Math.PI/20) { // TODO Angle threshold adjustments based on distance.
						// Remove closest to centre, then adjust indexes.
						Location3D pointThis = boundaryPts.get(i);
						Location3D pointOther = boundaryPts.get(j);
						double distanceToThis = Math.hypot(centreLong - pointThis.getLongitude(), centreLat - pointThis.getLatitude());
						double distanceToOther = Math.hypot(centreLong - pointOther.getLongitude(), centreLat - pointOther.getLatitude());
						if (distanceToThis < distanceToOther) {
							angleList.remove(i);
							boundaryPts.remove(i);
							if (i > 0) i--;
							j = i;
						} else {
							angleList.remove(j);
							boundaryPts.remove(j);
							j--;
							break;
						}
					}
				}
			}
		}
		
		// Order List
		while (true) {
			boolean isDisorder = false;
			for (int i = 1; i < angleList.size(); i++) {
				if (angleList.get(i) < angleList.get(i-1)) {
					double temp = angleList.get(i);
					angleList.set(i, angleList.get(i-1));
					angleList.set(i-1, temp);
					Location3D temp2 = boundaryPts.get(i);
					boundaryPts.set(i, boundaryPts.get(i-1));
					boundaryPts.set(i-1, temp2);
					isDisorder = true;
				}
			}
			if (!isDisorder) {
				break;
			}
		}
	}
	
	private Location3D getCentrePoint(List<Location3D> points) {
		if (points.isEmpty()) {
			throw new IllegalArgumentException();
		}
		double maxLong = -99999;
		double minLong = 99999;
		double maxLat = -99999;
		double minLat = 99999;
		for (int i = 0; i < points.size(); i++) {
			Location3D point = points.get(i);
			double longitude = point.getLongitude();
			double latitude = point.getLatitude();
			if (longitude > maxLong) {
				maxLong = longitude;
			}
			if (longitude < minLong) {
				minLong = longitude;
			}
			if (latitude > maxLat) {
				maxLat = latitude;
			}
			if (latitude < minLat) {
				minLat = latitude;
			}
		}
		double centreLong = minLong + ((maxLong - minLong) / 2);
		double centreLat = minLat + ((maxLat - minLat) / 2);
		return new Location3D(centreLat, centreLong, 0, AltitudeType.AGL);
	}
	
	// Getters, Setters
	
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
	
	public MapManager getMapManager() {
		return mapManager;
	}
}
