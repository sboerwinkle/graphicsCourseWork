package lumin;

import javax.media.opengl.*;
import com.jogamp.opengl.util.*;
import java.awt.Color;
import java.util.ArrayList;

public class Haze{
	ArrayList<Item> shells;
	int layers;

	public Haze(double[] center, double r, Color c, double density){
		layers = (int)(r*density*1000);	
		shells = new ArrayList<Item>(layers);

		for (int i = 1; i <= layers; i++)
			shells.add(new Sphere(center[0], center[1], center[2], r/layers*i, c));
	}

	public void render(GL2 gl){
		gl.glDisable(gl.GL_DEPTH_TEST);
		gl.glEnable (gl.GL_BLEND);
		gl.glBlendEquationSeparate(gl.GL_FUNC_ADD, gl.GL_FUNC_ADD);
		gl.glBlendFuncSeparate(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA, gl.GL_ONE, gl.GL_ONE_MINUS_SRC_ALPHA);
		float thickness = .5f;

		for (Item shell : shells){ 
			gl.glFogCoordf(thickness);
			shell.render(gl);
			thickness += 1f/4/layers;
		}

		gl.glFogCoordf(0f);
		gl.glDisable(gl.GL_BLEND);
		gl.glEnable(gl.GL_DEPTH_TEST);
	}
}