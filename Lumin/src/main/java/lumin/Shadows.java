package lumin;

import javax.media.opengl.*;

/**
 * Usage guide in full description.
 * First, call shadowPrep<br/>
 * Then, render the faces of your shadow volume.<br/>
 * Call renderPrep<br/>
 * Re-render your scene with the lights on.<br/>
 * Call cleanup<br/>
 */
public final class Shadows {

	static void shadowPrep(GL2 gl) {
		//This is halfway through the space representable in 8 bits, so we've got a bit of space to move around in.
		gl.glClearStencil(0x80);
		//When rendering the shadow volume, we don't want to affect any buffers that matter.
		gl.glColorMask(false, false, false, false);
		gl.glDepthMask(false);
		//Not sure if we actually have to enable this yet, but it can't hurt.
		//We definitely need it for the re-render, and maybe now.
		gl.glEnable(gl.GL_STENCIL_TEST);
		//Start everything at the halfway point
		gl.glClear(gl.GL_STENCIL_BUFFER_BIT);
		//For each back face that we can see (note that we never disabled the depth test,
			//so it is still calculating which faces we can see),
		//we increment the counter.
		gl.glStencilOpSeparate(gl.GL_BACK, gl.GL_KEEP, gl.GL_DECR_WRAP, gl.GL_KEEP);
		//For each front face we can see, decrement the counter.
		gl.glStencilOpSeparate(gl.GL_FRONT, gl.GL_KEEP, gl.GL_INCR_WRAP, gl.GL_KEEP);
		//If something's in the volume, we'll be able to see more front faces than back faces,
		//and the net change will be negative.
	}

	static void renderPrep(GL2 gl, boolean renderLights) {
		gl.glColorMask(true, true, true, true);
		//By default it's strictly <, which means it won't let us re-draw the same faces under new lighting.
		gl.glDepthFunc(gl.GL_LEQUAL);
		//Stop messing with the stencil buffer
		gl.glStencilOp(gl.GL_KEEP, gl.GL_KEEP, gl.GL_KEEP);
		//renderLights true:
			//Only allow drawing in the areas that did not experience a net negative change (Aren't shadowed)
			//This means 0x80 is LESS or EQUAL to what's in the stencil buffer
		//renderLights false:
			//Want to draw in the shadowed areas (For pulses, e.g.)
			//0x80 must be strictly > stencil buffer, since stencil must be decremented by shadow
		gl.glStencilFunc(renderLights?gl.GL_LEQUAL:gl.GL_GREATER, 0x80, 0xff);
	}

	static void renderPrep(GL2 gl) {
		renderPrep(gl, true);
	}

	static void cleanup(GL2 gl) {
		//Put everything back the way it was
		gl.glDepthMask(true);
		gl.glDisable(gl.GL_STENCIL_TEST);
		gl.glStencilFunc(gl.GL_ALWAYS, 0, 0);
		gl.glDepthFunc(gl.GL_LESS);
	}

	/**
	 * Renders the shadow volume cast by a single triangle.
	 * This triangle should be facing towards the light source.
	 * @param points 9 floats: x, y, z of 1st point, xyz of 2nd pt., xyz of 3rd pt.
	*/
	static void renderShadowTri(GL2 gl, float[] points, float[] lightPos) {
		float[] farPts = new float[9];
		float[] vec = new float[3];
		for (int i = 0; i < 3; i++) {
			float mag2 = 0;
			for (int j = 0; j < 3; j++) {
				vec[j] = points[3*i+j] - lightPos[j];
				mag2 += vec[j] * vec[j];
			}
			float fac = (float)(10/Math.sqrt((double)mag2));
			for (int j = 0; j < 3; j++) {
				farPts[3*i+j] = points[3*i+j] + fac*vec[j];
			}
		}
		gl.glBegin(gl.GL_TRIANGLE_STRIP);
		gl.glVertex3fv(points, 0);
		gl.glVertex3fv(points, 3);
		gl.glVertex3fv(points, 6);
		gl.glVertex3fv(farPts, 3);
		gl.glVertex3fv(farPts, 6);
		gl.glVertex3fv(farPts, 0);
		gl.glVertex3fv(points, 6);
		gl.glVertex3fv(points, 0);
		gl.glEnd();
		gl.glBegin(gl.GL_TRIANGLE_FAN);
		gl.glVertex3fv(points, 0);
		gl.glVertex3fv(farPts, 0);
		gl.glVertex3fv(farPts, 3);
		gl.glVertex3fv(points, 3);
		gl.glEnd();
	}
}
