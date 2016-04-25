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
	private double			phase = 0;		// Just an animation counter
	private InputHandler		input;
	private long			lastTime;		//For determining elapsed time
	private GLJPanel		panel;

	//Player vars
	private double			x, y, z;		// Position

	//A useful vector to have lying around
	private float[]			zeroPt = {0, 0, 0, 1};

	private ArrayList<Item>		items;
	private ArrayList<Light>	lights;
	private int			selectedThing; // If no item is selected, this is 0
	private ArrayList<Haze>	hazes;

	private float[]			flashlightColor = {1f, 1f, 1f, 1f};

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

		items = new ArrayList<Item>();
		
		items.add(new Cube(0, 0, 4.5, 1, new Color[] {Color.red, Color.green, Color.blue}));
		items.add(new Sphere(2.5, 0, 4.5, 1, Color.green));
		lights = new ArrayList<Light>(); // Aah let people add lights ore somthing
		lights.add(new Light(0, 0, 0));
		lights.add(new Light(0, 0, 6));
		items.add(new Cube(0, 0, 4.5, 10, new Color[] {new Color(.66f, .13f, .9f, .8f)}));	//purplish


		hazes = new ArrayList<Haze>();	
		hazes.add(new Haze(new double[]{3, 0, 1}, 2, new Color(.75f, .75f, .75f, .0065f), .080));
		selectedThing = 0;
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
		gl.glEnable (GL2.GL_BLEND);
		gl.glBlendFunc (GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

		gl.glEnable(GL2.GL_FOG);
		{
		    float fogColor[] = { 0.65f, 0.65f, 0.65f, .01f };
		    //float fogColor[] = { 0.5f, 0.5f, 0.5f, .025f };		//this one is pretty good as well

		    gl.glFogi(GL2.GL_FOG_MODE, GL2.GL_LINEAR);
		    gl.glFogfv(GL2.GL_FOG_COLOR, fogColor, 0);
		    gl.glFogf(GL2.GL_FOG_DENSITY, 0.8f);
		    gl.glHint(GL2.GL_FOG_HINT, GL.GL_NICEST);
		    gl.glFogi(GL2.GL_FOG_COORD_SRC, GL2.GL_FOG_COORD);

		    gl.glClearColor(0f, 0f, 0f, 1f);
		}

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

		gl.glFogCoordf(0f);			//closer to 1 this is set, the more foggy what is drawn will appear
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
		gl.glLightf(GL2.GL_LIGHT0, GL2.GL_QUADRATIC_ATTENUATION, .5F);
		//Make the flashlight a spot light
		gl.glLightf(GL2.GL_LIGHT0, GL2.GL_SPOT_EXPONENT, 2);
		gl.glLightf(GL2.GL_LIGHT0, GL2.GL_SPOT_CUTOFF, 60);
		//Turn off glare on the flashlight, it's distracting
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, new float[] {1f, 1f, 1f, .5f}, 0);
		//Set the color...
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, flashlightColor, 0);

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


		gl.glFogCoordf(0f);
		gl.glEnable (GL2.GL_BLEND);
		gl.glBlendFunc (GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		//Enable default ambient lighting (plus the flashlight) for the first, unoccluded render.
		gl.glLightModelfv(gl.GL_LIGHT_MODEL_AMBIENT, new float[] {0.3f, 0.3f, 0.3f, .25f}, 0);
		gl.glEnable(gl.GL_DEPTH_TEST);
		for (Item i : items) i.render(gl);

		gl.glBegin(GL.GL_TRIANGLES);
		//Note that our perspective is looking down the negative z axis by default.

		//gl.glFogCoordf(1f);
		//White triangle up
		gl.glNormal3f(0, 0, -1);
		gl.glColor4f(1.0f, 1.0f, 1.0f, .75f);
		gl.glVertex3d(1, 0, 3);
		gl.glVertex3d(-1, 0, 3);
		gl.glVertex3d(0, 1, 3);
		//Reddish triangle down
		gl.glColor4f(1.0f, 0f, 0f, .75f);
		gl.glVertex3d(1, 0, 3);
		gl.glVertex3d(-1, 0, 3);
		gl.glVertex3d(0, -1, 3);
		//gl.glPopMatrix();

		gl.glEnd();

		for (Haze i : hazes){
			i.render(gl);
		}
		gl.glLightModelfv(gl.GL_LIGHT_MODEL_AMBIENT, zeroPt, 0);

		


		//draw transparent things last
		

		gl.glFogCoordf(0f);
		//Render lighting from point light s================================
		gl.glEnable(gl.GL_BLEND);
		gl.glBlendFunc(gl.GL_ONE, gl.GL_ONE); // Just add colors

		/*
		//Things are simpler to make pretty if this light doesn't attenuate with distance
		gl.glLightf(GL2.GL_LIGHT0, GL2.GL_QUADRATIC_ATTENUATION, 0);
		gl.glLightf(GL2.GL_LIGHT0, GL2.GL_CONSTANT_ATTENUATION, 1);
		*/

		//Point lights are not spot lights
		gl.glLightf(GL2.GL_LIGHT0, GL2.GL_SPOT_CUTOFF, 180);

		for (Light l : lights) l.render(gl, items);

		//PULSE RENDER ================================
		gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
		Shadows.shadowPrep(gl);
		for (Pulse p : pulses) p.render(gl);
		Shadows.renderPrep(gl, false);
		gl.glColor4f(.5f, 1, .5f, 1);
		fillScreen(gl);
		Shadows.cleanup(gl);

		//SELECTION RENDER ================================
		if (selectedThing != 0) {
			gl.glDisable(gl.GL_DEPTH_TEST);
			gl.glDisable(gl.GL_LIGHTING);
			gl.glPointSize(5);
			gl.glBegin(gl.GL_POINTS);
			float value = (float)Math.abs(Math.cos(phase));
			gl.glColor3f(value, value, value);
			if (selectedThing < items.size()) {
				gl.glVertex3fv(items.get(selectedThing).pos, 0);
			} else {
				gl.glVertex3fv(lights.get(selectedThing-items.size()).pos, 0);
			}
			gl.glEnd();
			gl.glEnable(gl.GL_LIGHTING);
			gl.glEnable(gl.GL_DEPTH_TEST);
		}
		gl.glBlendFunc(gl.GL_ONE, gl.GL_ZERO);
		gl.glDisable(gl.GL_BLEND);

		
	}

	private void fillScreen(GL2 gl) {
		gl.glDisable(gl.GL_LIGHTING);
		//We want to cover the whole screen, so depth test is balogna
		gl.glDisable(gl.GL_DEPTH_TEST);
		//Additionally this "flying around space" thing is garbage, always paint it in front of me
		gl.glMatrixMode(gl.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		gl.glVertex3d(1, 1, -1);
		gl.glVertex3d(1, -1, -1);
		gl.glVertex3d(-1, 1, -1);
		gl.glVertex3d(-1, -1, -1);
		gl.glEnd();

		//Undo my changes
		gl.glPopMatrix();
		gl.glEnable(gl.GL_DEPTH_TEST);
		gl.glEnable(gl.GL_LIGHTING);
	}

	//Does the actions that need to happen once per tick
	private void tick(int nanos) {
		double mvmnt = nanos/(double)1e9;
		phase += 3*mvmnt;
		if (phase > Math.PI*2) phase -= Math.PI*2;
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

		//Set the color of the selected light, or our flashlight.
		float[] lightColor;
		if (selectedThing < items.size()) lightColor = flashlightColor;
		else lightColor = lights.get(selectedThing-items.size()).color;
		for (int i = 0; i < 3; i++) {
			int v = input.getVec(3+i);
			if (v == 0) continue;
			lightColor[i] = (v+1)/2;
		}

		//Switch the selected thing
		selectedThing = (selectedThing + input.cumulativeMouseTicks)%(items.size()+lights.size());
		if (selectedThing < 0) selectedThing += items.size()+lights.size();
		input.cumulativeMouseTicks = 0;

		//Move the selected thing to be in front of us
		if (input.mouseDown && selectedThing != 0) {
			if (selectedThing < items.size()) {
				Item i = items.get(selectedThing);
				i.pos[0] = (float)(x + Math.sin(input.yaw)*Math.cos(input.pitch)*3);
				i.pos[1] = (float)(y + Math.sin(input.pitch)*3);
				i.pos[2] = (float)(z + Math.cos(input.yaw)*Math.cos(input.pitch)*3);
			} else {
				Light l = lights.get(selectedThing - items.size());
				l.pos[0] = (float)(x + Math.sin(input.yaw)*Math.cos(input.pitch)*3);
				l.pos[1] = (float)(y + Math.sin(input.pitch)*3);
				l.pos[2] = (float)(z + Math.cos(input.yaw)*Math.cos(input.pitch)*3);
			}
		}
	}

	private void renderHallway(GL2 gl){
		gl.glColor3f(1f, 1f, 1f);


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
