package lumin;

import javax.media.opengl.*;
import java.awt.Color;

public final class Sphere extends Item {
	static final int SIDES = 40;
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

	float r;
	float[] color;
	Sphere(double X, double Y, double Z, double R, Color C) {
		super(X, Y, Z);
		r = (float) R;
		color = new float[] {C.getRed()/255f, C.getGreen()/255f, C.getBlue()/255f, C.getAlpha()/255f};
	}
	/**
	 * Draws a sphere centered at (x, y, z) with radius r.
	 */
	public void render(GL2 gl) {
		gl.glColor4fv(color, 0);
		for (int elev = 0; elev < SIDES/2; elev++) {
			double r1 = r * v2s[elev];
			double r2 = r * v2s[elev+1];
			float y1 = (float)(pos[1] + r * v1s[elev]);
			float y2 = (float)(pos[1] + r * v1s[elev+1]);
			gl.glBegin(gl.GL_TRIANGLE_STRIP);
			for (int a = elev%2; a < SIDES+2+elev%2; a += 2) {
				gl.glNormal3f((float)(v2s[elev]*v1s[ a ]), (float)(v1s[elev]), (float)(v2s[elev]*v2s[ a ]));
				gl.glVertex3f((float)(pos[0] + r1*v1s[ a ]), y1, (float)(pos[2] + r1*v2s[ a ]));
				gl.glNormal3f((float)(v2s[elev+1]*v1s[a+1]), (float)(v1s[elev+1]), (float)(v2s[elev+1]*v2s[a+1]));
				gl.glVertex3f((float)(pos[0] + r2*v1s[a+1]), y2, (float)(pos[2] + r2*v2s[a+1]));
			}
			gl.glEnd();
		}
	}

	public void renderShadow(GL2 gl, float[] lightPos) {
		//In this situation, the edge of the shadow is defined by a ring on the sphere.
		//This ring needs a center point and two vectors defining its axes.
		float[] vec = new float[3];
		for (int i = 0; i < 3; i++) vec[i] = pos[i] - lightPos[i];
		float dist2 = vec[0]*vec[0] + vec[1]*vec[1] + vec[2]*vec[2];
		float ratio = r*r/dist2;
		//The center of the ring
		float[] c1 = new float[3];
		for (int i = 0; i < 3; i++) c1[i] = pos[i] - ratio*vec[i];
		//len of ratio*vec is r*r/d
		//len of other vec must be sqrt(r*r - r*r*r*r/dist2) = r*sqrt(1-ratio)

		int perpIx = vec[0]*vec[0]/dist2 > 0.5 ? 1 : 0;
		//jHat is the first axis vector. It needs to be perpendicular to dist, but that's the only requirement.
		float[] jHat = new float[3];
		jHat[perpIx] = 1;
		float ratio2 = -vec[perpIx]/dist2;
		float d2jHat = 0;
		for (int i = 0; i < 3; i++) {
			jHat[i] += ratio2 * vec[i];
			d2jHat += jHat[i] * jHat[i];
		}
		//len of this vec must be r*sqrt(1-ratio), but at present it is sqrt(d2jHat)
		ratio2 = r*(float)Math.sqrt((1-ratio)/d2jHat);
		for (int i = 0; i < 3; i++) jHat[i] *= ratio2;
		//If we do the cross product now, the answer will be too big by a factor of dist.
		float dist = (float)Math.sqrt(dist2);
		//kHat is the other axis vector, computed via cross product.
		float[] kHat = new float[3];
		for (int i = 0; i < 3; i++) {
			kHat[i] = (jHat[(i+2)%3]*vec[(i+1)%3] - jHat[(i+1)%3]*vec[(i+2)%3])/dist;
		}
		//This isn't correct, but I'll fix it later! Or not!
		ratio2 = 10/dist;
		float[] c2 = new float[3];
		for (int i = 0; i < 3; i++) {
			c2[i] = lightPos[i] + vec[i]*ratio2*(1-ratio);
		}
		//Draw the front of the truncated cone
		gl.glBegin(gl.GL_TRIANGLE_FAN);
		for (int i = 0; i < SIDES; i += 2) {
			for (int j = 0; j < 3; j++) {
				vec[j] = c1[j] + (float)(v1s[i]*jHat[j] + v2s[i]*kHat[j]);
			}
			gl.glVertex3fv(vec, 0);
		}
		gl.glEnd();
		//Draw the back of the truncated cone
		gl.glBegin(gl.GL_TRIANGLE_FAN);
		for (int i = SIDES-1; i > 0; i -= 2) {
			for (int j = 0; j < 3; j++) {
				vec[j] = c2[j] + (float)(v1s[i]*jHat[j] + v2s[i]*kHat[j])*ratio2;
			}
			gl.glVertex3fv(vec, 0);
		}
		gl.glEnd();
		//Draw the edge of the cone
		gl.glBegin(gl.GL_TRIANGLE_STRIP);
		for (int i = 0; i < SIDES+2; i += 2) {
			for (int j = 0; j < 3; j++) vec[j] = c1[j] + (float)(v1s[i]*jHat[j] + v2s[i]*kHat[j]);
			gl.glVertex3fv(vec, 0);
			for (int j = 0; j < 3; j++) vec[j] = c2[j] + (float)(v1s[i+1]*jHat[j] + v2s[i+1]*kHat[j])*ratio2;
			gl.glVertex3fv(vec, 0);
		}
		gl.glEnd();
	}
}
