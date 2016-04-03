package lumin;

import javax.media.opengl.*;

public final class Spheres {
	static final int SIDES = 20;
	static double[] v1s, v2s;
	static void init() {
		v1s = new double[SIDES+3];
		v2s = new double[SIDES+3];
		for (int i = 0; i < SIDES; i++) {
			double angle = i*Math.PI*2/SIDES;
			v1s[i] = Math.cos(angle);
			v2s[i] = Math.sin(angle);
		}
		for (int i = 0; i < 3; i++) {
			v1s[SIDES+i] = v1s[i];
			v2s[SIDES+i] = v2s[i];
		}
	}

	/**
	 * Draws a sphere centered at (x, y, z) with radius r.
	 * Note that this doesn't do normals right now, so it won't be lit properly.
	 * It's really only for shadow regions.
	 */
	static void drawSphere(GL2 gl, double x, double y, double z, double r) {
		for (int elev = 0; elev < SIDES/2; elev++) {
			double r1 = r * v2s[elev];
			double r2 = r * v2s[elev+1];
			float y1 = (float)(y + r * v1s[elev]);
			float y2 = (float)(y + r * v1s[elev+1]);
			gl.glBegin(gl.GL_TRIANGLE_STRIP);
			for (int a = elev%2; a < SIDES+2+elev%2; a += 2) {
				gl.glVertex3f((float)(x + r1*v1s[ a ]), y1, (float)(z + r1*v2s[ a ]));
				gl.glVertex3f((float)(x + r2*v1s[a+1]), y2, (float)(z + r2*v2s[a+1]));
			}
			gl.glEnd();
		}
	}
}
