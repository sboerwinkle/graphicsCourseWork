
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

public final class Main
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

	//**********************************************************************
	// Main
	//**********************************************************************

	public static void main(String[] args)
	{
		GLProfile		profile = GLProfile.getDefault();
		GLCapabilities	capabilities = new GLCapabilities(profile);
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

		panel.addGLEventListener(new Main(panel));

		FPSAnimator animator = new FPSAnimator(panel, 60);

		animator.start();
	}

	public Main(GLJPanel p) {
		lastTime = System.nanoTime();

		panel = p;
		input = new InputHandler();
		input.frameX = p.getLocationOnScreen().x;
		input.frameY = p.getLocationOnScreen().y;
		p.addKeyListener(input);
		p.addMouseMotionListener(input);
	}

	//**********************************************************************
	// Override Methods (GLEventListener)
	//**********************************************************************

	public void		init(GLAutoDrawable drawable)
	{
		input.w = drawable.getWidth();
		input.h = drawable.getHeight();
		GL2 gl = drawable.getGL().getGL2();
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
		GLU.gluPerspective(45, 1, 0.1, 20);
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

		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
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
	}

	//Does the actions that need to happen once per tick
	private void tick(int nanos) {
		double mvmnt = nanos/(double)1e9;
		double cosY = mvmnt*Math.cos(input.yaw);
		double sinY = mvmnt*Math.sin(input.yaw);
		z += cosY*input.getVec(0)-sinY*input.getVec(1);
		x += cosY*input.getVec(1)+sinY*input.getVec(0);
		y += mvmnt * input.getVec(2);
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
