
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
	private int				k = 0;		// Just an animation counter

	private int				w;			// Canvas width
	private int				h;			// Canvas height

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

		panel.addGLEventListener(new Main());

		FPSAnimator		animator = new FPSAnimator(panel, 60);

		animator.start();
	}

	//**********************************************************************
	// Override Methods (GLEventListener)
	//**********************************************************************

	public void		init(GLAutoDrawable drawable)
	{
		w = drawable.getWidth();
		h = drawable.getHeight();
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
		GLU.gluPerspective(45, 1, 0.05, 10);
	}

	public void		dispose(GLAutoDrawable drawable)
	{
	}

	public void		display(GLAutoDrawable drawable)
	{
		GL2		gl = drawable.getGL().getGL2();

		gl.glClear(GL.GL_COLOR_BUFFER_BIT);		// Clear the buffer
		drawSomething(gl);						// Draw something
	}

	public void		reshape(GLAutoDrawable drawable, int x, int y, int w, int h)
	{
		this.w = w;
		this.h = h;
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
		gl.glVertex3d(1, 0, -3);
		gl.glVertex3d(-1, 0, -3);
		gl.glVertex3d(0, 1, -3);
		//Red triangle down
		gl.glColor3f(1.0f, 0, 0);
		gl.glVertex3d(1, 0, -3);
		gl.glVertex3d(-1, 0, -3);
		gl.glVertex3d(0, -1, -3);

		gl.glEnd();
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

//******************************************************************************
