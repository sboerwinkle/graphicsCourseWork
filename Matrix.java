
/**
 * Much of this class was shamelessly stolen from (http://www.jocl.org/samples/JOCLSimpleGL3.java).
 * -Simon
 */

public class Matrix {
	//=== Helper functions for matrix operations ==============================

	/**
	 * Helper method that creates a perspective matrix
	 * @param fovy The fov in y-direction, in degrees
	 *
	 * @param aspect The aspect ratio
	 * @param zNear The near clipping plane
	 * @param zFar The far clipping plane
	 * @return A perspective matrix
	 */
	static float[] perspective(
		float fovy, float aspect, float zNear, float zFar)
	{
		float radians = (float)Math.toRadians(fovy / 2);
		float deltaZ = zFar - zNear;
		float sine = (float)Math.sin(radians);
		if ((deltaZ == 0) || (sine == 0) || (aspect == 0))
		{
			return identity();
		}
		float cotangent = (float)Math.cos(radians) / sine;
		float m[] = identity();
		m[0*4+0] = cotangent / aspect;
		m[1*4+1] = cotangent;
		m[2*4+2] = -(zFar + zNear) / deltaZ;
		m[2*4+3] = -1;
		m[3*4+2] = -2 * zNear * zFar / deltaZ;
		m[3*4+3] = 0;
		return m;
	}

	static float[] lookDir(double eyeX, double eyeY, double eyeZ, double pitch, double yaw) {
		float cosP = (float)Math.cos(pitch);
		float sinP = (float)Math.sin(pitch);
		float cosY = (float)Math.cos(yaw);
		float sinY = (float)Math.sin(yaw);
		//Forward vector
		float fx = -cosP*sinY;
		float fy = sinP;
		float fz = -cosP*cosY;
		//Up vector
		float upX = sinP*sinY;
		float upY = cosP;
		float upZ = sinP*cosY;
		//System.out.println(fx*upX+fy*upY+fz*upZ);
		//sidways vector
		float sideX = fy*upZ-fz*upY;
		float sideY = fz*upX-fx*upZ;
		float sideZ = fx*upY-fy*upX;
		return multiply(
			new float[] {
				1,0,0,0,
				0,1,0,0,
				0,0,1,0,
				-(float)eyeX,(float)eyeY,-(float)eyeZ,1}
			,
			/*new float[] {
				sideX, sideY, sideZ, 0,
				upX, upY, upZ, 0,
				fx, fy, fz, 0,
				0,0,0,1}*/
			new float[] {
				sideX, upX, fx, 0,
				sideY, upY, fy, 0,
				sideZ, upZ, fz, 0,
				0,0,0,1}
			);
	}

	/**
	 * Creates an identity matrix
	 *
	 * @return An identity matrix
	 */
	static float[] identity()
	{
		float m[] = new float[16];
		m[0] = m[5] = m[10] = m[15] = 1.0f;
		return m;
	}

	/**
	 * Multiplies the given matrices and returns the result
	 *
	 * @param m0 The first matrix
	 * @param m1 The second matrix
	 * @return The product m0*m1
	 */
	static float[] multiply(float m0[], float m1[])
	{
		float m[] = new float[16];
		for (int x=0; x < 4; x++)
		{
			for(int y=0; y < 4; y++)
			{
				m[x*4 + y] =
					m0[x*4+0] * m1[y+ 0] +
					m0[x*4+1] * m1[y+ 4] +
					m0[x*4+2] * m1[y+ 8] +
					m0[x*4+3] * m1[y+12];
			}
		}
		return m;
	}
}
