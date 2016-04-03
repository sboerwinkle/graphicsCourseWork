package lumin;

import javax.media.opengl.*;

/**
 * Usage guide in full description.
 * First, call shadowPrep<br/>
 * Then, render the faces of your shadow volume.<br/>
 * Call renderPrep<br/>
 * Re-render your scene with the lights off.<br/>
 * Call cleanup<br/>
 */
public class Pulse {
	double x, y, z;
	double size;

	Pulse(double X, double Y, double Z) {
		x = X;
		y = Y;
		z = Z;
		size = 0.5;
	}

	void render(GL2 gl) {
		Spheres.drawSphere(gl, x, y, z, size+0.5);
		Spheres.drawSphere(gl, x, y, z, -size);
	}
}
