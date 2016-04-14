package lumin;

import javax.media.opengl.*;

abstract class Item {

	float[] pos;

	public Item(double X, double Y, double Z) {
		pos = new float[3];
		pos[0] = (float)X;
		pos[1] = (float)Y;
		pos[2] = (float)Z;
	}

	abstract void render(GL2 gl);
	abstract void renderShadow(GL2 gl, float[] lightPos);
}
