package main;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import afrl.cmasi.*;
import afrl.cmasi.searchai.*;
import avtas.lmcp.*;

// Author: Kevin Wang
// Version: Prototype v3

public class Client {
	
	/*
	 * Potential Features:
	 * 		- Use live hazard zone spread data to predict future hazard zone spread.
	 *		- Proper contingency waypoints.
	 *		- Proper functionality near longitude or latitude of 0.
	 */
	
    private static int port = 5555;
    private static String host = "localhost";
    private List<LMCPObject> messageList = new ArrayList<>();
    
    private long time = 0;
    private int timeCount = 0;
    
    //private List<Entity> entityList = new ArrayList<>();
    private List<Drone> droneList = new ArrayList<>();
    private List<Location3D> locationList = new ArrayList<>();
    private List<HazardZoneDetection> newHazardList = new ArrayList<>();
    private List<LMCPObject> unhandled = new ArrayList<>();
    
    private Display display = new Display();
    private Control control;
    
    public static void main(String[] args) {
		Client client = new Client();
		client.mainLoop();
	}
    
	public void mainLoop() {
		try {
            Socket socket = connect(host, port);
            while (true) {
            	if (readMessages(socket.getInputStream())) {
            		if (control != null) {
            			control.tick(newHazardList, time);
            			newHazardList.clear();
            		} else if (timeCount > 2){
            			genSearchArea();
            		}
            		display.render();
            		writeMessages(socket.getOutputStream());
            	}
            }
        } catch (Exception ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(0);
        }
	}
    
    private Drone getDrone(long ID) {
    	for (Drone d : droneList) {
    		if (d.id() == ID) {
    			return d;
    		}
    	}
    	return null;
    }
    
