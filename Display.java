package main;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;

import javax.swing.JFrame;

// Author: Kevin Wang

public class Display {
	
	private final JFrame frame  = new JFrame();
	private final Canvas canvas = new Canvas();
	
	private final int width = 550, height = width;
	
	private ByteMap byteMap = null;
	
	private final int colorRange = 40;
	private Color[] colors = new Color[colorRange + 1];
	
	public Display() {
		frame.setTitle("FireHack Client - SearchMap");
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        canvas.setPreferredSize(new Dimension(width, height));
        canvas.setMaximumSize(new Dimension(width, height));
        canvas.setMinimumSize(new Dimension(width, height));
        
        frame.getContentPane().add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        initColors();
	}
	
	private void initColors() {
		for (int i = 0; i <= colorRange; i++) {
			int value = (int) ((255d / colorRange) * i);
			colors[i] = new Color(255, value, value);
		}
	}
	
	public void render() {
		BufferStrategy bs = canvas.getBufferStrategy();
        if (bs == null) {
            canvas.createBufferStrategy(3);
        } else {
            Graphics g = bs.getDrawGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, width, height);
            if (byteMap != null) {
            	drawHeatMap(g);
    		}
            bs.show();
            g.dispose();
        }
	}
	
	private void drawHeatMap(Graphics g) {
		byte[][] map = byteMap.getByteMap();
		int divisor = 200 / colorRange;
		for (int x = 0; x < map.length; x++) {
			for (int y = 0; y < map[0].length; y++) {
				g.setColor(colors[(200 - (map[x][y] + 100)) / divisor]);
				int xPos = 5 + (((width - 10) * x) / map.length);
				int yPos = 5 + (((height - 10) * y) / map.length);
				g.fillRect(xPos, yPos, 36, 36);
			}
		}
	}
	
	public void setByteMap(ByteMap byteMap) {
		this.byteMap = byteMap;
	}
	
}
