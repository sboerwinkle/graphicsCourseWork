package lumin;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;
import javax.media.opengl.*;
import javax.media.opengl.awt.GLJPanel;
import javax.media.opengl.glu.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.gl2.GLUT;
//All of this was shamelessly copied from:
//www.guyford.co.uk/showpage.php?id=50&page=How_to_setup_and_load_GLSL_Shaders_in_JOGL_2.0
//it was hard to find examples of shaders being used in JOGL for much.
public class ShaderControl
{
    private int vertexPrgId;
    private int fragmentPrgId;
    private int prgId;
    public String[] vSource;
    public String[] fSource;

    public void init(GL2 gl)
    {
	try
	{
	    attachShaders(gl);
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	}
    }
    public int getId()
    {
	return prgId;
    }

    public String[] loadShader(String fileName)
    {
	StringBuilder sb = new StringBuilder();
	BufferedReader br;
	try
	{
	    br = new BufferedReader(new FileReader(fileName));
	    String line;
	    while((line = br.readLine()) != null)
	    {
		sb.append(line);
		sb.append("\n");
		//WHY ISN'T THERE AN APPENDLINE() METHOD??
		//THAT SEEMS PRETTY SILLY
	    }
	    br.close();
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	}
	System.out.println("Shader is " + sb.toString());
	return new String[] { sb.toString() };
    }
    
    private void attachShaders(GL2 gl) throws Exception
    {
	vertexPrgId = gl.glCreateShader(gl.GL_VERTEX_SHADER);
	fragmentPrgId = gl.glCreateShader(gl.GL_FRAGMENT_SHADER);
	gl.glShaderSource(vertexPrgId, 1, vSource, null, 0);
	gl.glCompileShader(vertexPrgId);

	gl.glShaderSource(fragmentPrgId, 1, fSource, null, 0);
	gl.glCompileShader(fragmentPrgId);

	prgId = gl.glCreateProgram();

	gl.glAttachShader(prgId, vertexPrgId);
	gl.glAttachShader(prgId, fragmentPrgId);
	gl.glLinkProgram(prgId);
	gl.glValidateProgram(prgId);

	int[] status = new int[1];
	gl.glGetShaderiv(vertexPrgId, gl.GL_COMPILE_STATUS, status, 0);
	if(status[0] != gl.GL_TRUE) 
	{
	    System.out.println("Vertex Shader failed compilation:");
	    byte[] buffer = new byte[512];
	    gl.glGetShaderInfoLog(vertexPrgId, 512, null, 0, buffer, 0);
	    System.out.println(new String(buffer));
	}
	
	gl.glGetShaderiv(fragmentPrgId, gl.GL_COMPILE_STATUS, status, 0);
	if (status[0] != gl.GL_TRUE) 
	{
	    System.out.println("Fragment Shader failed compilation:");
	    byte[] buffer = new byte[512];
	    gl.glGetShaderInfoLog(fragmentPrgId, 512, null, 0, buffer, 0);
	    System.out.println(new String(buffer));
	}
    }

    public int useShader(GL2 gl)
    {
	gl.glUseProgram(prgId);
	return prgId;
    }
    public void dontUseShader(GL2 gl)
    {
	gl.glUseProgram(0);
    }
}
