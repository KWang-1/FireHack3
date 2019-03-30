package main;

import java.util.ArrayList;
import java.util.List;

import afrl.cmasi.AirVehicleConfiguration;
import afrl.cmasi.AirVehicleState;
import afrl.cmasi.AltitudeType;
import afrl.cmasi.CommandStatusType;
import afrl.cmasi.GimbalScanAction;
import afrl.cmasi.GimbalStareAction;
import afrl.cmasi.Location3D;
import afrl.cmasi.MissionCommand;
import afrl.cmasi.SpeedType;
import afrl.cmasi.TurnType;
import afrl.cmasi.VehicleActionCommand;
import afrl.cmasi.Waypoint;
import afrl.cmasi.searchai.HazardZoneDetection;

// Author: Kevin Wang

public class Drone {
	
	private Client client;
	private MapManager mapManager;
	
	private long ID;
	private AirVehicleState avs;
	private AirVehicleConfiguration avc;
	private List<HazardZoneDetection> detectionList = new ArrayList<>();
	private List<int[]> searchNodes = new ArrayList<>();
	
	// Movement Settings
	private AltitudeType altitudeType = AltitudeType.AGL;
	private SpeedType speedType = SpeedType.Airspeed;
	private TurnType turnType = TurnType.FlyOver;
	private float altitude = 300;
	private float climb = 10;
	
	// Settings
	private final double standOff = 0.002d;
	private final long detectionWindow = 600;
	
	private long stareStartTime = 0;
	private double targetLong = 0;
	private double targetLat = 0;
	private double moveLong = 0;
	private double moveLat = 0;
	
	private boolean onMission = false;
	private boolean isStaring = false;
	
	private int resIndex = 0;
	
	// Used in recursive path-finding method.
	private ByteMap map;
	private byte[][] pathMap;

	public Drone(Client client, MapManager mapManager, long id) {
		this.client = client;
		this.mapManager = mapManager;
		this.map = mapManager.getPathMap();
		this.ID = id;
	}
	
	// Primary Interface Methods
	
	public void tick(long time) {
		if (!isStaring) {
			stareStartTime = time;
		}
		
		if (onMission && passedDetectionWindow(time)) {
			endMission();
			mapManager.setSafe(targetLong, targetLat, resIndex);
		}
		
		if (onMission && targetWithinSensorRange() && !isStaring) {
			MissionCommand missionCommand = new MissionCommand();
			missionCommand.setVehicleID(ID);
			missionCommand.setStatus(CommandStatusType.Pending);
			
			int speed = 15;
			GimbalStareAction gsa = new GimbalStareAction();
			gsa.setPayloadID(1);
			gsa.setDuration(1000000);
			gsa.setStarepoint(new Location3D(targetLat, targetLong, 0, AltitudeType.MSL));
			missionCommand.getVehicleActionList().add(gsa);
			
			Waypoint wp = new Waypoint();
			wp.setNumber(1);
			wp.setLongitude(moveLong);
			wp.setLatitude(moveLat);
			wp.setAltitude(altitude);
			wp.setAltitudeType(altitudeType);
			wp.setSpeed(speed);
			wp.setSpeedType(speedType);
			wp.setClimbRate(climb);
			wp.setTurnType(turnType);
			wp.setContingencyWaypointA(1); // TODO Contingency
	        wp.setContingencyWaypointB(1);
			wp.setNextWaypoint(1);
			missionCommand.getWaypointList().add(wp);
			missionCommand.setFirstWaypoint(1);
			
			client.addMessage(missionCommand);
			isStaring = true;
		}
		
	}
	
	public void updateDetection(HazardZoneDetection detection) {
		Location3D location = detection.getDetectedLocation();
		mapManager.setHazard(location.getLongitude(), location.getLatitude(), resIndex);
		if (Math.abs(location.getLongitude() - targetLong) < 0.0003 && Math.abs(location.getLatitude() - targetLat) < 0.0003) {
			endMission();
		}
	}
	
