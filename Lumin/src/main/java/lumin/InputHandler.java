package lumin;

import java.awt.event.*;
import java.awt.Robot;
import static java.awt.event.KeyEvent.*;

public class InputHandler implements KeyListener, MouseListener, MouseWheelListener, MouseMotionListener {
	private Robot			mouseBot;		//For keeping the cursor in-screen
	public int			w;			// Canvas width
	public int			h;			// Canvas height
	public int			frameX, frameY;		// Canvas location
	public boolean[]		keys = new boolean[12];
	static final int[]		moveCodes =
	//Movement keys
		{VK_S, VK_W, VK_D, VK_A, VK_SHIFT, VK_SPACE,
	//Flashlight keys : R, G, B
		 VK_J, VK_U, VK_K, VK_I, VK_L, VK_O};
	public double pitch, yaw;

	public boolean[]		actions = new boolean[2];
	static final int[]		actionCodes = {VK_E, VK_Q};

	public boolean mouseDown = false;
	public int cumulativeMouseTicks = 0;

	public InputHandler() {
		try {
			mouseBot = new Robot();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int getVec(int dimension) {
		dimension *= 2;
		if (keys[dimension] == keys[dimension+1]) return 0;
		return keys[dimension] ? -1 : 1;
	}

	public void keyPressed(KeyEvent e){
		for (int i = 0; i < moveCodes.length; i++) {
			if (e.getKeyCode() == moveCodes[i]) {
				keys[i] = true;
				return;
			}
		}
		for (int i = 0; i < actionCodes.length; i++) {
			if (e.getKeyCode() == actionCodes[i]) {
				actions[i] = true;
				return;
			}
		}
	}

	public void keyReleased(KeyEvent e){
		for (int i = 0; i < moveCodes.length; i++) {
			if (e.getKeyCode() == moveCodes[i]) {
				keys[i] = false;
				return;
			}
		}
	}

	public void keyTyped(KeyEvent e){}

	public void mouseMoved(MouseEvent e) {
		int cx = frameX + w/2;
		int cy = frameY + h/2;
		int dx = e.getX() - w/2;
		int dy = e.getY() - h/2;
		if (dx == 0 && dy == 0) return;
		mouseBot.mouseMove(cx, cy);
		if (Math.abs(dx) > 40 || Math.abs(dy) > 40) return;
		pitch -= dy * 0.01;
		if (pitch < -Math.PI/2) pitch = -Math.PI/2;
		else if (pitch > Math.PI/2) pitch = Math.PI/2;
		yaw -= dx * 0.01;
		if (yaw > Math.PI) yaw -= Math.PI*2;
		else if (yaw < -Math.PI) yaw += Math.PI*2;
	}

	public void mouseDragged(MouseEvent e) {
		mouseMoved(e);
	}

	public void mousePressed(MouseEvent e) {mouseDown = true;}
	public void mouseReleased(MouseEvent e) {mouseDown = false;}
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {mouseDown = false;}

	public void mouseWheelMoved(MouseWheelEvent e) {
		cumulativeMouseTicks += e.getWheelRotation();
	}
}
