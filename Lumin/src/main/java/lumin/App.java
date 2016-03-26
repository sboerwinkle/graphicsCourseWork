package lumin;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
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

	//Shadow box vars (A temporary thing, for testing the shadow system)
	private double			sx, sy, sz;

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

		frame.setContentPane(panel);
		panel.setPreferredSize(new Dimension(600, 600));
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

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
		p.addMouseMotionListener(input);

		sx = 0.1;
		sy = 0.1;
		sz = 4.6;
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
		GLU.gluPerspective(90, 1, 0.1, 20);

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
		double cosP = Math.cos(input.pitch);
		double sinP = Math.sin(input.pitch);
		double cosY = Math.cos(input.yaw);
		double sinY = Math.sin(input.yaw);
		GLU.gluLookAt(x, y, z, x + cosP*sinY, y + sinP, z + cosP*cosY, -sinP*sinY, cosP, -sinP*cosY);

		gl.glClear(GL.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
		drawSomething(gl);
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
		gl.glBegin(GL.GL_TRIANGLES);
		//Note that our perspective is looking down the negative z axis by default.

		//White triangle up
		gl.glColor3f(1.0f, 1.0f, 1.0f);
		gl.glVertex3d(1, 0, 3);
		gl.glVertex3d(-1, 0, 3);
		gl.glVertex3d(0, 1, 3);
		//Red triangle down
		gl.glColor3f(1.0f, 0, 0);
		gl.glVertex3d(1, 0, 3);
		gl.glVertex3d(-1, 0, 3);
		gl.glVertex3d(0, -1, 3);

		gl.glEnd();
		drawCube(gl, 0, 0, 4.5, 1, new Color[] {Color.red, Color.green, Color.blue});

		Shadows.shadowPrep(gl);

		//We're going to shadow everything inside the ominously-named shadow cube
		drawCube(gl, sx, sy, sz, 1, new Color[] {new Color(0xdeadBeef)});

		Shadows.renderPrep(gl);

		letDarknessCoverTheWorld(gl);

		Shadows.cleanup(gl);
	}

	private void letDarknessCoverTheWorld(GL2 gl) {
		gl.glEnable(gl.GL_BLEND);
		//We want to cover the whole screen, so depth test is balogna
		gl.glDisable(gl.GL_DEPTH_TEST);
		//Additionally this "flying around space" thing is garbage, always paint it in front of me
		gl.glMatrixMode(gl.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		gl.glBlendColor(0, 0, 0, 0.5f);
		//To get the result color, take 0*(color we're painting) + (alpha specified above)*(color already there)
		gl.glBlendFunc(gl.GL_ZERO, gl.GL_CONSTANT_ALPHA);
		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		gl.glVertex3d(1, 1, -1);
		gl.glVertex3d(1, -1, -1);
		gl.glVertex3d(-1, 1, -1);
		gl.glVertex3d(-1, -1, -1);
		gl.glEnd();

		//Undo my changes
		gl.glPopMatrix();
		gl.glEnable(gl.GL_DEPTH_TEST);
		gl.glDisable(gl.GL_BLEND);
	}

	private void drawCube(GL2 gl, double double_x, double double_y, double double_z, double double_r, Color[] colors) {
		float x = (float)double_x;
		float y = (float)double_y;
		float z = (float)double_z;
		float r = (float)double_r;
		float[] data = new float[3];
		int nc = colors.length; // nc => numColors
		//dys uses r, while dxs uses 1.
		//This is because dxs must be multiplied by dz, which gives an r factor.
		//This, in turn, is because the order of iteration must be flipped
			//for far faces
		float[] dxs = {-1, 1, 1, -1};
		float[] dys = {-r, -r, r, r};
		int[] corners = {0, 1, 2, 2, 3, 0}; // To draw a square using a pair of triangles, visit corners in this order
		gl.glBegin(gl.GL_TRIANGLES);
		for (int dim = 0; dim < 3; dim++) {
			for (float dz : new float[] {-r, r}) {
				Color col = colors[((dz>0?3:0)+dim)%nc];
				for (int i = 0; i < 6; i++) {
					int c = corners[i];
					data[0] = x;
					data[1] = y;
					data[2] = z;
					data[dim] += dz;
					data[(dim+1)%3] += dz*dxs[c];
					data[(dim+2)%3] += dys[c];
					gl.glColor3f(col.getRed()/255f, col.getGreen()/255f, col.getBlue()/255f);
					gl.glVertex3f(data[0], data[1], data[2]);
				}
			}
		}
		gl.glEnd();
	}

	//Does the actions that need to happen once per tick
	private void tick(int nanos) {
		double mvmnt = nanos/(double)1e9;
		double cosY = mvmnt*Math.cos(input.yaw);
		double sinY = mvmnt*Math.sin(input.yaw);
		z += cosY*input.getVec(0)-sinY*input.getVec(1);
		x += cosY*input.getVec(1)+sinY*input.getVec(0);
		y += mvmnt * input.getVec(2);

		sx += mvmnt*input.getVec(3);
		sy += mvmnt*input.getVec(4);
		sz += mvmnt*input.getVec(5);
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
