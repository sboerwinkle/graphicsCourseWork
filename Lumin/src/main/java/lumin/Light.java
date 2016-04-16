package lumin;

import java.util.ArrayList;
import javax.media.opengl.*;

class Light {
	float[] pos;
	float[] color;

	Light(double x, double y, double z) {
		pos = new float[] {(float)x, (float)y, (float)z, 1};
		color = new float[] {1, 1, 1, 1};
	}

	public void render(GL2 gl, ArrayList<Item> items) {
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, pos, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, color, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, color, 0);

		Shadows.shadowPrep(gl);
		for (Item i : items) i.renderShadow(gl, pos);
		Shadows.renderPrep(gl);
		for (Item i : items) i.render(gl);
		Shadows.cleanup(gl);
	}
}
