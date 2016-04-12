package lumin;

import java.awt.Color;
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
	float size;
	Sphere me;

	Pulse(double X, double Y, double Z) {
		me = new Sphere(X, Y, Z, 0, Color.black);
		size = 0.5f;
	}

	void render(GL2 gl) {
		me.r = size+0.5f;
		me.render(gl);
		me.r = -size;
		me.render(gl);
	}
}