    private boolean readMessages(InputStream in) throws Exception {
    	LMCPObject o = LMCPFactory.getObject(in);
    	if (o instanceof afrl.cmasi.searchai.HazardZoneDetection) {
        	HazardZoneDetection hzd = (HazardZoneDetection) o;
        	if (control == null) {
        		locationList.add(hzd.getDetectedLocation());
        	}
        	newHazardList.add(hzd);
        } else if (o instanceof afrl.cmasi.AbstractGeometry) {
            unhandled.add(o);
        } else if (o instanceof afrl.cmasi.KeyValuePair) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.Location3D) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.PayloadAction) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.PayloadConfiguration) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.PayloadState) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.VehicleAction) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.Task) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.SearchTask) {
        	unhandled.add(o);
        //} else if (o instanceof afrl.cmasi.AbstractZone) {
        //	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.EntityConfiguration) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.FlightProfile) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.AirVehicleConfiguration) {
            AirVehicleConfiguration avc = (afrl.cmasi.AirVehicleConfiguration) o;
            if (control == null) {
            	return false;
            }
            Drone d = getDrone(avc.getID());
            if (d == null) {
            	d = control.newDrone(avc.getID());
            	droneList.add(d);
            }
            d.setAVC(avc);
        } else if (o instanceof afrl.cmasi.AirVehicleState) {
            AirVehicleState avs = (AirVehicleState) o;
            if (control == null) {
            	locationList.add(avs.getLocation());
            	return false;
            }
            Drone d = getDrone(avs.getID());
            if (d == null) {
            	d = control.newDrone(avs.getID());
            	droneList.add(d);
            }
            d.setAVS(avs);
        } else if (o instanceof afrl.cmasi.EntityState) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.Wedge) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.AreaSearchTask) {
        	AreaSearchTask ast = (AreaSearchTask) o;
        	genSearchArea(ast.getSearchArea());
        } else if (o instanceof afrl.cmasi.CameraAction) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.CameraConfiguration) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.GimballedPayloadState) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.CameraState) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.Circle) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.GimbalAngleAction) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.GimbalConfiguration) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.GimbalScanAction) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.GimbalStareAction) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.GimbalState) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.GoToWaypointAction) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.KeepInZone) {
        	KeepInZone kiz = (KeepInZone) o;
        	if (control == null) {
        		genSearchArea(kiz.getBoundary());
        	} else {
        		// TODO Add to control
        	}
        } else if (o instanceof afrl.cmasi.KeepOutZone) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.LineSearchTask) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.NavigationAction) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.LoiterAction) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.LoiterTask) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.Waypoint) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.MissionCommand) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.MustFlyTask) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.OperatorSignal) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.OperatingRegion) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.AutomationRequest) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.PointSearchTask) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.Polygon) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.Rectangle) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.RemoveTasks) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.SessionStatus) {
            timeCount++;
        	time = ((SessionStatus) o).getScenarioTime();
        	if (time == 0) reset();
        	return true;
        } else if (o instanceof afrl.cmasi.VehicleActionCommand) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.VideoStreamAction) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.VideoStreamConfiguration) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.VideoStreamState) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.AutomationResponse) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.RemoveZones) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.RemoveEntities) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.FlightDirectorAction) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.WeatherReport) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.FollowPathCommand) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.PathWaypoint) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.StopMovementAction) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.WaypointTransfer) {
        	unhandled.add(o);
        } else if (o instanceof afrl.cmasi.PayloadStowAction) {
        	unhandled.add(o);
        } else {
            System.out.println("ERROR - Input Unreadable: " + o.getLMCPTypeName());
            unhandled.add(o);
        }
        return false;
    }
	
    // Write Methods
    
    public void addMessage(LMCPObject message) {
    	messageList.add(message);
    }
    
    private void writeMessages(OutputStream out) throws Exception {
    	for (LMCPObject message : messageList) {
    		out.write(LMCPFactory.packMessage(message, true));
    	}
    	messageList.clear();
    }

    // TODO Move to Control class.
    public void addEstimateReports(List<Polygon> estimateZones) throws Exception {
    	if (estimateZones == null) {
    		return;
    	}
    	HazardZoneEstimateReport hzer = new HazardZoneEstimateReport();
    	for (int i = 0; i < estimateZones.size(); i++) {
    		hzer.setEstimatedZoneShape(estimateZones.get(i));
            hzer.setUniqueTrackingID(i);
            hzer.setPerceivedZoneType(afrl.cmasi.searchai.HazardType.Fire);
            hzer.setEstimatedGrowthRate(0);
            hzer.setEstimatedZoneDirection(0);
            hzer.setEstimatedZoneSpeed(0);
            messageList.add(hzer);
    	}
    	
    }
    
    private void reset() {
    	timeCount = 0;
    	control = null;
    	droneList = new ArrayList<>();
        locationList = new ArrayList<>();
        newHazardList = new ArrayList<>();
        unhandled = new ArrayList<>();
        display.render();
    }
    
	private Socket connect(String host, int port) {
        Socket socket = null;
        try {
            socket = new Socket(host, port);
        } catch (UnknownHostException ex) {
            System.err.println("Host Unknown. Quitting");
            System.exit(0);
        } catch (IOException ex) {
            System.err.println("Could not Connect to " + host + ":" + port + ".  Trying again...");
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex1) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex1);
            }
            return connect(host, port);
        }
        System.out.println("Connected to " + host + ": " + port);
        return socket;
    }
	
	private void genSearchArea() {
    	double maxLongitude = locationList.get(0).getLongitude();
    	double minLongitude = maxLongitude;
    	double maxLatitude = locationList.get(0).getLatitude();
    	double minLatitude = maxLatitude;
    	
    	for (Location3D location : locationList) {
    		if (location.getLongitude() > maxLongitude) {
    			maxLongitude = location.getLongitude();
    		} else if (location.getLongitude() < minLongitude) {
    			minLongitude = location.getLongitude();
    		}
    		if (location.getLatitude() > maxLatitude) {
    			maxLatitude = location.getLatitude();
    		} else if (location.getLatitude() < minLatitude) {
    			minLatitude = location.getLatitude();
    		}
    	}
		control = new Control(this, droneList, minLongitude - 0.5 * (maxLongitude - minLongitude), maxLatitude + 0.5 * (maxLatitude - minLatitude), 2 * (maxLongitude - minLongitude), -(2 * (maxLatitude - minLatitude)));
		display.setByteMap(control.getMapManager().getSearchMap(0));
	}
	
	public void genSearchArea(AbstractGeometry area) {
		if (area instanceof Rectangle) {
    		Rectangle rectArea = (Rectangle) area;
    		Location3D center = rectArea.getCenterPoint();
    		double width = convertToDegrees(rectArea.getWidth());
    		double height = convertToDegrees(rectArea.getHeight());
    		float rotation = rectArea.getRotation();
    		double[] rotated1 = rotateAboutPoint(center.getLongitude() - (width/2), center.getLatitude() - (height/2), center.getLongitude(), center.getLatitude(), rotation);
    		double[] rotated2 = rotateAboutPoint(center.getLongitude() + (width/2), center.getLatitude() + (height/2), center.getLongitude(), center.getLatitude(), rotation);
    		control = new Control(this, droneList, rotated1[0], rotated1[1], rotated2[0] - rotated1[0], rotated2[1] - rotated1[1]);
    		display.setByteMap(control.getMapManager().getSearchMap(0));
    	} else if (area instanceof Circle) {
    		Circle circArea = (Circle) area;
    		Location3D center = circArea.getCenterPoint();
    		double radius = convertToDegrees(circArea.getRadius());
    		control = new Control(this, droneList, center.getLongitude() - radius, center.getLatitude() - radius, 2 * radius, 2 * radius);
    		display.setByteMap(control.getMapManager().getSearchMap(0));
    	} else if (area instanceof Polygon) {
    		Polygon polyArea = (Polygon) area;
    		List<Location3D> boundaryPoints = polyArea.getBoundaryPoints();
    		double maxLong = boundaryPoints.get(0).getLongitude();
    		double minLong = maxLong;
    		double maxLat = boundaryPoints.get(0).getLatitude();
    		double minLat = maxLat;
    		for (int i = 1; i < boundaryPoints.size(); i++) {
    			Location3D point = boundaryPoints.get(i);
    			double longitude = point.getLongitude();
    			double latitude = point.getLatitude();
    			if (longitude > maxLong) {
    				maxLong = longitude;
    			} else if (longitude < minLong) {
    				minLong = longitude;
    			}
    			if (latitude > maxLat) {
    				maxLat = latitude;
    			} else if (latitude < minLat) {
    				minLat = latitude;
    			}
    		}
    		control = new Control(this, droneList, minLong, minLat, maxLong - minLong, maxLat - minLat);
    		display.setByteMap(control.getMapManager().getSearchMap(0));
    	} else {
    		throw new UnsupportedOperationException("Subclass of AbstractGeometry not supported");
    	}
	}
	
	// Rotates point clockwise about centre by angle.
    private double[] rotateAboutPoint(double x, double y, double centerX, double centerY, float angle) {
    	angle = -angle; // convert to standard orientation
    	double[] result = new double[2];
    	result[0] = centerX + (x - centerX)*Math.cos(angle) - (y - centerY)*Math.sin(angle);
    	result[1] = centerY + (x - centerX)*Math.sin(angle) + (y - centerY)*Math.cos(angle);
    	return result;
    }
    
    private double convertToDegrees(float meters) {
    	return ((double) meters) / 111320d;
    }
	
	protected long getTime() {
		return time;
	}
	
}
