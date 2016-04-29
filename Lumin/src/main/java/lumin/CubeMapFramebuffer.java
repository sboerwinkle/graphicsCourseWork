package lumin;

import java.io.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.texture.*;
public class CubeMapFramebuffer
{
    // These are actually addresses to these things
    private int cubeMapTex;
    private int frameBuffer;
    private int depthBuffer;

    public void init(GL2 gl, int width, int height)
    {

	//Creating a cube map texture
	int[] texAddr = new int[1];
	gl.glGenTextures(1, texAddr, 0);

	cubeMapTex = texAddr[0];

	// selecting an unused texture
	// (we COULD use the default, GL_TEXTURE0, but I want to be 
	//  explicit about the relationship between glActiveTexture()
	//  and glBindTexture().
	gl.glActiveTexture(gl.GL_TEXTURE7);
	gl.glBindTexture(gl.GL_TEXTURE_CUBE_MAP, cubeMapTex);

	gl.glTexParameteri(gl.GL_TEXTURE_CUBE_MAP, gl.GL_TEXTURE_WRAP_S, gl.GL_CLAMP);
	gl.glTexParameteri(gl.GL_TEXTURE_CUBE_MAP, gl.GL_TEXTURE_WRAP_T, gl.GL_CLAMP);
	gl.glTexParameteri(gl.GL_TEXTURE_CUBE_MAP, gl.GL_TEXTURE_WRAP_R, gl.GL_CLAMP);
	gl.glTexParameteri(gl.GL_TEXTURE_CUBE_MAP, gl.GL_TEXTURE_MIN_FILTER, gl.GL_LINEAR);
	gl.glTexParameteri(gl.GL_TEXTURE_CUBE_MAP, gl.GL_TEXTURE_MAG_FILTER, gl.GL_LINEAR);

	for(int i = 0; i < 6; i++)
	{
	    gl.glTexImage2D(gl.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, gl.GL_RGB, width, height, 0, gl.GL_RGB, gl.GL_UNSIGNED_BYTE, null);
	}

	//Creating a frame buffer object
	int[] fboAddr = new int[1];
	gl.glGenFramebuffers(1, fboAddr, 0);

	frameBuffer = fboAddr[0];

	gl.glBindFramebuffer(gl.GL_FRAMEBUFFER, frameBuffer);



	int[] dbAddr = new int[1];
	gl.glGenRenderbuffers(1, dbAddr, 0);

	depthBuffer = dbAddr[0];
	//Bind the depth buffer
	gl.glBindRenderbuffer(gl.GL_RENDERBUFFER, depthBuffer);

	//Allocate storage for it
	//TODO:This might need to be GL_DEPTH_COMPONENT16
	gl.glRenderbufferStorage(gl.GL_RENDERBUFFER, gl.GL_DEPTH_COMPONENT, width, height);
	//Unbind it
	gl.glBindRenderbuffer(gl.GL_RENDERBUFFER, 0);


	//Since the frame buffer is still bound, we call this to bind the render buffer to it
	//(There was a mistake in the sample code I copied this from, they were attaching the framebuffer to itself as a renderbuffer)
	gl.glFramebufferRenderbuffer(gl.GL_FRAMEBUFFER, gl.GL_DEPTH_ATTACHMENT, gl.GL_RENDERBUFFER, depthBuffer);

	//Attach the X+ cube map texture
	gl.glFramebufferTexture2D(gl.GL_FRAMEBUFFER, gl.GL_COLOR_ATTACHMENT0, gl.GL_TEXTURE_CUBE_MAP_POSITIVE_X, cubeMapTex, 0);

	// now just disable what we bound above so we can do other things
	// TODO: This could be causing problems - try commenting it out.
	gl.glBindFramebuffer(gl.GL_FRAMEBUFFER, 0);
	gl.glBindTexture(gl.GL_TEXTURE_CUBE_MAP, 0);
    }
    public int getCubeTexture()
    {
	return cubeMapTex;
    }

    public void beginRendering(GL2 gl)
    {
    //I'm not sure what should go here...
    //Maybe we bind the texture and the framebuffer?
    // and possibly enable them??
    // TODO: Follow the dynamic cubemaps tutorial more closely???
	gl.glActiveTexture(gl.GL_TEXTURE7);
	//XXX: Is it actually necessary to rebind these?
	gl.glBindFramebuffer(gl.GL_FRAMEBUFFER, frameBuffer);
	gl.glBindTexture(gl.GL_TEXTURE_CUBE_MAP, cubeMapTex);
    }

    public void endRendering(GL2 gl)
    {
	//I *THINK* this is what goes here...
	gl.glBindFramebuffer(gl.GL_FRAMEBUFFER, 0);
	gl.glBindTexture(gl.GL_TEXTURE_CUBE_MAP, 0);
    }

    public void drawToFace(GL2 gl, int face)
    {
	//Attach new texture and renderbuffer to fbo?
	gl.glFramebufferTexture2D(gl.GL_FRAMEBUFFER, gl.GL_COLOR_ATTACHMENT0, gl.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, cubeMapTex, 0);

	gl.glClear(gl.GL_COLOR_BUFFER_BIT |  gl.GL_DEPTH_BUFFER_BIT);

	gl.glMatrixMode(gl.GL_PROJECTION);
	gl.glLoadIdentity();

	App.GLU.gluPerspective(90, 1, 1, 1000);

	gl.glMatrixMode(gl.GL_MODELVIEW);
	gl.glLoadIdentity();

	//Set up the lookat depending on the current face we are rendering to
	switch(face)
	{
	    case GL2.GL_TEXTURE_CUBE_MAP_POSITIVE_X:
		App.GLU.gluLookAt(0, 0, 0, 1, 0, 0, 0, 1, 0);
		break;
	    case GL2.GL_TEXTURE_CUBE_MAP_NEGATIVE_X:
		App.GLU.gluLookAt(0, 0, 0, -1,0, 0, 0, 1, 0);
		break;
	    case GL2.GL_TEXTURE_CUBE_MAP_POSITIVE_Y:
		App.GLU.gluLookAt(0, 0, 0, 0, 1, 0, 1, 0, 0);
		break;
	    case GL2.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y:
		App.GLU.gluLookAt(0, 0, 0, 0, -1, 0, 1, 0, 0);
		break;
	    case GL2.GL_TEXTURE_CUBE_MAP_POSITIVE_Z:
		App.GLU.gluLookAt(0, 0, 0, 0, 0, 1, 0, 1, 0);
		break;
	    case GL2.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z:
		App.GLU.gluLookAt(0, 0, 0, 0, 0, -1, 0, 1, 0);
		break;
	    default:
		break;
	};

	//Where are the coordinates for where we should center the cubemap?
	//gl.glTranslatef(

	//From this point on, we are ready to draw, I think.

    }

}
