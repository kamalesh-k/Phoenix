package com.phoenix.core;

import org.joml.Vector3f; // For position, rotation, scale, and vertex data

/**
 * Represents a single object in the 3D scene.
 * Stores its position, rotation, scale, name, shape type, and mesh data (vertices and indices).
 */
public class GameObject {

  // Constants for shape types
  public static final int SHAPE_CUBE = 0;
  public static final int SHAPE_SPHERE = 1;
  // Add more shape constants here as needed

  private Vector3f position; // Object's position in world space
  private Vector3f rotationAngles; // Object's rotation around X, Y, Z axes (Euler angles in degrees)
  private Vector3f scale; // Object's scale along X, Y, Z axes
  private String name; // Name of the object
  private int shapeType; // Type of shape (e.g., SHAPE_CUBE, SHAPE_SPHERE)

  // Mesh Data for vertex/edge manipulation
  private Vector3f[] vertices; // Array of vertex positions in object's local space
  private int[] indices; // Array of indices defining triangles or lines


  /**
   * Constructs a new GameObject.
   * @param shapeType The type of shape for this object (use SHAPE_ constants).
   * @param name The name of the object.
   */
  public GameObject(int shapeType, String name) {
    this.position = new Vector3f(0.0f, 0.0f, 0.0f); // Default position at origin
    this.rotationAngles = new Vector3f(0.0f, 0.0f, 0.0f); // Default rotation (no rotation)
    this.scale = new Vector3f(1.0f, 1.0f, 1.0f); // Default scale (unit size)
    this.name = name;
    this.shapeType = shapeType;

    // Mesh data will be populated by ShapeRenderer when the object is created
    this.vertices = null;
    this.indices = null;
  }

  /**
   * Constructs a new GameObject with mesh data.
   * This constructor is typically called by ShapeRenderer.
   * @param shapeType The type of shape for this object.
   * @param name The name of the object.
   * @param vertices The array of vertex positions for the object's mesh.
   * @param indices The array of indices defining the mesh's structure.
   */
  public GameObject(int shapeType, String name, Vector3f[] vertices, int[] indices) {
    this(shapeType, name); // Call the primary constructor
    this.vertices = vertices;
    this.indices = indices;
  }


  // --- Getter Methods ---
  public Vector3f getPosition() {
    return position;
  }

  public Vector3f getRotationAngles() {
    return rotationAngles;
  }

  public Vector3f getScale() {
    return scale;
  }

  public String getName() {
    return name;
  }

  public int getShapeType() {
    return shapeType;
  }

  /**
   * Gets the array of vertex positions for this object's mesh.
   * @return The array of Vector3f representing the vertices in local space.
   */
  public Vector3f[] getVertices() {
    return vertices;
  }

  /**
   * Gets the array of indices defining the mesh's structure.
   * @return The array of integers representing the indices.
   */
  public int[] getIndices() {
    return indices;
  }

  // --- Setter Methods (if needed, though direct access via getters is common for Vector3f) ---
  public void setPosition(Vector3f position) {
    this.position.set(position);
  }

  public void setRotationAngles(Vector3f rotationAngles) {
    this.rotationAngles.set(rotationAngles);
  }

  public void setScale(Vector3f scale) {
    this.scale.set(scale);
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setShapeType(int shapeType) {
    this.shapeType = shapeType;
  }

  /**
   * Sets the vertex data for this object.
   * @param vertices The array of vertex positions.
   */
  public void setVertices(Vector3f[] vertices) {
    this.vertices = vertices;
  }

  /**
   * Sets the index data for this object.
   * @param indices The array of indices.
   */
  public void setIndices(int[] indices) {
    this.indices = indices;
  }
}
