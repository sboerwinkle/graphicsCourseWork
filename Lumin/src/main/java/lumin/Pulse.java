package lumin;

import java.awt.Color;
import javax.media.opengl.*;

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
