
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.nio.FloatBuffer;
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

	float perspectiveMatrix[];
	int MvpLocation;
	int vbo, prgId;

	public void		init(GLAutoDrawable drawable)
	{
		input.w = drawable.getWidth();
		input.h = drawable.getHeight();
		GL2 gl = drawable.getGL().getGL2();

		perspectiveMatrix = Matrix.perspective(45, 1, 0.1f, 20);
		String vertexProgram =
"#version 130\n" +
"in vec3 pos;" +
"in vec3 Color;" +
"out vec3 color;" +
"uniform mat4 MVP;" +
"void main(){" +
	"gl_Position = MVP*vec4(pos.xyz, 1);" +
	"color = Color;" +
"}";
		String fragmentProgram = 
"#version 130\n" +
"in vec3 color;" +
"out vec4 outColor;" +
"void main(){" +
	"outColor = vec4(color, 1);" +
"}";
		int vertexPrgId = gl.glCreateShader(gl.GL_VERTEX_SHADER);
		gl.glShaderSource(vertexPrgId, 1, new String[] {vertexProgram}, null, 0);
		gl.glCompileShader(vertexPrgId);

		int fragmentPrgId = gl.glCreateShader(gl.GL_FRAGMENT_SHADER);
		gl.glShaderSource(fragmentPrgId, 1, new String[] {fragmentProgram}, null, 0);
		gl.glCompileShader(fragmentPrgId);

		prgId = gl.glCreateProgram();
		System.out.println("Got program " + prgId);
		gl.glAttachShader(prgId, vertexPrgId);
		gl.glAttachShader(prgId, fragmentPrgId);
		gl.glLinkProgram(prgId);
		gl.glUseProgram(prgId);

		int[] status = new int[1];
		gl.glGetShaderiv(vertexPrgId, gl.GL_COMPILE_STATUS, status, 0);
		if (status[0] != gl.GL_TRUE) {
			System.out.println("Vertex Shader failed compilation:");
			byte[] buffer = new byte[512];
			gl.glGetShaderInfoLog(vertexPrgId, 512, null, 0, buffer, 0);
			for (int i = 0; i < 512; i++) {
				if (buffer[i] == 0) break;
				System.out.print((char)buffer[i]);
			}
			System.out.println();
		}

		gl.glGetShaderiv(fragmentPrgId, gl.GL_COMPILE_STATUS, status, 0);
		if (status[0] != gl.GL_TRUE) {
			System.out.println("Fragment Shader failed compilation:");
			byte[] buffer = new byte[512];
			gl.glGetShaderInfoLog(fragmentPrgId, 512, null, 0, buffer, 0);
			for (int i = 0; i < 512; i++) {
				if (buffer[i] == 0) break;
				System.out.print((char)buffer[i]);
			}
			System.out.println();
		}
		gl.glBindAttribLocation(prgId, 0, "pos");
		gl.glBindAttribLocation(prgId, 1, "Color");
		MvpLocation = gl.glGetUniformLocation(prgId, "MVP");

		int[] array = new int[1];
		gl.glGenBuffers(1, array, 0);
		vbo = array[0];

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

		//gl.glMatrixMode(gl.GL_MODELVIEW);
		//gl.glLoadIdentity();
		//input.pitch = input.yaw = 0;
		float[] m = Matrix.multiply(Matrix.lookDir(x, y, z, input.pitch, input.yaw), perspectiveMatrix);
		gl.glUseProgram(prgId);
		gl.glUniformMatrix4fv(MvpLocation, 1, false, m, 0);
		/*gl.glUniformMatrix4fv(MvpLocation, 1, false, new float[] {
			1,0,0,0,
			0,1,0,0,
			0,0,0,0,
			0,0,0,1}, 0);*/

		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
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
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
		gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 24, 0);
		gl.glVertexAttribPointer(1, 3, GL.GL_FLOAT, false, 24, 12);
		gl.glEnableVertexAttribArray(0);
		gl.glEnableVertexAttribArray(1);
		gl.glEnable(GL.GL_DEPTH_TEST);

		/*int[] results = new int[1];
		gl.glGetIntegerv(gl.GL_CURRENT_PROGRAM, results, 0);
		System.out.println("Have program " + results[0] + " bound");
		gl.glGetVertexAttribiv(0, gl.GL_VERTEX_ATTRIB_ARRAY_STRIDE, results, 0);
		System.out.println("Stride of 0 is "+results[0]);
		gl.glGetVertexAttribiv(1, gl.GL_VERTEX_ATTRIB_ARRAY_STRIDE, results, 0);
		System.out.println("Stride of 1 is "+results[0]);
		gl.glGetVertexAttribiv(0, gl.GL_VERTEX_ATTRIB_ARRAY_ENABLED, results, 0);
		System.out.println("Enabled of 0 is "+results[0]);
		gl.glGetVertexAttribiv(1, gl.GL_VERTEX_ATTRIB_ARRAY_ENABLED, results, 0);
		System.out.println("Enabled of 1 is "+results[0]);*/

		gl.glBufferData(GL.GL_ARRAY_BUFFER, 4*6*6, FloatBuffer.wrap(new float[]
		//White triangle up
			{ 1, 0, 3,	1, 1, 1,
			 -1, 0, 3,	1, 1, 1,
			  0, 1, 3,	1, 1, 1,
		//Red triangle down
			  1, 0, 3,	1, 0, 0,
			 -1, 0, 3,	1, 0, 0,
			  0,-1, 3,	1, 0, 0}), gl.GL_STREAM_DRAW);
		//Draw 6 vertices starting at vertex 0
		gl.glDrawArrays(GL.GL_TRIANGLES, 0, 6);
		drawCube(gl, 0, 0, 4.5, 1, new Color[] {Color.RED, Color.GREEN, Color.BLUE});
		int code = gl.glGetError();
		if (code != 0) System.out.println(GLU.gluErrorString(code));
	}

	private void drawCube(GL2 gl, double double_x, double double_y, double double_z, double double_r, Color[] colors) {
		float x = (float)double_x;
		float y = (float)double_y;
		float z = (float)double_z;
		float r = (float)double_r;
		float[] data = new float[6*6*6];
		int nc = colors.length; // nc => numColors
		float[] dxs = {-r, r, r, -r};
		float[] dys = {-r, -r, r, r};
		int[] corners = {0, 1, 2, 2, 3, 0}; // To draw a square using a pair of triangles, visit corners in this order
		for (int dim = 0; dim < 3; dim++) {
			for (float dz : new float[] {-r, r}) {
				int ix1 = dim + (dz > 0 ? 3 : 0);
				Color col = colors[ix1%nc];
				for (int i = 0; i < 6; i++) {
					int ix = ix1*36+i*6;
					int c = corners[i];
					data[ix+0] = x;
					data[ix+1] = y;
					data[ix+2] = z;
					data[ix+dim] += dz;
					data[ix+(dim+1)%3] += dxs[c];
					data[ix+(dim+2)%3] += dys[c];
					data[ix+3] = col.getRed()/255f;
					data[ix+4] = col.getGreen()/255f;
					data[ix+5] = col.getBlue()/255f;
				}
			}
		}
		gl.glBufferData(GL.GL_ARRAY_BUFFER, 4*6*6*6, FloatBuffer.wrap(data), gl.GL_STREAM_DRAW);
		gl.glDrawArrays(GL.GL_TRIANGLES, 0, 6*6);
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
