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
		gl.glStencilOpSeparate(gl.GL_BACK, gl.GL_KEEP, gl.GL_KEEP, gl.GL_INCR_WRAP);
		//For each front face we can see, decrement the counter.
		gl.glStencilOpSeparate(gl.GL_FRONT, gl.GL_KEEP, gl.GL_KEEP, gl.GL_DECR_WRAP);
		//If something's in the volume, we'll be able to see more front faces than back faces,
		//and the net change will be negative.
	}
	static void renderPrep(GL2 gl) {
		gl.glColorMask(true, true, true, true);
		//By default it's strictly <, which means it won't let us re-draw the same faces under new lighting.
		gl.glDepthFunc(gl.GL_LEQUAL);
		//Stop messing with the stencil buffer
		gl.glStencilOp(gl.GL_KEEP, gl.GL_KEEP, gl.GL_KEEP);
		//Only allow drawing in the areas that experienced a net negative change.
		gl.glStencilFunc(gl.GL_LESS, 0x80, 0xff);
	}
	static void cleanup(GL2 gl) {
		//Put everything back the way it was
		gl.glDepthMask(true);
		gl.glDisable(gl.GL_STENCIL_TEST);
		gl.glStencilFunc(gl.GL_ALWAYS, 0, 0);
		gl.glDepthFunc(gl.GL_LESS);
	}
}
