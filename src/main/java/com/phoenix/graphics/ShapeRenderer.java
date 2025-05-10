package com.phoenix.graphics;

import com.phoenix.core.GameObject; // Import GameObject to access mesh data
import org.joml.Vector3f; // For vertex data

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*; // Import OpenGL constants and functions
// Ensure you have the GLU dependency if using the Sphere in ShapeRenderer
// import static org.lwjgl.util.glu.GLU.*;


/**
 * Provides methods for rendering basic 3D shapes using fixed-function OpenGL.
 * Now includes a method to render a mesh using vertex and index data from a GameObject.
 */
public class ShapeRenderer {

  /**
   * Constructs a new ShapeRenderer.
   */
  public ShapeRenderer() {
    // Constructor is empty for now.
  }

  /**
   * Creates a GameObject representing a unit cube and populates its vertex and index data.
   * @param name The name for the cube object.
   * @return A new GameObject representing a cube.
   */
  public GameObject createCube(String name) {
    // Define the vertices of a unit cube (centered at origin, side length 1.0)
    Vector3f[] vertices = new Vector3f[] {
        new Vector3f(-0.5f, -0.5f, 0.5f),  // 0: Bottom-left-front
        new Vector3f( 0.5f, -0.5f, 0.5f),  // 1: Bottom-right-front
        new Vector3f( 0.5f,  0.5f, 0.5f),  // 2: Top-right-front
        new Vector3f(-0.5f,  0.5f, 0.5f),  // 3: Top-left-front
        new Vector3f(-0.5f, -0.5f, -0.5f), // 4: Bottom-left-back
        new Vector3f( 0.5f, -0.5f, -0.5f), // 5: Bottom-right-back
        new Vector3f( 0.5f,  0.5f, -0.5f), // 6: Top-right-back
        new Vector3f(-0.5f,  0.5f, -0.5f)  // 7: Top-left-back
    };

    // Define the indices for the triangles that make up the cube's faces
    // Each set of 3 indices defines one triangle.
    // This defines the faces for solid rendering.
    int[] indices = new int[] {
        // Front face
        0, 1, 2,
        2, 3, 0,
        // Back face
        4, 6, 5,
        6, 4, 7,
        // Top face
        3, 2, 6,
        6, 7, 3,
        // Bottom face
        0, 4, 5,
        5, 1, 0,
        // Right face
        1, 5, 6,
        6, 2, 1,
        // Left face
        4, 0, 3,
        3, 7, 4
    };

    // Create a new GameObject with the cube shape type, name, vertices, and indices
    return new GameObject(GameObject.SHAPE_CUBE, name, vertices, indices);
  }

  /**
   * Creates a GameObject representing a unit sphere and populates its vertex and index data.
   * This is a basic sphere generation. More complex spheres (e.g., with adjustable segments)
   * would require more sophisticated algorithms.
   * @param name The name for the sphere object.
   * @return A new GameObject representing a sphere.
   */
  public GameObject createSphere(String name) {
    // Basic sphere generation - for simplicity, this example will create a low-poly sphere.
    // A real engine would use a more robust sphere generation algorithm (e.g., UV sphere, Icosphere).

    int segments = 16; // Number of segments around the sphere
    int rings = 16; // Number of rings from pole to pole
    float radius = 0.5f; // Unit sphere radius

    List<Vector3f> vertexList = new ArrayList<>();
    List<Integer> indexList = new ArrayList<>();

    // Add vertices
    for (int i = 0; i <= rings; i++) {
      float phi = (float) Math.PI * i / rings; // Angle from the vertical axis
      for (int j = 0; j <= segments; j++) {
        float theta = (float) (2.0 * Math.PI * j / segments); // Angle around the vertical axis

        float x = radius * (float) Math.sin(phi) * (float) Math.cos(theta);
        float y = radius * (float) Math.cos(phi);
        float z = radius * (float) Math.sin(phi) * (float) Math.sin(theta);

        vertexList.add(new Vector3f(x, y, z));
      }
    }

    // Add indices for triangles
    for (int i = 0; i < rings; i++) {
      for (int j = 0; j < segments; j++) {
        int first = i * (segments + 1) + j;
        int second = first + segments + 1;

        // Two triangles per quad face
        indexList.add(first);
        indexList.add(second);
        indexList.add(first + 1);

        indexList.add(first + 1);
        indexList.add(second);
        indexList.add(second + 1);
      }
    }

    // Convert lists to arrays
    Vector3f[] vertices = vertexList.toArray(new Vector3f[0]);
    int[] indices = indexList.stream().mapToInt(Integer::intValue).toArray();


    // Create a new GameObject with the sphere shape type, name, vertices, and indices
    return new GameObject(GameObject.SHAPE_SPHERE, name, vertices, indices);
  }


