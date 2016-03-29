package lumin;

import java.awt.event.*;
import java.awt.Robot;

public class InputHandler implements KeyListener, MouseMotionListener {
	private Robot			mouseBot;		//For keeping the cursor in-screen
	public int			w;			// Canvas width
	public int			h;			// Canvas height
	public int			frameX, frameY;		// Canvas location
	public boolean[]		keys = new boolean[6];
	static int[]			codes =
		{KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_SPACE, KeyEvent.VK_SHIFT};
	public double pitch, yaw;

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
		return keys[dimension] ? 1 : -1;
	}

	public void keyPressed(KeyEvent e){
		for (int i = 0; i < codes.length; i++) {
			if (e.getKeyCode() == codes[i]) {
				keys[i] = true;
				return;
			}
		}
	}

	public void keyReleased(KeyEvent e){
		for (int i = 0; i < codes.length; i++) {
			if (e.getKeyCode() == codes[i]) {
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

	public void mouseDragged(MouseEvent e) {}
}
