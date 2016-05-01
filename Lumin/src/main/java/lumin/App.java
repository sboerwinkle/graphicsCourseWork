package lumin;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;
import java.nio.FloatBuffer;
import javax.swing.*;
import javax.media.opengl.*;
import javax.media.opengl.awt.GLJPanel;
import javax.media.opengl.glu.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.Texture;

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
	private ArrayList<Item>		diamonds;
	private ArrayList<Light>	lights;
	private int			selectedThing; // If no item is selected, this is 0
	private ArrayList<Haze>	hazes;

	private float[]			flashlightColor = {1f, 1f, 1f, 1f};

	ArrayList<Pulse> pulses;

	//These are for use with the shaders
	ShaderControl shaderControl;
	CubeMapFramebuffer cubeMapFB;
	float[] projMatrix;
	float[] modelMatrix;
	float[] viewMatrix;

	int vbo, vao;
	int cameraPosLoc, cubeMapLoc, projLoc, modelLoc, viewLoc;

	Texture	bricks;
	Texture concrete;

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
		
		items.add(new Cube(0, 0, 10, 1, new Color[] {Color.blue, Color.green, Color.red}));
		items.add(new Sphere(0, 0, -10, 1, Color.green));
		items.add(new Sphere(4, -.5, 0, .5, Color.black));
		items.add(new Sphere(4, +.5, 0, .5, Color.white));
		items.add(new Sphere(4, 0, -.5, .5, Color.red));
		items.add(new Sphere(3.5, 0, 0, .5, Color.yellow));
		items.add(new Sphere(4, 0, +.5, .5, Color.blue));
		items.add(new Sphere(4.5, 0, 0, .5, Color.green));

		diamonds = new ArrayList<Item>();

		diamonds.add(new Cube(0, 0, 0, .75, new Color[] {new Color(185/256.0f, 242/256.0f, 1.0f, .75f)}));

		lights = new ArrayList<Light>(); // Aah let people add lights ore somthing
		lights.add(new Light(0, 0, 0));
		lights.add(new Light(0, 0, 6));
		//items.add(new Cube(0, 0, 4.5, 10, new Color[] {new Color(.66f, .13f, .9f, .8f)}));	//purplish


		hazes = new ArrayList<Haze>();	
		hazes.add(new Haze(new double[]{0, 0, 0}, 2, new Color(.75f, .75f, .75f, .0065f), .080));
		selectedThing = items.size() + lights.size();
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

		shaderControl = new ShaderControl();
		//Put the shader scripts in the main folder, with the build/ and src/ directories
		shaderControl.vSource = shaderControl.loadShader("../../../../vxShader.txt");
		shaderControl.fSource = shaderControl.loadShader("../../../../fgShader.txt");
		shaderControl.init(gl);

		cubeMapFB = new CubeMapFramebuffer();

		cubeMapFB.init(gl, 256, 256);

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
		projMatrix = Matrix.perspective(90.0f, 1.0f, 0.1f, 40.0f);
		modelMatrix = Matrix.identity();

		//We generate one vertex array object, to store the one vertex buffer object we will make
		int[] vaAddr = new int[1];
		//Generate a name address for the vao
		gl.glGenVertexArrays(1, vaAddr, 0);
		vao = vaAddr[0];
		//Bind the vao to the current context.
		gl.glBindVertexArray(vao);

		// We COULD use two vbos here, one for the vertices and one for the normals,
		// but as it turns out, we've stored all of the information into a single 
		// array of floats, so it is more convenient to use stride and offset with
		// glVertexAttribPointer.
		int[] vbAddr = new int[1];
		gl.glGenBuffers(1, vbAddr, 0);
		vbo = vbAddr[0];

		FloatBuffer ptsBuffer = FloatBuffer.wrap(cubeVerts);



		gl.glBindBuffer(gl.GL_ARRAY_BUFFER, vbo);
		gl.glEnableVertexAttribArray(0); //Position
		gl.glEnableVertexAttribArray(1); //Normal
		// configure the attribute pointers, with the appropriate stride and offset so that it can process the vertexes of the cube as well as the normals.
		gl.glVertexAttribPointer(0, 3, gl.GL_FLOAT, false, 4 * 6, 0);
		gl.glVertexAttribPointer(1, 3, gl.GL_FLOAT, false, 4 * 6, 12);
		gl.glBufferData(gl.GL_ARRAY_BUFFER, 6 * 36 * 4, ptsBuffer, gl.GL_STATIC_DRAW);

		//Unbind our VAO
		gl.glBindVertexArray(0);
		

		//Obtain the locations for uniform variables in our linked program code
		shaderControl.useShader(gl);

		
		int prgId = shaderControl.getId();

		cameraPosLoc= gl.glGetUniformLocation(prgId, "cameraPos");
		cubeMapLoc= gl.glGetUniformLocation(prgId, "cubeMap");
		modelLoc = gl.glGetUniformLocation(prgId, "model");
		viewLoc = gl.glGetUniformLocation(prgId, "view");
		projLoc = gl.glGetUniformLocation(prgId, "projection");

		gl.glUniformMatrix4fv(modelLoc, 1, false, modelMatrix, 0);
		gl.glUniformMatrix4fv(projLoc, 1, false, projMatrix, 0);

		shaderControl.dontUseShader(gl);
		
		// I'm not even sure if this works properly...
		//updateCubeMap(drawable);
		

		gl.glFogCoordf(0f);			//closer to 1 this is set, the more foggy what is drawn will appear

		//File f = new File("C:\\Users\\Rayne\\Google Drive\\Graphics\\graphicsCourseWork\\Lumin\\src\\main\\java\\lumin\\bricks.bmp");
		//File g = new File("C:\\Users\\Rayne\\Google Drive\\Graphics\\graphicsCourseWork\\Lumin\\src\\main\\java\\lumin\\concrete.jpg");
		File f = new File("../../../../src/main/java/lumin/bricks.bmp");
		File g = new File("../../../../src/main/java/lumin/concrete.jpg");
		
		try{bricks = TextureIO.newTexture(f, true);}
		catch (IOException e){
			System.out.println("Didn't work.");
		}

		try{concrete = TextureIO.newTexture(g, true);}
		catch (IOException e){
			System.out.println("Didn't work.");
		}

		//Print out version info
		System.out.println("Shading lang version: " + gl.glGetString(GL2.GL_SHADING_LANGUAGE_VERSION));
		System.out.println("OpenGL version: " + gl.glGetString(gl.GL_VERSION));
		
	}
	//Should render the scene 6 times at each of the six directions outward from the cube.
	public void updateCubeMap(GLAutoDrawable drawable)
	{
	    GL2 gl = drawable.getGL().getGL2();
	    cubeMapFB.beginRendering(gl);
	    for(int i = 0; i < 6; i++)
	    {
		cubeMapFB.drawToFace(gl, i);

		display(drawable);
	    }
	    cubeMapFB.endRendering(gl);
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
		viewMatrix = Matrix.lookDir(x, y, z, input.pitch, input.yaw);
		

		gl.glClear(GL.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
		shaderControl.useShader(gl);
		//Update the uniform variables in our shader scripts while we have the shader bound
		gl.glUniformMatrix4fv(viewLoc, 1, false, viewMatrix, 0);
		gl.glUniform1i(cubeMapLoc, 7);
		gl.glUniform3f(cameraPosLoc, (float)x, (float)y, (float)z);

		shaderControl.dontUseShader(gl);
		
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
	    // This part isn't working properly, so don't draw it.
	    /*
		shaderControl.useShader(gl);
		gl.glBindVertexArray(vao);
		gl.glBindTexture(gl.GL_TEXTURE_CUBE_MAP, cubeMapFB.getCubeTexture());
		gl.glDrawArrays(gl.GL_TRIANGLES, 0, 36);
		shaderControl.dontUseShader(gl);
	    */
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

		renderHallway(gl);

		for (Item i : items) i.render(gl);

		gl.glPushMatrix();
		gl.glTranslated(-4, 0, -0);
		gl.glRotated(45, 0,1,0);
		gl.glRotated(45, 1, 0, 0);
		

		for (Item d : diamonds) d.render(gl);

		gl.glPopMatrix();

		gl.glBegin(GL.GL_TRIANGLES);
		//Note that our perspective is looking down the negative z axis by default.

		// //gl.glFogCoordf(1f);
		// //White triangle up
		// gl.glNormal3f(-1, 0, 0);
		// gl.glColor4f(1.0f, 1.0f, 1.0f, .75f);
		// gl.glVertex3d(1, 0, 3);
		// gl.glVertex3d(-1, 0, 3);
		// gl.glVertex3d(0, 1, 3);
		// //Reddish triangle down
		// gl.glColor4f(1.0f, 0f, 0f, .75f);
		// gl.glVertex3d(1, 0, 3);
		// gl.glVertex3d(-1, 0, 3);
		// gl.glVertex3d(0, -1, 3);
		// //gl.glPopMatrix();

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
		if (selectedThing != items.size() + lights.size()) {
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
		float[] lightColor = flashlightColor;
		if (selectedThing < items.size()) {
			Item i = items.get(selectedThing);
			if (i instanceof Sphere) {
				lightColor = ((Sphere)i).color;
			}
		} else if (selectedThing < items.size() + lights.size()) {
			lightColor = lights.get(selectedThing-items.size()).color;
		}
		for (int i = 0; i < 3; i++) {
			int v = input.getVec(3+i);
			if (v == 0) continue;
			lightColor[i] = (v+1)/2;
		}

		//Switch the selected thing
		selectedThing = (selectedThing + input.cumulativeMouseTicks)%(items.size()+lights.size()+1);
		if (selectedThing < 0) selectedThing += items.size()+lights.size()+1;
		input.cumulativeMouseTicks = 0;

		if (input.actions[5]) {
			input.actions[5] = false;
			if (selectedThing != items.size() + lights.size()) {
				if (selectedThing < items.size()) items.remove(selectedThing);
				else lights.remove(selectedThing-items.size());
			}
		}

		if (input.actions[4]) {
			input.actions[4] = false;
			selectedThing = items.size() + lights.size();
		}

		//Move the selected thing to be in front of us
		if (input.mouseDown && selectedThing != items.size() + lights.size()) {
			if (selectedThing < items.size()) {
				Item i = items.get(selectedThing);
				grabVector(i.pos);
			} else {
				Light l = lights.get(selectedThing - items.size());
				grabVector(l.pos);
			}
		}

		//Add items if requested by user
		if (input.actions[1]) {
			input.actions[1] = false;
			addItem(new Cube(0, 0, 0, 1, new Color[] {Color.blue, Color.green, Color.red}));
		}
		if (input.actions[2]) {
			input.actions[2] = false;
			addItem(new Sphere(0, 0, 0, 1, new Color(flashlightColor[0], flashlightColor[1], flashlightColor[2])));
		}
		if (input.actions[3]) {
			input.actions[3] = false;
			addLight(new Light(0, 0, 0));
		}
	}

	private void renderHallway(GL2 gl){
		bricks.bind(gl);

		bricks.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
		bricks.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);

		bricks.enable(gl);

		gl.glColor3f(1f, 1f, 1f);

		double x = 3;
		double y = -2;
		double z = -13;

		texWall(gl, x, y, z, 0, 5, 10);
		texWall(gl, x, y, z+10, 3, 5, 0);
		texWall(gl, x+3, y, z+10, 0, 5, 6);
		texWall(gl, x+3, y, z+16, -3, 5, 0);
		texWall(gl, x, y, z+16, 0, 5, 10);
		texWall(gl, x, y, z+26, -6, 5, 0);

		texWall(gl, x-6, y, z+26, 0, 5, -10);
		texWall(gl, x-6, y, z+16, -3, 5, 0);
		texWall(gl, x-9, y, z+16, 0, 5, -6);
		texWall(gl, x-9, y, z+10, +3, 5, 0);
		texWall(gl, x-6, y, z+10, 0, 5, -10);
		texWall(gl, x-6, y, z, +6, 5, 0);

		bricks.disable(gl);

		concrete.bind(gl);

		concrete.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
		concrete.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);

		concrete.enable(gl);

		texFloor(gl, x+3, y, z, -12, 0, 26);
		//texFloor(gl, x, y, z, -6, 0, 20);

		concrete.disable(gl);

	}

	private void texWall(GL2 gl, double x, double y, double z, double dx, double dy, double dz){
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2d(0,0);
		gl.glVertex3d(x, y, z);
		gl.glTexCoord2d(Math.sqrt(Math.pow(dz,2) + Math.pow(dx, 2)),0);
		gl.glVertex3d(x+dx, y, z+dz);
		gl.glTexCoord2d(Math.sqrt(Math.pow(dz,2) + Math.pow(dx, 2)),dy);
		gl.glVertex3d(x+dx, y + dy, z + dz);
		gl.glTexCoord2d(0,dy);
		gl.glVertex3d(x, y+dy, z);
		gl.glEnd();
	}

	private void texFloor(GL2 gl, double x, double y, double z, double dx, double dy, double dz){
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2d(0,0);
		gl.glVertex3d(x, y, z);
		gl.glTexCoord2d(dz,0);
		gl.glVertex3d(x, y, z+dz);
		gl.glTexCoord2d(dz, dx);
		gl.glVertex3d(x+dx, y, z+dz);
		gl.glTexCoord2d(0,dx);
		gl.glVertex3d(x+dx, y, z);
		gl.glEnd();
	}

	private void grabVector(float[] pos) {
		pos[0] = (float)(x + Math.sin(input.yaw)*Math.cos(input.pitch)*3);
		pos[1] = (float)(y + Math.sin(input.pitch)*3);
		pos[2] = (float)(z + Math.cos(input.yaw)*Math.cos(input.pitch)*3);
	}

	private void addItem(Item i) {
		items.add(i);
		selectedThing = items.size() - 1;
		grabVector(i.pos);
	}

	private void addLight(Light l) {
		lights.add(l);
		selectedThing = items.size() + lights.size() - 1;
		grabVector(l.pos);
	}

	private float[] cubeVerts = {
	-0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
	 0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f, 
	 0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f, 
	 0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f, 
	-0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f, 
	-0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f, 

	-0.5f, -0.5f,  0.5f,  0.0f,  0.0f, 1.0f,
	 0.5f, -0.5f,  0.5f,  0.0f,  0.0f, 1.0f,
	 0.5f,  0.5f,  0.5f,  0.0f,  0.0f, 1.0f,
	 0.5f,  0.5f,  0.5f,  0.0f,  0.0f, 1.0f,
	-0.5f,  0.5f,  0.5f,  0.0f,  0.0f, 1.0f,
	-0.5f, -0.5f,  0.5f,  0.0f,  0.0f, 1.0f,

	-0.5f,  0.5f,  0.5f, -1.0f,  0.0f,  0.0f,
	-0.5f,  0.5f, -0.5f, -1.0f,  0.0f,  0.0f,
	-0.5f, -0.5f, -0.5f, -1.0f,  0.0f,  0.0f,
	-0.5f, -0.5f, -0.5f, -1.0f,  0.0f,  0.0f,
	-0.5f, -0.5f,  0.5f, -1.0f,  0.0f,  0.0f,
	-0.5f,  0.5f,  0.5f, -1.0f,  0.0f,  0.0f,

	 0.5f,  0.5f,  0.5f,  1.0f,  0.0f,  0.0f,
	 0.5f,  0.5f, -0.5f,  1.0f,  0.0f,  0.0f,
	 0.5f, -0.5f, -0.5f,  1.0f,  0.0f,  0.0f,
	 0.5f, -0.5f, -0.5f,  1.0f,  0.0f,  0.0f,
	 0.5f, -0.5f,  0.5f,  1.0f,  0.0f,  0.0f,
	 0.5f,  0.5f,  0.5f,  1.0f,  0.0f,  0.0f,

	-0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,
	 0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,
	 0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f,
	 0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f,
	-0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f,
	-0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,

	-0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f,
	 0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f,
	 0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,
	 0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,
	-0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,
	-0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f
    };
	
	

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
