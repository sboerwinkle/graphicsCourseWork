package lumin;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;
import javax.media.opengl.*;
import javax.media.opengl.awt.GLJPanel;
import javax.media.opengl.glu.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.gl2.GLUT;

public final class App
	implements GLEventListener
{
	public static final GLU		GLU = new GLU();
	public static final GLUT	GLUT = new GLUT();
	public static final Random	RANDOM = new Random();

	// State (internal) variables
	private int			k = 0;			// Just an animation counter
	private InputHandler		input;
	private long			lastTime;		//For determining elapsed time
	private GLJPanel		panel;

	//Player vars
	private double			x, y, z;		// Position

	//Position of the light source
	private float[]			lightPos = {0, 0, -1, 1};

	private float[]			zeroPt = {0, 0, 0, 1};

	private Item[]			items;
	private int			selectedItem; // If no item is selected, this is items.length

	ArrayList<Pulse> pulses;

	//**********************************************************************
	// App
	//**********************************************************************

	public static void main(String[] args)
	{
		GLProfile		profile = GLProfile.getDefault();
		GLCapabilities	capabilities = new GLCapabilities(profile);
		capabilities.setStencilBits(8);
		GLJPanel		panel = new GLJPanel(capabilities);
		JFrame			frame = new JFrame("Example");

		Sphere.init(); // Do all the trig ahead of time

		frame.setContentPane(panel);
		panel.setPreferredSize(new Dimension(600, 600));
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setVisible(true);

		frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					System.exit(0);
				}
			});

		panel.addGLEventListener(new App(panel));

		FPSAnimator animator = new FPSAnimator(panel, 60);

		animator.start();
	}

	public App(GLJPanel p) {
		lastTime = System.nanoTime();

		panel = p;
		input = new InputHandler();
		input.frameX = p.getLocationOnScreen().x;
		input.frameY = p.getLocationOnScreen().y;
		p.addKeyListener(input);
		p.addMouseListener(input);
		p.addMouseWheelListener(input);
		p.addMouseMotionListener(input);

		pulses = new ArrayList<Pulse>();

		items = new Item[3];
		items[0] = new Cube(0, 0, 4.5, 1, new Color[] {Color.red, Color.green, Color.blue});
		items[1] = new Cube(0, 0, 4.5, 5, new Color[] {Color.white, Color.white, Color.white});
		items[2] = new Sphere(2.5, 0, 4.5, 1, Color.green);
		selectedItem = items.length;
	}

	//**********************************************************************
	// Override Methods (GLEventListener)
	//**********************************************************************

	public void		init(GLAutoDrawable drawable)
	{
		input.w = drawable.getWidth();
		input.h = drawable.getHeight();
		GL2 gl = drawable.getGL().getGL2();
		gl.glEnable(gl.GL_DEPTH_TEST);
		gl.glEnable(gl.GL_COLOR_MATERIAL);

		//do some lighting and fog. I actually have very little understanding about what is happening here
        float local_view[] = { 0.0f };
        //gl.glLightModelfv(GL2.GL_LIGHT_MODEL_LOCAL_VIEWER, local_view, 0);

        gl.glFrontFace(GL.GL_CW);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);
        gl.glEnable(GL2.GL_AUTO_NORMAL);
        gl.glEnable(GL2.GL_NORMALIZE);

        /*gl.glEnable(GL2.GL_FOG);
        {
            float fogColor[] = { 0.5f, 0.5f, 0.5f, .8f };

            gl.glFogi(GL2.GL_FOG_MODE, GL2.GL_EXP);
            gl.glFogfv(GL2.GL_FOG_COLOR, fogColor, 0);
            gl.glFogf(GL2.GL_FOG_DENSITY, 0.35f);
            gl.glHint(GL2.GL_FOG_HINT, GL.GL_DONT_CARE);

            gl.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        }*/

		//We're affecting the projection matrix
		//(The one that turns world coordinates into screen coordinates)
		gl.glMatrixMode(gl.GL_PROJECTION);
		//Load identity, because...
		gl.glLoadIdentity();
		//This actully multiplies the existing matrix by something.
		//45 degree FOV, aspect ratio 1,
		//Nearest visible depth is 0.05, farthest visible depth is 10.
		//Note that you get slightly better occlusion accuracy (I'm pretty sure)
		//The lower the ratio zFar/zNear is. Also zNear can't be 0.
		GLU.gluPerspective(90, 1, 0.1, 40);

		System.out.println(gl.glGetString(GL2.GL_SHADING_LANGUAGE_VERSION));
	}

	public void		dispose(GLAutoDrawable drawable)
	{
	}

	public void		display(GLAutoDrawable drawable)
	{
		long time = System.nanoTime();
		tick((int)(time-lastTime));
		lastTime = time;

		GL2		gl = drawable.getGL().getGL2();

		gl.glMatrixMode(gl.GL_MODELVIEW);
		gl.glLoadIdentity();
		//Set up the flashlight here, since we don't want it transformed by the modelview matrix.
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, zeroPt, 0);
		gl.glLightf(GL2.GL_LIGHT0, GL2.GL_CONSTANT_ATTENUATION, 0);
		gl.glLightf(GL2.GL_LIGHT0, GL2.GL_QUADRATIC_ATTENUATION, 1);
		//Make the flashlight a spot light
		gl.glLightf(GL2.GL_LIGHT0, GL2.GL_SPOT_EXPONENT, 3);
		gl.glLightf(GL2.GL_LIGHT0, GL2.GL_SPOT_CUTOFF, 90);
		//Turn off glare on the flashlight, it's distracting
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, zeroPt, 0);

		double cosP = Math.cos(input.pitch);
		double sinP = Math.sin(input.pitch);
		double cosY = Math.cos(input.yaw);
		double sinY = Math.sin(input.yaw);
		GLU.gluLookAt(x, y, z, x + cosP*sinY, y + sinP, z + cosP*cosY, -sinP*sinY, cosP, -sinP*cosY);

		gl.glClear(GL.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
		//gl.glEnable(gl.GL_CULL_FACE);
		//gl.glCullFace(gl.GL_BACK);
		drawSomething(gl);
		//gl.glDisable(gl.GL_CULL_FACE);
	}

	public void		reshape(GLAutoDrawable drawable, int x, int y, int w, int h)
	{
		input.frameX = panel.getLocationOnScreen().x;
		input.frameY = panel.getLocationOnScreen().y;
		input.w = w;
		input.h = h;
	}

	//**********************************************************************
	// Private Methods (Scene)
	//**********************************************************************

	// This page is helpful (scroll down to "Drawing Lines and Polygons"):
	// http://www.linuxfocus.org/English/January1998/article17.html
	private void	drawSomething(GL2 gl)
	{
		//Set the specular reflectiveness of the materials
		float mat[] = new float[4];
		mat[0] = 0.626959f;
		mat[1] = 0.626959f;
		mat[2] = 0.626959f;
		mat[3] = 1.0f;
		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_SPECULAR, mat, 0);
		gl.glMaterialf(GL.GL_FRONT, GL2.GL_SHININESS, 0.6f * 128.0f);

		//Scene render, lights off ================================
		gl.glBegin(GL.GL_TRIANGLES);
		//Note that our perspective is looking down the negative z axis by default.

		//White triangle up
		gl.glNormal3f(0, 0, -1);
		gl.glColor3f(1.0f, 1.0f, 1.0f);
		gl.glVertex3d(1, 0, 3);
		gl.glVertex3d(-1, 0, 3);
		gl.glVertex3d(0, 1, 3);
		//Reddish triangle down
		gl.glColor3f(1.0f, 0, 0);
		gl.glVertex3d(1, 0, 3);
		gl.glVertex3d(-1, 0, 3);
		gl.glVertex3d(0, -1, 3);
		//gl.glPopMatrix();

		gl.glEnd();

		for (Item i : items) i.render(gl);

		//Render Light 0 ================================
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos, 0);
		/*//Things are simpler to make pretty if this light doesn't attenuate with distance
		gl.glLightf(GL2.GL_LIGHT0, GL2.GL_QUADRATIC_ATTENUATION, 0);
		gl.glLightf(GL2.GL_LIGHT0, GL2.GL_CONSTANT_ATTENUATION, 1);*/
		//Point lights are not spot lights
		gl.glLightf(GL2.GL_LIGHT0, GL2.GL_SPOT_CUTOFF, 180);
		//Point lights have regular glare
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, new float[] {1, 1, 1, 1}, 0);

		Shadows.shadowPrep(gl);
		
		for (Item i : items) i.renderShadow(gl, lightPos);
		//gl.glColor3f(0, 1, 0);
		//Shadows.renderShadowTri(gl, new float[] {1,1,3.5f,-1,1,3.5f,-1,-1,3.5f}, lightPos);
		//items[0].renderShadow(gl, lightPos);

		Shadows.renderPrep(gl);

		gl.glLightModelfv(gl.GL_LIGHT_MODEL_AMBIENT, zeroPt, 0);
		gl.glEnable(gl.GL_BLEND);
		gl.glBlendFunc(gl.GL_ONE, gl.GL_ONE);
		for (Item i : items) i.render(gl);
		gl.glBlendFunc(gl.GL_ONE, gl.GL_ZERO);
		gl.glDisable(gl.GL_BLEND);
		gl.glLightModelfv(gl.GL_LIGHT_MODEL_AMBIENT, new float[] {0.2f, 0.2f, 0.2f, 1}, 0);

		Shadows.cleanup(gl);

		//PULSE RENDER ================================
		Shadows.shadowPrep(gl);
		for (Pulse p : pulses) p.render(gl);
		Shadows.renderPrep(gl, false);
		gl.glColor4f(.5f, 1, .5f, 1);
		fillScreen(gl);
		Shadows.cleanup(gl);

		//SELECTION RENDER ================================
		if (selectedItem < items.length) {
			gl.glDisable(gl.GL_DEPTH_TEST);
			gl.glDisable(gl.GL_LIGHTING);
			gl.glPointSize(5);
			gl.glBegin(gl.GL_POINTS);
			gl.glColor3f(1, (float)Math.abs(Math.cos(input.pitch)), (float)Math.abs(Math.sin(input.yaw)));
			gl.glVertex3fv(items[selectedItem].pos, 0);
			gl.glEnd();
			gl.glEnable(gl.GL_LIGHTING);
			gl.glEnable(gl.GL_DEPTH_TEST);
		}
	}

	private void fillScreen(GL2 gl) {
		gl.glEnable(gl.GL_BLEND);
		gl.glDisable(gl.GL_LIGHTING);
		//We want to cover the whole screen, so depth test is balogna
		gl.glDisable(gl.GL_DEPTH_TEST);
		//Additionally this "flying around space" thing is garbage, always paint it in front of me
		gl.glMatrixMode(gl.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		//To get the result color, take 0*(color we're painting) + (alpha specified above)*(color already there)
		gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		gl.glVertex3d(1, 1, -1);
		gl.glVertex3d(1, -1, -1);
		gl.glVertex3d(-1, 1, -1);
		gl.glVertex3d(-1, -1, -1);
		gl.glEnd();

		//Undo my changes
		gl.glBlendFunc(gl.GL_ONE, gl.GL_ZERO);
		gl.glPopMatrix();
		gl.glEnable(gl.GL_DEPTH_TEST);
		gl.glEnable(gl.GL_LIGHTING);
		gl.glDisable(gl.GL_BLEND);
	}

	//Does the actions that need to happen once per tick
	private void tick(int nanos) {
		double mvmnt = nanos/(double)1e9;
		double cosY = mvmnt*Math.cos(input.yaw);
		double sinY = mvmnt*Math.sin(input.yaw);
		z += cosY*input.getVec(0)-sinY*input.getVec(1);
		x += cosY*input.getVec(1)+sinY*input.getVec(0);
		y += mvmnt * input.getVec(2);

		for (Pulse p : pulses) p.size += mvmnt;
		while (!pulses.isEmpty() && pulses.get(0).size >= 20) pulses.remove(0);

		if (input.actions[0]) {
			input.actions[0] = false;
			pulses.add(new Pulse(x, y, z));
		}
		if (input.actions[1]) {
			input.actions[1] = false;
			lightPos[0] = (float)x;
			lightPos[1] = (float)y;
			lightPos[2] = (float)z;
		}

		selectedItem = (selectedItem + input.cumulativeMouseTicks)%(items.length+1);
		if (selectedItem < 0) selectedItem += items.length + 1;
		input.cumulativeMouseTicks = 0;

		if (input.mouseDown && selectedItem < items.length) {
			items[selectedItem].pos[0] = (float)(x + Math.sin(input.yaw)*Math.cos(input.pitch)*3);
			items[selectedItem].pos[1] = (float)(y + Math.sin(input.pitch)*3);
			items[selectedItem].pos[2] = (float)(z + Math.cos(input.yaw)*Math.cos(input.pitch)*3);
		}
	}

	// This example on this page is long but helpful:
	// http://jogamp.org/jogl-demos/src/demos/j2d/FlyingText.java
	/*private void	drawSomeText(GLAutoDrawable drawable)
	{
		renderer.beginRendering(drawable.getWidth(), drawable.getHeight());
		renderer.setColor(1.0f, 1.0f, 0, 1.0f);
		renderer.draw("This is a point", w/2 + 8, h/2 - 5);
		renderer.endRendering();
	}*/
}