	public void searchLocation(double longitude, double latitude, long startTime) {
		onMission = true;
		
		double angle = Math.atan2(latitude - avs.getLocation().getLatitude(), longitude - avs.getLocation().getLongitude());
		
		
		moveLong = longitude - (standOff * Math.cos(angle));
		moveLat = latitude - (standOff * Math.sin(angle));
		
		targetLong = longitude;
		targetLat = latitude;
		
		MissionCommand missionCommand = new MissionCommand();
		missionCommand.setVehicleID(ID);
		missionCommand.setStatus(CommandStatusType.Pending);

		int speed = 30;
		if (targetWithinSensorRange()) {
			speed = 15;
			GimbalStareAction gsa = new GimbalStareAction();
			gsa.setPayloadID(1);
			gsa.setDuration(1000000);
			gsa.setStarepoint(new Location3D(targetLat, targetLong, 0, AltitudeType.MSL));
			missionCommand.getVehicleActionList().add(gsa);
			isStaring = true;
			stareStartTime = startTime;
		} else {
			// TEMP
			GimbalStareAction gsa = new GimbalStareAction();
			gsa.setPayloadID(1);
			gsa.setDuration(1000000);
			gsa.setStarepoint(new Location3D(targetLat, targetLong, 0, AltitudeType.MSL));
			missionCommand.getVehicleActionList().add(gsa);
			isStaring = false;
			/*
			GimbalScanAction scan = new GimbalScanAction();
			scan.setAzimuthSlewRate(1000f);
			scan.setStartAzimuth(300f);
			scan.setEndAzimuth(60f);
			isStaring = false;
			*/
		}
		
		// If not in safe zone, take direct route to safe zone.
		Waypoint wp = new Waypoint();
		wp.setNumber(1);
		wp.setLongitude(moveLong);
		wp.setLatitude(moveLat);
		wp.setAltitude(altitude);
		wp.setAltitudeType(altitudeType);
		wp.setSpeed(speed);
		wp.setSpeedType(speedType);
		wp.setClimbRate(climb);
		wp.setTurnType(turnType);
		wp.setContingencyWaypointA(1);
        wp.setContingencyWaypointB(1);
		wp.setNextWaypoint(1);
		missionCommand.getWaypointList().add(wp);
		
		missionCommand.setFirstWaypoint(1);
		client.addMessage(missionCommand);
	}
	
	// Secondary Methods
	
	public boolean passedDetectionWindow(long time) {
		if (time - stareStartTime > detectionWindow) {
			return true;
		}
		return false;
	}
	
	public boolean targetWithinSensorRange() {
		Location3D location = avs.getLocation();
		double distance = Math.hypot(location.getLatitude() - targetLat, location.getLongitude() - targetLong);
		if (distance < 0.004) { // True range 0.0045, but allow communication delay error margin
			return true;
		}
		return false;
	}
	
	public boolean onMission() {
		return onMission;
	}
	
	public void endMission() {
		onMission = false;
	}
	
	
	public void addHazardDetection(HazardZoneDetection hzd) {
		detectionList.add(hzd);
	}
	
	public void assignNode(int[] node) {
		searchNodes.add(node);
	}
	
	public void clearNodes() {
		searchNodes = new ArrayList<>();
	}
	
	// Getters, Setters
	
	public int getResolutionIndex() {
		return resIndex;
	}
	
	public void setResolutionIndex(int resolutionIndex) {
		this.resIndex = resolutionIndex;
		// TODO Reset search nodes
	}
	
	public void setAVS(AirVehicleState avs) {
		this.avs = avs;
	}
	
	public void setAVC(AirVehicleConfiguration avc) {
		this.avc = avc;
	}
	
	public long id() {
		return ID;
	}
	
	public AirVehicleState avs() {
		return avs;
	}
	
	public AirVehicleConfiguration avc() {
		return avc;
	}
	
	public double getTargetLongitude() {
		return targetLong;
	}
	
	public double getTargetLatitude() {
		return targetLat;
	}
	
}
