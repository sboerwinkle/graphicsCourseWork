package lumin;

import java.awt.event.*;
import java.awt.Robot;
import static java.awt.event.KeyEvent.*;

public class InputHandler implements KeyListener, MouseMotionListener {
	private Robot			mouseBot;		//For keeping the cursor in-screen
	public int			w;			// Canvas width
	public int			h;			// Canvas height
	public int			frameX, frameY;		// Canvas location
	public boolean[]		keys = new boolean[12];
	static int[]			codes =
	//Movement keys
		{VK_S, VK_W, VK_D, VK_A, VK_SHIFT, VK_SPACE,
	//Keys to move the shadow box
		 VK_H, VK_F, VK_R, VK_Y, VK_G, VK_T};
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
		return keys[dimension] ? -1 : 1;
	}

	public void keyPressed(KeyEvent e){
		for (int i = 0; i < codes.length; i++) {
			if (e.getKeyCode() == codes[i]) {
				keys[i] = true;
				return;
			}
		}
		if (e.getKeyCode() == VK_J) yaw += 0.05;
		if (e.getKeyCode() == VK_L) yaw -= 0.05;
		if (e.getKeyCode() == VK_I) pitch += 0.05;
		if (e.getKeyCode() == VK_K) pitch -= 0.05;
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