  /**
   * Renders the solid mesh of a GameObject using its vertex and index data.
   * Assumes transformations (translation, rotation, scale) are already applied
   * via the modelview matrix.
   * Color should be set using glColor3f/4f before calling.
   * @param obj The GameObject whose mesh should be rendered.
   */
  public void renderMesh(GameObject obj) {
    if (obj == null || obj.getVertices() == null || obj.getIndices() == null) {
      System.err.println("Cannot render mesh: GameObject or its mesh data is null.");
      return;
    }

    Vector3f[] vertices = obj.getVertices();
    int[] indices = obj.getIndices();

    // In fixed-function OpenGL immediate mode, we iterate through indices
    // and draw triangles using the vertex data.
    // This is not efficient for complex meshes; VBOs are recommended for performance.
    glBegin(GL_TRIANGLES);
    for (int i = 0; i < indices.length; i += 3) {
      int v0Index = indices[i];
      int v1Index = indices[i + 1];
      int v2Index = indices[i + 2];

      // Ensure indices are valid
      if (v0Index < 0 || v0Index >= vertices.length ||
          v1Index < 0 || v1Index >= vertices.length ||
          v2Index < 0 || v2Index >= vertices.length) {
        System.err.println("Invalid index encountered during mesh rendering.");
        continue; // Skip this triangle
      }

      Vector3f v0 = vertices[v0Index];
      Vector3f v1 = vertices[v1Index];
      Vector3f v2 = vertices[v2Index];

      // Basic normal calculation for flat shading (for each triangle)
      // For smooth shading, you would need per-vertex normals.
      Vector3f normal = new Vector3f(v1).sub(v0).cross(new Vector3f(v2).sub(v0)).normalize();
      glNormal3f(normal.x, normal.y, normal.z);


      glVertex3f(v0.x, v0.y, v0.z);
      glVertex3f(v1.x, v1.y, v1.z);
      glVertex3f(v2.x, v2.y, v2.z);
    }
    glEnd();
  }


  /**
   * Renders a cube with the given side length.
   * NOTE: This method is kept for reference but renderMesh is now preferred for solid rendering.
   * @param size The desired size of the cube (length of each side).
   */
  public void renderCube(float size) {
    // This method is now less relevant as renderMesh is used for solid rendering.
    // It could be used for specific cases or removed if not needed.
    float halfSize = 1.0f / 2.0f; // Render a unit cube

    glBegin(GL_QUADS);
    // Front face
    glNormal3f(0.0f, 0.0f, 1.0f);
    glVertex3f(-halfSize, -halfSize, halfSize);
    glVertex3f(halfSize, -halfSize, halfSize);
    glVertex3f(halfSize, halfSize, halfSize);
    glVertex3f(-halfSize, halfSize, halfSize);

    // Back face
    glNormal3f(0.0f, 0.0f, -1.0f);
    glVertex3f(-halfSize, -halfSize, -halfSize);
    glVertex3f(-halfSize, halfSize, -halfSize);
    glVertex3f(halfSize, halfSize, -halfSize);
    glVertex3f(halfSize, -halfSize, -halfSize);

    // Top face
    glNormal3f(0.0f, 1.0f, 0.0f);
    glVertex3f(-halfSize, halfSize, -halfSize);
    glVertex3f(-halfSize, halfSize, halfSize);
    glVertex3f(halfSize, halfSize, halfSize);
    glVertex3f(halfSize, halfSize, -halfSize);

    // Bottom face
    glNormal3f(0.0f, -1.0f, 0.0f);
    glVertex3f(-halfSize, -halfSize, -halfSize);
    glVertex3f(halfSize, -halfSize, -halfSize);
    glVertex3f(halfSize, -halfSize, halfSize);
    glVertex3f(-halfSize, -halfSize, halfSize);

    // Right face
    glNormal3f(1.0f, 0.0f, 0.0f);
    glVertex3f(halfSize, -halfSize, -halfSize);
    glVertex3f(halfSize, halfSize, -halfSize);
    glVertex3f(halfSize, halfSize, halfSize);
    glVertex3f(halfSize, -halfSize, halfSize);

    // Left face
    glNormal3f(-1.0f, 0.0f, 0.0f);
    glVertex3f(-halfSize, -halfSize, -halfSize);
    glVertex3f(-halfSize, -halfSize, halfSize);
    glVertex3f(-halfSize, halfSize, halfSize);
    glVertex3f(-halfSize, halfSize, -halfSize);
    glEnd();
  }

  /**
   * Renders a sphere with the given radius using GLU.
   * NOTE: This method is kept for reference but renderMesh is now preferred for solid rendering.
   * @param radius The desired radius of the sphere.
   */
  public void renderSphere(float radius) {
    // This method is now less relevant as renderMesh is used for solid rendering.
    // It could be used for specific cases or removed if not needed.

         /*
         // Use GLU for simplicity for the sphere
         GLUquadric sphere = gluNewQuadric();
         gluSphere(sphere, 1.0f, 32, 32); // radius 1, 32 slices, 32 stacks
         gluDeleteQuadric(sphere); // Clean up the quadric object
         */
    // If using GLU, uncomment the above and ensure lwjgl-glu is in dependencies.
    // Otherwise, rely on renderMesh for sphere rendering using generated data.
  }


  /**
   * Cleans up any resources used by the ShapeRenderer (if any).
   */
  public void cleanup() {
    // No resources to clean up in this basic implementation.
  }
}
