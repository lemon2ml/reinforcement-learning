package com.batalov.RL;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Rendering of the Rocket Lander in a JPanel. Left arrow on the keyboard fires the left engine and right arrow - the right one.
 * @author denisb
 */
public class RocketLanderView extends JPanel {
	@SuppressWarnings("unused")
	private static final Logger LOG = Logger.getLogger(RocketLanderView.class.getName());

	private static final long serialVersionUID = -7090833214585141686L;

	private Image stars;
	private Image ground;
	private boolean realTime = true;

	private volatile RocketLander lander;

	public RocketLanderView(final RocketLander lander) {
		this.lander = lander;
		try {
			this.stars = ImageIO.read(new File("./resources/stars2.png"));
			this.ground = ImageIO.read(new File("./resources/surface.png"));
			this.stars = this.scaleDownBy(this.stars, 2);
			this.ground = this.scaleDownBy(this.ground, 2);
		} catch (final IOException ioe) {
			ioe.printStackTrace();
		}
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(final MouseEvent e) {
				RocketLanderView.this.switchRealTime();
			}
		});
		this.addKeyListener(new KeyAdapter() {
			public void keyPressed(final KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_LEFT) {
					updateBurnLeft(true);
				}
				else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
					updateBurnRight(true);
				}
				System.out.println(lander);
			}
			public void keyReleased(final KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_LEFT) {
					updateBurnLeft(false);
				}
				else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
					updateBurnRight(false);
				}
			}
		});
	}

	public RocketLander getRocketLander() {
		return this.lander;
	}

	public void setRocketLander(final RocketLander lander) {
		this.lander = lander;
	}

	public synchronized void switchRealTime() {
		this.realTime = !this.realTime;
		System.out.println("real-time = " + this.realTime);
	}

	public synchronized boolean isRealTime() {
		return this.realTime;
	}

	private Image scaleDownBy(final Image img, final int factor) {
		return img.getScaledInstance(img.getWidth(this) / factor, img.getHeight(this) / factor, Image.SCALE_DEFAULT);
	}

	public Dimension getPreferredSize() {
		return new Dimension(500, 1000);
	}

	private int getGroundHeight() {
		return this.ground.getHeight(this);
	}

	private int transformY(final int y) {
		return Math.round(this.getSize().height - this.getGroundHeight()) - y;
	}

	private static final float PIXELS_PER_METER = 5;

	private static int metersToPixels(final float meters) {
		return Math.round(meters * PIXELS_PER_METER);
	}

	@SuppressWarnings("unused")
	private static float pixelsToMeters(final int pixels) {
		return pixels / PIXELS_PER_METER;
	}

	public void paintComponent(final Graphics g) {
		
		final int xCenter = this.getWidth()/2;
		final int xOrigin = xCenter - metersToPixels((float)this.lander.getPosition().getX());
		final int xBackground = xOrigin % 250 - this.stars.getWidth(this)/2 + this.getWidth()/2;
		int yBackground = this.getHeight() - this.stars.getHeight(this) - this.getGroundHeight();

		int yGround = this.getHeight() - this.getGroundHeight();

		int yRocket = this.transformY(metersToPixels((float)this.lander.getPosition().getY()));
		if (yRocket <= this.getHeight()/4) { // mode 1 - far above ground
			final int yOffset = this.getHeight()/4 - yRocket;
			final int adjustment = yRocket < 0 ? this.getHeight()/4 : this.getHeight()/4 - yRocket;

			yGround += adjustment + yOffset;

			yBackground += adjustment;
			yBackground += 444 + yOffset % 444;

			yRocket = this.getHeight()/4 + adjustment;
		}

		// paint background
		if (this.stars != null) {
			g.drawImage(this.stars, xBackground, yBackground, this);
		}

		// paint ground
		if (this.ground != null && yGround < this.getHeight()) {
			g.drawImage(this.ground, xBackground, yGround, this);

			// draw ground line
			g.setColor(Color.BLACK);
			g.drawLine(0, yGround, this.getWidth(), yGround);
			// draw landing zone
			final int xZoneRadius = 200;
			final int yZoneRadius = this.getGroundHeight();
			g.setColor(Color.WHITE);
			g.fillArc(xOrigin-xZoneRadius, yGround-yZoneRadius, xZoneRadius*2, yZoneRadius*2, 180, 180);
			// draw origin on ground
			g.setColor(Color.BLACK);
			g.drawLine(xOrigin, yGround, xOrigin, yGround + metersToPixels(5));
		}

		this.drawRocketShip(g, xCenter, yRocket);
	}

	private void drawRocketShip(final Graphics g, final int xRocket, int yRocket) {
		// draw rocket ship
		g.setColor(Color.BLUE);
		final float bodyRadiusMeters = 10;
		final int bodyRadius = metersToPixels(bodyRadiusMeters); // pixels
		yRocket -= bodyRadius; 
		g.fillArc(xRocket-bodyRadius, yRocket-bodyRadius, bodyRadius*2, bodyRadius*2, (int)Math.round(Math.toDegrees(this.lander.getRotation()+Math.PI)), 180);
		g.setColor(new Color(0xBCC6CC));
		g.fillArc(xRocket-bodyRadius, yRocket-bodyRadius, bodyRadius*2, bodyRadius*2, (int)Math.round(Math.toDegrees(this.lander.getRotation())), 180);
		// draw horizontal beam
		final int xProjection = (int)Math.round(2*bodyRadius*Math.cos(this.lander.getRotation()));
		final int yProjection = (int)Math.round(2*bodyRadius*Math.sin(this.lander.getRotation()));
		g.setColor(Color.BLUE);
		g.drawLine(xRocket-xProjection, yRocket+yProjection, xRocket+xProjection, yRocket-yProjection);
		
		// draw engine fire
		g.setColor(Color.RED);
		if (this.lander.getBurnLeft()) {
			g.fillArc(xRocket-xProjection-bodyRadius, yRocket+yProjection-bodyRadius, bodyRadius*2, bodyRadius*2, (int)Math.round(Math.toDegrees(this.lander.getRotation())+260), 20);
		}
		if (this.lander.getBurnRight()) {
			g.fillArc(xRocket+xProjection-bodyRadius, yRocket-yProjection-bodyRadius, bodyRadius*2, bodyRadius*2, (int)Math.round(Math.toDegrees(this.lander.getRotation())+260), 20);
		}
	}

	public void updateBurnLeft(final boolean burnLeft) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				RocketLanderView.this.lander.setBurnLeft(burnLeft);
				System.out.println(RocketLanderView.this.lander);
				RocketLanderView.this.repaint();
			}
		});
	}

	public void updateBurnRight(final boolean burnRight) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				RocketLanderView.this.lander.setBurnRight(burnRight);
				System.out.println(RocketLanderView.this.lander);
				RocketLanderView.this.repaint();
			}
		});
	}
}