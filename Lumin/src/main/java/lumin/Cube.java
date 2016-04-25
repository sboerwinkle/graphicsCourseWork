package lumin;

import javax.media.opengl.*;
import java.awt.Color;

public final class Cube extends Item {

	float r;
	Color[] colors;
	int nc; //num colors

	static float[] dxs = {1, -1, -1, 1};
	static float[] dys = {-1, -1, 1, 1};
	static int[] corners = {0, 1, 2, 2, 3, 0}; // To draw a square using a pair of triangles, visit corners in this order

	Cube(double X, double Y, double Z, double R, Color[] Cs) {
		super(X, Y, Z);
		r = (float)R;
		colors = Cs;
		nc = colors.length;
	}

	public void render(GL2 gl) {
		float[] data = new float[3];
		gl.glBegin(gl.GL_TRIANGLES);
		for (int dim = 0; dim < 3; dim++) {
			float[] normal = new float[3];
			for (float dz : new float[] {-r, r}) {
				normal[dim] = dz>0 ? 1 : -1;
				gl.glNormal3fv(normal, 0);
				Color col = colors[((dz>0?3:0)+dim)%nc];
				for (int i = 0; i < 6; i++) {
					int c = corners[i];
					for (int j = 0; j < 3; j++) data[j] = pos[j];
					data[dim] += dz;
					data[(dim+1)%3] += dz*dxs[c];
					data[(dim+2)%3] += r*dys[c];
					gl.glColor4f(col.getRed()/255f, col.getGreen()/255f, col.getBlue()/255f, col.getAlpha()/255f);
					gl.glVertex3fv(data, 0);
				}
			}
		}
		gl.glEnd();
	}

	public void renderShadow(GL2 gl, float[] lightPos) {
		float[] data = new float[9];
		for (int dim = 0; dim < 3; dim++) {
			for (float dz : new float[] {-1, 1}) {
				if ((lightPos[dim]-pos[dim])*dz <= r) continue;
				for (int i = 0; i < 6; i += 3) {
					for (int j = 0; j < 3; j++) {
						for (int k = 0; k < 3; k++) data[3*j+k] = pos[k];
						int c = corners[i+j];
						data[3*j+dim] += r*dz;
						data[3*j+(dim+1)%3] += r*dz*dxs[c];
						data[3*j+(dim+2)%3] += r*dys[c];
					}
					Shadows.renderShadowTri(gl, data, lightPos);
				}
			}
		}
	}
}
