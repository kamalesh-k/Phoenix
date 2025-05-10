package com.phoenix.core;

import imgui.ImGui;
// ImGuiImplGl3 and ImGuiImplGlfw are now used directly in PhoenixGuiLayer
// import imgui.gl3.ImGuiImplGl3;
// import imgui.glfw.ImGuiImplGlfw;

import org.lwjgl.opengl.GL; // Import GL for createCapabilities
import org.lwjgl.opengl.GL11;
import com.phoenix.graphics.ShapeRenderer;
import com.phoenix.graphics.Window; // Use the provided Window class
import com.phoenix.ui.PhoenixGuiLayer; // Import the ImGuiLayer
import org.joml.Vector3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.joml.Vector2f;
import org.joml.Intersectionf; // JOML's intersection utility

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.nio.FloatBuffer; // Required for BufferUtils
import org.lwjgl.BufferUtils; // Required for BufferUtils
import java.util.Stack; // For Undo/Redo
// import java.util.Iterator; // For removing elements from lists (Not directly used, but good to keep)
import java.util.Collections; // For sorting picked components

import static org.lwjgl.glfw.GLFW.*; // Import GLFW static methods
import static org.lwjgl.opengl.GL11.*; // Import OpenGL static methods



/**
 * The main class for the Phoenix Game Engine.
 * Manages the main game loop, window using the custom Window class,
 * scene objects, input processing, and rendering using fixed-function OpenGL.
 * Integrates with PhoenixGuiLayer for ImGui UI rendering.
 *
 * Includes features for object selection (now with refined raycasting),
 * transformation (translate, rotate, scale) in Global and Local modes,
 * and basic camera controls (orbit, pan, zoom).
 *
 * Implements G (Move), R (Rotate), S (Scale) shortcuts with axis locking (X, Y, Z, Shift+Axis)
 * and Blender-like behavior for global/local transformations.
 * Clicking outside objects/axes and dragging rotates the global axis (camera).
 * Clicking directly on global axes initiates global translation for the last selected object.
 * Improved object picking sensitivity using ray-sphere intersection.
 * Smoother and slower global axis translation and camera pan.
 * Added Ctrl + D to duplicate selected object at mouse click location on the grid plane (Y=0).
 * Added vertex and edge selection and dragging for the selected object.
 * Fixed rendering order to make vertices and edges visible.
 * Updated to use renderMesh for solid object rendering to show mesh deformation.
 * Refined object picking using ray-sphere intersection with JOML's utility.
 * Fixed normalization errors in picking logic.
 * Increased picking tolerances for vertices and edges.
 *
 * Updates in this version:
 * - Implemented double-click and drag for vertex, edge, and face manipulation.
 * - Refined click handling to distinguish between single-click selection and double-click drag initiation.
 * - Ensured smooth and correct transformation for component dragging using ray-plane intersection.
 * - Improved face dragging to move the face as a rigid unit by storing initial local vertex positions.
 * - Modified renderAxes to show negative global axes.
 * - Reinstated camera zoom functionality with Q/E keys.
 * - Object reshaping is achieved by direct vertex manipulation during component drag.
 * - Adjusted double-click distance tolerance for easier drag initiation.
 * - Added debugging print statements for picking and transformation logic.
 * - Ensured initial component positions are captured correctly on double-click.
 * - Included the necessary GameObject and Window classes.
 * - Included the missing render() method.
 * - Modified mouse handling to allow component dragging after a single click and drag.
 * - Refined drag initiation logic for components to trigger on mouse movement after selection.
 * - Added 'G' key shortcut to initiate dragging for selected components (Vertex, Edge, Face).
 * - Modified 'G' key handling to immediately start dragging for components.
 * - Corrected redundant variable declarations and a typo in mouse handling.
 * - Removed stray non-ASCII characters from a printf statement.
 * - Adjusted mouse drag logic to correctly handle camera orbit/pan and object transformations.
 * - Ensured dragging state is managed correctly for different interaction types.
 * - Refined drag initiation and stopping logic for camera, object, and component transformations.
 * - Ensured initial mouse position is captured correctly at the start of component drag.
 * - Refined mouse event handling to better distinguish between click, pre-selection, and drag initiation.
 * - Ensured component dragging is prioritized when a component is selected and the mouse is dragged.
 * - Improved Q/E key camera zoom for smoother effect.
 * - Extended global axis lines to match the grid plane size.
 * - Implemented Undo (Ctrl+Z) and Redo (Ctrl+Y) functionality.
 * - Added a simple state-saving mechanism for transformations.
 * - Implemented more precise object picking using ray-triangle intersection for the selected object.
 * - Implemented multi-selection for vertices, edges, and faces using Ctrl + Left Click.
 * - Updated component selection and rendering to handle multiple selections.
 * - Modified component dragging to transform all selected components of the same type.
 * - Fixed duplicate variable declaration in renderSelectedObjectComponents.
 * - Implemented multi-object selection using Ctrl + Left Click.
 * - Updated object transformations (Move, Rotate, Scale) to apply to all selected objects.
 * - Added debugging prints for multi-object transformation initiation and application.
 * - Refined mouse click handling to improve object selection reliability.
 * - Fixed typo 'dyDee' to 'dyScreen' on line 412.
 * - Corrected usage of 'currentSelectedObjectForComponents' to 'obj' in renderSelectedObjectComponents.
 * - Moved ctrlDown initialization to the update() method to avoid NullPointerException.
 * - Corrected multi-object transformation initiation to capture all initial states.
 * - Fixed NullPointerException in undo/redo by removing incorrect null object addition and ensuring correct state capture.
 * - Refined multi-object duplication positioning to maintain relative positions and ensured new duplicates are selected.
 * - Modified picking methods to return lists of components for multi-selection.
 * - Updated duplication logic to correctly handle component selections for duplicates.
 * - Improved face picking accuracy by checking face normal against camera view direction.
 * - Ensured correct handling of multiple picked vertices and edges in the selection logic.
 * - Reviewed and corrected multi-vertex selection logic with Ctrl+Click.
 * - Added comments about the limitations of vertex/edge occlusion picking in this fixed-function OpenGL context.
 * - Fixed compilation error by using the 'obj' parameter in renderSelectedObjectComponents.
 * - Fixed incomplete System.out.println statement in the update method.
 * - Corrected 'Cannot resolve symbol currentProjection' in getRayFromScreen method.
 * - Integrated ImGuiLayer for UI.
 * - Modified to use PhoenixGuiLayer as an ImGui.app.Application.
 * - Refactored to move OpenGL initialization and rendering calls to PhoenixGuiLayer.process().
 * - Removed dependency on separate Window class for input and window management.
 * - Updated input handling to use ImGui.getIO().
 * - **Reverted PhoenixGuiLayer to a non-Application class.**
 * - **Integrated the provided Window class for window and main loop management.**
 * - **Manual ImGui initialization and rendering calls in the main loop.**
 * - **Input handling uses Window and checks ImGui.getIO().WantCaptureMouse/Keyboard().**
 */
public class PhoenixGameEngine implements Runnable {
  private boolean running;
  private Thread gameThread;
  // Use the provided Window class for the main rendering window
  private Window gameWindow;
  private ShapeRenderer shapeRenderer;
  // PhoenixGuiLayer is now a UI component managed by the engine
  private PhoenixGuiLayer imGuiLayer;

  // currentWidth and currentHeight are managed by the Window class
  private int currentWidth = 800;
  private int currentHeight = 600;
  private final String TITLE = "Phoenix Game Engine";
  private final int FPS = 60; // FPS is now managed by the main loop timing

  private List<GameObject> objects = new ArrayList<>();
  // Changed selectedObjectIndex to a list for multi-selection
  private List<Integer> selectedObjects = new ArrayList<>();
  private int lastSelectedObjectIndex = -1; // Keep track of the last selected object for context
  // Removed lastSelectedObjectIndexForProperties as UI layer manages this now

  // Camera/View properties
  private float cameraDistance = 10.0f;
  private float cameraZoom = 1.0f;
  private float targetCameraZoom = 1.0f; // Target zoom for smooth transition
  private float cameraPitch = 30.0f;
  private float cameraYaw = -45.0f;
  private Vector3f cameraPanOffset = new Vector3f(0.0f, 0.0f, 0.0f);
  private float zoomSpeed = 1.05f; // Factor for zoom in/out

  // Transformation Modes
  public enum TransformMode { // Made public for ImGuiLayer
    GLOBAL, LOCAL
  }
  private TransformMode currentMode = TransformMode.GLOBAL;

  // Selection Modes (Blender-like)
  public enum SelectionMode { // Made public for ImGuiLayer
    OBJECT, VERTEX, EDGE, FACE
  }
  private SelectionMode currentSelectionMode = SelectionMode.OBJECT;


  // Transformation State
  private enum TransformationState {
    NONE, MOVE, ROTATE, SCALE, VERTEX_MOVE, EDGE_MOVE, FACE_MOVE
  }
  private TransformationState currentTransformationState = TransformationState.NONE;

  // Initial states for multi-object transformation
  private List<Vector3f> initialSelectedObjectPositions = new ArrayList<>();
  private List<Vector3f> initialSelectedObjectRotations = new ArrayList<>();
  private List<Vector3f> initialSelectedObjectScales = new ArrayList<>();

  private Vector3f initialObjectPosition = new Vector3f(); // Kept for single object context if needed
  private Vector3f initialObjectRotation = new Vector3f(); // Kept for single object context if needed
  private Vector3f initialObjectScale = new Vector3f(); // Kept for single object context if needed

  private double initialMouseX, initialMouseY; // Captured at the start of ANY drag/transform
  private int lockedAxis = -1; // -1: none, 0: X, 1: Y, 2: Z
  private boolean isAxisLocked = false;
  private boolean isPlaneLocked = false;
  private int lastKeyPressedForTransform = -1;

  // Component Manipulation State (Using Lists for Multi-selection)
  private List<Integer> selectedVertices = new ArrayList<>();
  private List<int[]> selectedEdges = new ArrayList<>(); // Store edge as {v1, v2}
  private List<Integer> selectedFaces = new ArrayList<>();

  // Initial positions for multi-component dragging
  private List<Vector3f> initialSelectedVertexPositions = new ArrayList<>();
  private List<Vector3f> initialSelectedEdgeStartVertexPositions = new ArrayList<>();
  private List<Vector3f> initialSelectedEdgeEndVertexPositions = new ArrayList<>();
  private List<Vector3f[]> initialSelectedFaceVerticesLocal = new ArrayList<>(); // List of {v0, v1, v2} arrays

  private Vector3f initialComponentCentroidWorldPosition = new Vector3f(); // Centroid of selected components
  private Vector3f initialRayDirectionForPlane = new Vector3f();

  // State for initiating component drag after single click
  private boolean isComponentPreSelected = false;
  private double preSelectMouseX = 0, preSelectMouseY = 0;
  private static final double DRAG_START_THRESHOLD_SQ = 5 * 5; // Minimum mouse movement (squared) to start drag


  // Input state tracking - Now using Window and checking ImGui.getIO()
  private double lastMouseX, lastMouseY; // Captured every frame for delta calculation
  private boolean leftMouseDown = false;
  private boolean middleMouseDown = false;
  private boolean shiftDown = false;
  private boolean ctrlDown; // Moved initialization to update()

  private boolean dragging = false; // General dragging flag


  // Double-click tracking (still kept for potential future use or alternative interaction)
  private long lastLeftClickTime = 0;
  private double lastLeftClickMouseX = 0, lastLeftClickMouseY = 0;
  private static final long DOUBLE_CLICK_MAX_DELAY = 300; // Milliseconds
  private static final double DOUBLE_CLICK_MAX_DIST_SQ = 10 * 10; // Max mouse movement (squared) for double click


  // Global Axis Translation State
  private int translatingGlobalAxis = 0;

  // Key state tracking for single press events - Now using Window and checking ImGui.getIO()
  private boolean keyPressHandled_Right = false;
  private boolean keyPressHandled_Left = false;
  private boolean keyPressHandled_T = false;
  private boolean keyPressHandled_G = false;
  private boolean keyPressHandled_R_Key = false;
  private boolean keyPressHandled_S = false;
  private boolean keyPressHandled_X = false;
  private boolean keyPressHandled_Y = false;
  private boolean keyPressHandled_Z = false;
  private boolean keyPressHandled_CtrlD = false;
  private boolean keyPressHandled_Tab = false;
  private boolean keyPressHandled_Q = false; // For camera zoom
  private boolean keyPressHandled_E = false; // For camera zoom
  private boolean keyPressHandled_Z_Undo = false; // Added for Undo
  private boolean keyPressHandled_Y_Redo = false; // Added for Redo


  // Undo/Redo Stacks
  private Stack<List<GameObjectState>> undoStack = new Stack<>();
  private Stack<List<GameObjectState>> redoStack = new Stack<>();
  private static final int MAX_UNDO_STATES = 50; // Limit undo history size

  // Helper class to store the state of a single GameObject
  private static class GameObjectState {
    int objectIndex;
    Vector3f position;
    Vector3f rotationAngles;
    Vector3f scale;
    Vector3f[] vertices; // Deep copy of vertices
    int[] indices; // Deep copy of indices
    String name;
    int shapeType;

    GameObjectState(int index, GameObject obj) {
      this.objectIndex = index;
      this.position = new Vector3f(obj.getPosition());
      this.rotationAngles = new Vector3f(obj.getRotationAngles());
      this.scale = new Vector3f(obj.getScale());
      // Deep copy vertices
      this.vertices = new Vector3f[obj.getVertices().length];
      for(int i = 0; i < obj.getVertices().length; i++) {
        this.vertices[i] = new Vector3f(obj.getVertices()[i]);
      }
      // Deep copy indices
      this.indices = Arrays.copyOf(obj.getIndices(), obj.getIndices().length);
      this.name = obj.getName();
      this.shapeType = obj.getShapeType();
    }

    // Method to apply this state back to a GameObject
    void applyTo(GameObject obj) {
      obj.getPosition().set(this.position);
      obj.getRotationAngles().set(this.rotationAngles);
      obj.getScale().set(this.scale);
      // Copy vertices back
      if (this.vertices.length == obj.getVertices().length) {
        for(int i = 0; i < this.vertices.length; i++) {
          obj.getVertices()[i].set(this.vertices[i]);
        }
      } else {
        System.err.println("GameObjectState.applyTo: Vertex count mismatch for object " + obj.getName());
        // Handle this case, maybe re-create the vertex buffer if possible, or log an error.
        // For this example, we'll just log an error and not apply vertex state.
      }
      // Indices are usually static for a shape, no need to copy back unless shape changes
      // obj.setIndices(Arrays.copyOf(this.indices, this.indices.length));
    }
  }


  // Grid properties
  private final int GRID_LINES = 20;
  private final float GRID_SPACING = 1.0f;

  // Buffers for getting matrices from OpenGL
  private double[] modelviewMatrixDoubleBuffer = new double[16];
  private double[] projectionMatrixDoubleBuffer = new double[16];
  private int[] viewport = new int[4];

  // Buffers to convert double[] to float[] for JOML Matrix4f.set(float[])
  private float[] modelviewMatrixFloatBuffer = new float[16];
  private float[] projectionMatrixFloatBuffer = new float[16];

  // JOML matrices for calculations
  private Matrix4f modelviewMatrix = new Matrix4f(); // Class field, can be used if it's the one intended
  private Matrix4f projectionMatrix = new Matrix4f(); // Class field, can be used if it's the one intended

  // Clear color for the OpenGL rendering (can be controlled by ImGui)
  private float[] clearColor = new float[]{0.2f, 0.2f, 0.2f, 1.0f};


  /**
   * Starts the game engine by creating and starting the game thread.
   */
  public void start() {
    running = true;
    gameThread = new Thread(this);
    gameThread.start();
  }

  /**
   * Stops the game engine by setting the running flag to false and joining the game thread.
   */
  public void stop() {
    running = false;
    try {
      if (gameThread != null) {
        if (gameThread.isAlive()) {
          gameThread.interrupt(); // Request interruption
        }
        gameThread.join(); // Wait for the thread to finish
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
      Thread.currentThread().interrupt(); // Re-interrupt the current thread
    }
  }

  /**
   * The main entry point for the game thread. Initializes the window, OpenGL,
   * scene objects, input processing, and runs the main game loop.
   */
  @Override
  public void run() {
    // Initialize the main game rendering window using the custom Window class
    gameWindow = new Window(currentWidth, currentHeight, TITLE);
    // The Window constructor already initializes GLFW, creates the window,
    // makes the context current, and calls GL.createCapabilities().

    shapeRenderer = new ShapeRenderer();

    // OpenGL setup that needs a current context
    GL11.glEnable(GL_DEPTH_TEST);
    // Add other necessary OpenGL states here (e.g., blending, culling)
    // GL11.glEnable(GL_BLEND);
    // GL11.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    // GL11.glEnable(GL_CULL_FACE);
    // GL11.glCullFace(GL_BACK);


    // Initialize ImGuiLayer, passing the engine instance and the window handle
    imGuiLayer = new PhoenixGuiLayer(this);
    imGuiLayer.init(gameWindow.getWindowHandle()); // Pass the GLFW window handle

    // Create initial game objects
    createInitialObjects();

    // Save the initial state for undo
    saveStateForUndo();

    // Main game loop
    long lastTime = System.nanoTime();
    double amountOfTicks = 60.0; // Target 60 updates per second
    double ns = 1000000000 / amountOfTicks;
    double delta = 0;
    long timer = System.currentTimeMillis();
    int frames = 0;
    int updates = 0;

    while (!gameWindow.shouldClose() && running) {
      long now = System.nanoTime();
      delta += (now - lastTime) / ns;
      lastTime = now;

      // Poll events (input, window resizing, etc.)
      gameWindow.pollEvents();

      // Update game state (input processing, game logic)
      // This is where the engine's input handling and game logic happens
      updateGameState();
      updates++;


      // Render the scene and ImGui UI
      // The ImGuiLayer's process method will handle both 3D rendering and ImGui rendering
      render(); // Call the combined render method

      // Swap buffers to display the rendered frame
      gameWindow.swapBuffers();
      frames++;

      // Update window title with FPS/UPS if needed (can also be done in ImGui)
      if (System.currentTimeMillis() - timer > 1000) {
        timer += 1000;
        // System.out.printf("FPS: %d, UPS: %d%n", frames, updates);
        // gameWindow.setTitle(String.format("%s - FPS: %d, UPS: %d", TITLE, frames, updates));
        frames = 0;
        updates = 0;
      }

      // Simple sleep to avoid consuming too much CPU if updates are faster than desired
      // This is a basic approach; a proper game loop might use more sophisticated timing.
      try {
        Thread.sleep(1); // Sleep for a short duration
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt(); // Restore interrupt flag
        break; // Exit loop if interrupted
      }
    }

    // Cleanup resources after the main loop exits
    cleanup();
  }

  // Method to create initial game objects
  private void createInitialObjects() {
    shapeRenderer = new ShapeRenderer();

    GameObject cube = shapeRenderer.createCube("Cube");
    cube.getPosition().y = 0.5f;
    objects.add(cube);

    GameObject sphere = shapeRenderer.createSphere("Sphere");
    sphere.getPosition().set(3.0f, 0.5f, 0.0f);
    objects.add(sphere);

    GameObject secondCube = shapeRenderer.createCube("Cube 2");
    secondCube.getPosition().set(0.0f, 0.5f * 0.7f, 3.0f);
    secondCube.getScale().set(0.7f);
    objects.add(secondCube);

    System.out.println("Total objects initialized: " + objects.size());

    if (!objects.isEmpty()) {
      selectedObjects.add(0); // Select the first object initially
      lastSelectedObjectIndex = 0;
      System.out.println("Initially selected object index: " + selectedObjects.get(0));
    } else {
      selectedObjects.clear();
      lastSelectedObjectIndex = -1;
      System.out.println("No objects initialized. Selected index remains -1.");
    }
  }


  // Method for the UI layer to call to get the clear color
  public float[] getClearColor() {
    return clearColor;
  }

  // Method for the UI layer to call to update the game state
  // This method is called from the main game loop
  public void updateGameState() {
    // This method contains the game state update logic (input processing, physics, etc.)

    // Get input state from ImGui.getIO() to check if ImGui is capturing mouse/keyboard
    boolean imGuiWantsMouse = ImGui.getIO().getWantCaptureMouse();
    boolean imGuiWantsKeyboard = ImGui.getIO().getWantCaptureKeyboard();

    // Get mouse position from the Window class
    double mouseX = gameWindow.getMouseX();
    double mouseY = gameWindow.getMouseY();

    // Get mouse button state from the Window class
    leftMouseDown = gameWindow.isMouseButtonPressed(GLFW_MOUSE_BUTTON_LEFT);
    middleMouseDown = gameWindow.isMouseButtonPressed(GLFW_MOUSE_BUTTON_MIDDLE);

    // Get modifier key state from the Window class
    shiftDown = gameWindow.isKeyPressed(GLFW_KEY_LEFT_SHIFT) || gameWindow.isKeyPressed(GLFW_KEY_RIGHT_SHIFT);
    ctrlDown = gameWindow.isKeyPressed(GLFW_KEY_LEFT_CONTROL) || gameWindow.isKeyPressed(GLFW_KEY_RIGHT_CONTROL);


    // --- Mouse Click Handling ---
    // Only process engine mouse clicks if ImGui doesn't want the mouse AND the Window reports a click
    if (!imGuiWantsMouse && gameWindow.isLeftMouseClicked()) {
      long currentTime = System.currentTimeMillis();
      double dxScreen = mouseX - lastLeftClickMouseX;
      double dyScreen = mouseY - lastLeftClickMouseY;
      boolean isDoubleClick = (currentTime - lastLeftClickTime) < DOUBLE_CLICK_MAX_DELAY &&
          (dxScreen * dxScreen + dyScreen * dyScreen) < DOUBLE_CLICK_MAX_DIST_SQ;

      lastLeftClickTime = currentTime;
      lastLeftClickMouseX = mouseX;
      lastLeftClickMouseY = mouseY;

      // If a transformation or global axis translation is active, a left click confirms it
      if (currentTransformationState != TransformationState.NONE || translatingGlobalAxis != 0) {
        currentTransformationState = TransformationState.NONE;
        isAxisLocked = false;
        isPlaneLocked = false;
        lockedAxis = -1;
        lastKeyPressedForTransform = -1;
        isComponentPreSelected = false;
        dragging = false;
        translatingGlobalAxis = 0;
        saveStateForUndo(); // Save state after confirming transform
        System.out.println("Transformation confirmed by left click.");
        lastLeftClickTime = 0; // Reset for next double click
      } else {
        // If no transformation is active, a left click is for selection or initiating component drag
        boolean componentSelectedThisClick = false;
        boolean clickedOnCurrentSelectedObject = false;

        // Check if clicked on the bounding sphere of the last selected object (if any)
        if (!selectedObjects.isEmpty() && lastSelectedObjectIndex != -1 && lastSelectedObjectIndex < objects.size()) {
          GameObject currentSelectedObjectForComponents = objects.get(lastSelectedObjectIndex);
          Vector3f[] ray = getRayFromScreen(mouseX, mouseY);
          if (ray != null) {
            Vector3f rayOrigin = ray[0];
            Vector3f rayDirection = ray[1];
            GameObject obj = currentSelectedObjectForComponents;
            // Using a bounding sphere check first for quick rejection
            Vector3f sphereCenter = obj.getPosition();
            float sphereRadius = 0.5f * Math.max(obj.getScale().x, Math.max(obj.getScale().y, obj.getScale().z)) * 1.1f; // Add a small tolerance
            Vector2f result = new Vector2f(); // JOML stores the near/far intersection distances here
            if (Intersectionf.intersectRaySphere(rayOrigin, rayDirection, sphereCenter, sphereRadius * sphereRadius, result)) {
              // Check if the intersection is in front of the ray origin
              if (result.x >= 0 || result.y >= 0) {
                clickedOnCurrentSelectedObject = true;
              }
            }
          }
        }

        // If clicked on the last selected object's bounding sphere and not in Object selection mode, try to pick components
        if (!selectedObjects.isEmpty() && lastSelectedObjectIndex != -1 && lastSelectedObjectIndex < objects.size() && currentSelectionMode != SelectionMode.OBJECT) {
          GameObject currentSelectedObjectForComponents = objects.get(lastSelectedObjectIndex);
          if (clickedOnCurrentSelectedObject) { // Only try to pick components if clicked on the object's bounding sphere
            switch (currentSelectionMode) {
              case FACE:
                List<Integer> pickedFaces = pickFaces(mouseX, mouseY, currentSelectedObjectForComponents);
                if (!pickedFaces.isEmpty()) {
                  componentSelectedThisClick = true;
                  if (ctrlDown) { // Ctrl + Click for multi-selection/deselection
                    for(int pf : pickedFaces) {
                      if (selectedFaces.contains(pf)) selectedFaces.remove((Integer) pf);
                      else selectedFaces.add(pf);
                    }
                  } else { // Single click clears previous selection and selects the new one
                    selectedFaces.clear(); selectedFaces.add(pickedFaces.get(0));
                    selectedVertices.clear(); selectedEdges.clear(); // Clear other component types
                  }
                } else if (!ctrlDown) selectedFaces.clear(); // If no face picked and not Ctrl, clear face selection
                break;
              case VERTEX:
                List<Integer> pickedVertices = pickVertices(mouseX, mouseY, currentSelectedObjectForComponents);
                if (!pickedVertices.isEmpty()) {
                  componentSelectedThisClick = true;
                  if (ctrlDown) { // Ctrl + Click for multi-selection/deselection
                    for(int pv : pickedVertices) {
                      if (selectedVertices.contains(pv)) selectedVertices.remove((Integer) pv);
                      else selectedVertices.add(pv);
                    }
                  } else { // Single click clears previous selection and selects the new one
                    selectedVertices.clear(); selectedVertices.add(pickedVertices.get(0));
                    selectedEdges.clear(); selectedFaces.clear(); // Clear other component types
                  }
                } else if (!ctrlDown) selectedVertices.clear(); // If no vertex picked and not Ctrl, clear vertex selection
                break;
              case EDGE:
                List<int[]> pickedEdges = pickEdges(mouseX, mouseY, currentSelectedObjectForComponents);
                if (!pickedEdges.isEmpty()) {
                  componentSelectedThisClick = true;
                  if (ctrlDown) { // Ctrl + Click for multi-selection/deselection
                    for(int[] pe : pickedEdges) {
                      boolean alreadySelected = false; int edgeIndexToRemove = -1;
                      for(int i=0; i<selectedEdges.size(); ++i) {
                        int[] edge = selectedEdges.get(i);
                        // Check if the edge is already selected (order doesn't matter)
                        if ((edge[0] == pe[0] && edge[1] == pe[1]) || (edge[0] == pe[1] && edge[1] == pe[0])) {
                          alreadySelected = true; edgeIndexToRemove = i; break;
                        }
                      }
                      if (alreadySelected) selectedEdges.remove(edgeIndexToRemove);
                      else selectedEdges.add(Arrays.copyOf(pe, pe.length)); // Add a copy
                    }
                  } else { // Single click clears previous selection and selects the new one
                    selectedEdges.clear(); selectedEdges.add(Arrays.copyOf(pickedEdges.get(0), pickedEdges.get(0).length)); // Add a copy
                    selectedVertices.clear(); selectedFaces.clear(); // Clear other component types
                  }
                } else if (!ctrlDown) selectedEdges.clear(); // If no edge picked and not Ctrl, clear edge selection
                break;
              default: break;
            }
          }
          // Initiate pre-selection for component dragging if a component was clicked
          if (componentSelectedThisClick) {
            isComponentPreSelected = true; preSelectMouseX = mouseX; preSelectMouseY = mouseY;
          } else if (clickedOnCurrentSelectedObject && !ctrlDown) {
            // If clicked on the object but no component was picked (and not multi-selecting), deselect components
            selectedVertices.clear(); selectedEdges.clear(); selectedFaces.clear();
            isComponentPreSelected = false;
          } else {
            // If clicked outside the object or no object was selected, clear component pre-selection
            isComponentPreSelected = false;
          }
        }

        // If no component was selected (or in Object selection mode), try to pick an object or global axis
        if (!componentSelectedThisClick || currentSelectionMode == SelectionMode.OBJECT) {
          int clickedObjectIndex = pickObject(mouseX, mouseY);
          if (clickedObjectIndex != -1) {
            if (ctrlDown) { // Ctrl + Click for multi-object selection/deselection
              if (selectedObjects.contains(clickedObjectIndex)) selectedObjects.remove((Integer) clickedObjectIndex);
              else selectedObjects.add(clickedObjectIndex);
            } else { // Single click clears previous selection and selects the new one
              selectedObjects.clear(); selectedObjects.add(clickedObjectIndex);
            }
            lastSelectedObjectIndex = clickedObjectIndex; // Update last selected object
            // Deselect components when selecting a new object
            selectedVertices.clear(); selectedEdges.clear(); selectedFaces.clear();
            isComponentPreSelected = false; // Clear component pre-selection
          } else {
            // If no object was clicked, check for global axis click
            int clickedAxis = pickGlobalAxis(mouseX, mouseY);
            // Only initiate global axis translation if an object is selected
            if (clickedAxis != 0 && !selectedObjects.isEmpty() && lastSelectedObjectIndex != -1 && lastSelectedObjectIndex < objects.size()) {
              translatingGlobalAxis = clickedAxis; // Set the axis being translated
              initialMouseX = mouseX; initialMouseY = mouseY; // Capture initial mouse position
              // Store initial positions for all selected objects for global axis translation
              captureInitialObjectStates();
              dragging = true; // Start dragging state
              isComponentPreSelected = false; // Clear component pre-selection
            } else {
              // If clicked nowhere, deselect everything
              selectedObjects.clear(); lastSelectedObjectIndex = -1;
              selectedVertices.clear(); selectedEdges.clear(); selectedFaces.clear();
              isComponentPreSelected = false;
            }
          }
        }
      }
    }


    // --- Right Click Handling ---
    // Only process engine right clicks if ImGui doesn't want the mouse AND the Window reports a click
    if (!imGuiWantsMouse && gameWindow.isRightMouseClicked()) {
      // If a transformation or global axis translation is active, a right click cancels it
      if (currentTransformationState != TransformationState.NONE || translatingGlobalAxis != 0) {
        // Revert object transformations if in object transformation state
        if (!selectedObjects.isEmpty() && currentTransformationState != TransformationState.VERTEX_MOVE && currentTransformationState != TransformationState.EDGE_MOVE && currentTransformationState != TransformationState.FACE_MOVE) {
          for(int i=0; i<selectedObjects.size(); ++i) {
            int objIdx = selectedObjects.get(i);
            // Ensure index and initial state list size are valid
            if (objIdx < objects.size() && i < initialSelectedObjectPositions.size()) {
              objects.get(objIdx).getPosition().set(initialSelectedObjectPositions.get(i));
              objects.get(objIdx).getRotationAngles().set(initialSelectedObjectRotations.get(i));
              objects.get(objIdx).getScale().set(initialSelectedObjectScales.get(i));
            }
          }
        } else if (!selectedObjects.isEmpty() && lastSelectedObjectIndex != -1 && lastSelectedObjectIndex < objects.size()) {
          // Revert component transformations for the last selected object if in component transformation state
          GameObject currentSelectedObjectForComponents = objects.get(lastSelectedObjectIndex);
          if (currentTransformationState == TransformationState.VERTEX_MOVE && !selectedVertices.isEmpty()) {
            for(int i=0; i<selectedVertices.size(); ++i) {
              int vtxIdx = selectedVertices.get(i);
              if (vtxIdx < currentSelectedObjectForComponents.getVertices().length && i < initialSelectedVertexPositions.size()) {
                currentSelectedObjectForComponents.getVertices()[vtxIdx].set(initialSelectedVertexPositions.get(i));
              }
            }
          } else if (currentTransformationState == TransformationState.EDGE_MOVE && !selectedEdges.isEmpty()) {
            for(int i=0; i<selectedEdges.size(); ++i) {
              int[] edge = selectedEdges.get(i);
              if (edge[0] < currentSelectedObjectForComponents.getVertices().length && edge[1] < currentSelectedObjectForComponents.getVertices().length &&
                  i < initialSelectedEdgeStartVertexPositions.size() && i < initialSelectedEdgeEndVertexPositions.size()) {
                currentSelectedObjectForComponents.getVertices()[edge[0]].set(initialSelectedEdgeStartVertexPositions.get(i));
                currentSelectedObjectForComponents.getVertices()[edge[1]].set(initialSelectedEdgeEndVertexPositions.get(i));
              }
            }
          } else if (currentTransformationState == TransformationState.FACE_MOVE && !selectedFaces.isEmpty()) {
            int[] faceVtxIndices = currentSelectedObjectForComponents.getIndices();
            for(int i=0; i<selectedFaces.size(); ++i) {
              int faceIdx = selectedFaces.get(i);
              int baseFaceVtxIdx = faceIdx * 3;
              if (baseFaceVtxIdx + 2 < faceVtxIndices.length && i < initialSelectedFaceVerticesLocal.size()) {
                Vector3f[] initialVtxPos = initialSelectedFaceVerticesLocal.get(i);
                for(int j=0; j<3; ++j) {
                  int meshVertexIndex = faceVtxIndices[baseFaceVtxIdx + j];
                  if (meshVertexIndex < currentSelectedObjectForComponents.getVertices().length) {
                    currentSelectedObjectForComponents.getVertices()[meshVertexIndex].set(initialVtxPos[j]);
                  }
                }
              }
            }
          }
        }
        // Reset transformation/translation state and flags
        currentTransformationState = TransformationState.NONE;
        isAxisLocked = false; isPlaneLocked = false; lockedAxis = -1; lastKeyPressedForTransform = -1;
        isComponentPreSelected = false; dragging = false; translatingGlobalAxis = 0;
        System.out.println("Transformation cancelled by right click.");
      }
    }

    // --- Keyboard Input Handling ---
    // Only process engine keyboard input if ImGui doesn't want it
    // (except for global keys like ESC, or modifiers like SHIFT/CTRL)
    // Using Window's isKeyPressed method
    if (gameWindow.isKeyPressed(GLFW_KEY_ESCAPE)) stop(); // ESC should always work

    if (!imGuiWantsKeyboard) {
      // Object Selection Navigation (Left/Right Arrow)
      if (gameWindow.isKeyPressed(GLFW_KEY_RIGHT)) {
        if (!keyPressHandled_Right) {
          if (!objects.isEmpty()) {
            int currentLastSelectedIndex = (lastSelectedObjectIndex != -1) ? lastSelectedObjectIndex : (selectedObjects.isEmpty() ? -1 : selectedObjects.get(0));
            int nextSelectedIndex = (currentLastSelectedIndex + 1) % objects.size();
            selectedObjects.clear(); selectedObjects.add(nextSelectedIndex); lastSelectedObjectIndex = nextSelectedIndex;
            // Reset transformation/translation state and component selection on object change
            translatingGlobalAxis = 0; currentTransformationState = TransformationState.NONE;
            isAxisLocked = false; isPlaneLocked = false; lockedAxis = -1; lastKeyPressedForTransform = -1;
            selectedVertices.clear(); selectedEdges.clear(); selectedFaces.clear();
            isComponentPreSelected = false; dragging = false;
          }
          keyPressHandled_Right = true; // Mark key press as handled
        }
      } else { keyPressHandled_Right = false; } // Reset handled flag on key release

      if (gameWindow.isKeyPressed(GLFW_KEY_LEFT)) {
        if (!keyPressHandled_Left) {
          if (!objects.isEmpty()) {
            int currentLastSelectedIndex = (lastSelectedObjectIndex != -1) ? lastSelectedObjectIndex : (selectedObjects.isEmpty() ? -1 : selectedObjects.get(0));
            int prevSelectedIndex = (currentLastSelectedIndex - 1 + objects.size()) % objects.size();
            selectedObjects.clear(); selectedObjects.add(prevSelectedIndex); lastSelectedObjectIndex = prevSelectedIndex;
            // Reset transformation/translation state and component selection on object change
            translatingGlobalAxis = 0; currentTransformationState = TransformationState.NONE;
            isAxisLocked = false; isPlaneLocked = false; lockedAxis = -1; lastKeyPressedForTransform = -1;
            selectedVertices.clear(); selectedEdges.clear(); selectedFaces.clear();
            isComponentPreSelected = false; dragging = false;
          }
          keyPressHandled_Left = true; // Mark key press as handled
        }
      } else { keyPressHandled_Left = false; } // Reset handled flag on key release

      // Toggle Transform Mode (T key)
      if (gameWindow.isKeyPressed(GLFW_KEY_T)) {
        if (!keyPressHandled_T) {
          currentMode = (currentMode == TransformMode.GLOBAL) ? TransformMode.LOCAL : TransformMode.GLOBAL;
          keyPressHandled_T = true; // Mark key press as handled
        }
      } else { keyPressHandled_T = false; } // Reset handled flag on key release

      // Toggle Selection Mode (Tab key)
      if (gameWindow.isKeyPressed(GLFW_KEY_TAB)) {
        if (!keyPressHandled_Tab) {
          switch (currentSelectionMode) {
            case OBJECT: currentSelectionMode = SelectionMode.VERTEX; break;
            case VERTEX: currentSelectionMode = SelectionMode.EDGE; break;
            case EDGE: currentSelectionMode = SelectionMode.FACE; break;
            case FACE: currentSelectionMode = SelectionMode.OBJECT; break;
          }
          // Clear component selections when changing selection mode
          selectedVertices.clear(); selectedEdges.clear(); selectedFaces.clear();
          isComponentPreSelected = false; dragging = false; // Reset drag state
          keyPressHandled_Tab = true; // Mark key press as handled
        }
      } else { keyPressHandled_Tab = false; } // Reset handled flag on key release

      // Initiate Transformations (G, R, S keys) - Only if no transform/translation is active
      if (currentTransformationState == TransformationState.NONE && translatingGlobalAxis == 0) {
        // Initiate Move (G key)
        if (gameWindow.isKeyPressed(GLFW_KEY_G) && !keyPressHandled_G) {
          if (!selectedObjects.isEmpty()) {
            saveStateForUndo(); // Save state before transformation
            GameObject currentSelectedObjectForComponents = (!selectedObjects.isEmpty() && lastSelectedObjectIndex != -1 && lastSelectedObjectIndex < objects.size()) ? objects.get(lastSelectedObjectIndex) : null;
            boolean componentsSelected = currentSelectedObjectForComponents != null && (!selectedVertices.isEmpty() || !selectedEdges.isEmpty() || !selectedFaces.isEmpty());
            if (componentsSelected) {
              // If components are selected, initiate component move
              dragging = true; initialMouseX = mouseX; initialMouseY = mouseY;
              Matrix4f objTransform = getObjectTransformMatrix(currentSelectedObjectForComponents);
              Vector3f[] ray = getRayFromScreen(initialMouseX, initialMouseY);
              if (!selectedVertices.isEmpty()) {
                currentTransformationState = TransformationState.VERTEX_MOVE;
                initialSelectedVertexPositions.clear(); Vector3f centroid = new Vector3f(0,0,0);
                for(int vtxIdx : selectedVertices) {
                  if (vtxIdx < currentSelectedObjectForComponents.getVertices().length) {
                    initialSelectedVertexPositions.add(new Vector3f(currentSelectedObjectForComponents.getVertices()[vtxIdx]));
                    centroid.add(currentSelectedObjectForComponents.getVertices()[vtxIdx]);
                  }
                }
                if (!selectedVertices.isEmpty()) centroid.div(selectedVertices.size());
                initialComponentCentroidWorldPosition.set(centroid).mulPosition(objTransform);
                if (ray != null) initialRayDirectionForPlane.set(ray[1]);
              } else if (!selectedEdges.isEmpty()) {
                currentTransformationState = TransformationState.EDGE_MOVE;
                initialSelectedEdgeStartVertexPositions.clear(); initialSelectedEdgeEndVertexPositions.clear();
                Vector3f centroid = new Vector3f(0,0,0); int vertexCount = 0;
                for(int[] edge : selectedEdges) {
                  if (edge[0] < currentSelectedObjectForComponents.getVertices().length && edge[1] < currentSelectedObjectForComponents.getVertices().length) {
                    initialSelectedEdgeStartVertexPositions.add(new Vector3f(currentSelectedObjectForComponents.getVertices()[edge[0]]));
                    initialSelectedEdgeEndVertexPositions.add(new Vector3f(currentSelectedObjectForComponents.getVertices()[edge[1]]));
                    centroid.add(currentSelectedObjectForComponents.getVertices()[edge[0]]).add(currentSelectedObjectForComponents.getVertices()[edge[1]]);
                    vertexCount += 2;
                  }
                }
                if (vertexCount > 0) centroid.div(vertexCount);
                initialComponentCentroidWorldPosition.set(centroid).mulPosition(objTransform);
                if (ray != null) initialRayDirectionForPlane.set(ray[1]);
              } else if (!selectedFaces.isEmpty()) {
                currentTransformationState = TransformationState.FACE_MOVE;
                initialSelectedFaceVerticesLocal.clear(); Vector3f centroid = new Vector3f(0,0,0); int vertexCount = 0;
                int[] faceVtxIndices = currentSelectedObjectForComponents.getIndices();
                for(int faceIdx : selectedFaces) {
                  int baseFaceVtxIdx = faceIdx * 3;
                  if (baseFaceVtxIdx + 2 < faceVtxIndices.length) {
                    Vector3f[] initialVtxPos = new Vector3f[3];
                    for(int j=0; j<3; ++j) {
                      int meshVertexIndex = faceVtxIndices[baseFaceVtxIdx + j];
                      if (meshVertexIndex < currentSelectedObjectForComponents.getVertices().length) {
                        initialVtxPos[j] = new Vector3f(currentSelectedObjectForComponents.getVertices()[meshVertexIndex]);
                        centroid.add(currentSelectedObjectForComponents.getVertices()[meshVertexIndex]);
                        vertexCount++;
                      }
                    }
                    initialSelectedFaceVerticesLocal.add(initialVtxPos);
                  }
                }
                if (vertexCount > 0) centroid.div(vertexCount);
                initialComponentCentroidWorldPosition.set(centroid).mulPosition(objTransform);
                if (ray != null) initialRayDirectionForPlane.set(ray[1]);
              }
            } else {
              // If no components are selected, initiate object move
              currentTransformationState = TransformationState.MOVE;
              captureInitialObjectStates(); // Capture initial states of all selected objects
              initialMouseX = mouseX; initialMouseY = mouseY; dragging = true;
            }
            isComponentPreSelected = false; // Clear component pre-selection
            keyPressHandled_G = true; // Mark key press as handled
          }
        } else if (!gameWindow.isKeyPressed(GLFW_KEY_G)) keyPressHandled_G = false; // Reset handled flag

        // Initiate Rotate (R key)
        if (gameWindow.isKeyPressed(GLFW_KEY_R) && !keyPressHandled_R_Key) {
          if (!selectedObjects.isEmpty()) {
            saveStateForUndo(); // Save state before transformation
            currentTransformationState = TransformationState.ROTATE;
            captureInitialObjectStates(); // Capture initial states of all selected objects
            initialMouseX = mouseX; initialMouseY = mouseY;
            selectedVertices.clear(); selectedEdges.clear(); selectedFaces.clear(); // Deselect components
            isComponentPreSelected = false; dragging = true; // Start dragging state
          }
          keyPressHandled_R_Key = true; // Mark key press as handled
        } else if (!gameWindow.isKeyPressed(GLFW_KEY_R)) keyPressHandled_R_Key = false; // Reset handled flag

        // Initiate Scale (S key)
        if (gameWindow.isKeyPressed(GLFW_KEY_S) && !keyPressHandled_S) {
          if (!selectedObjects.isEmpty()) {
            saveStateForUndo(); // Save state before transformation
            currentTransformationState = TransformationState.SCALE;
            captureInitialObjectStates(); // Capture initial states of all selected objects
            initialMouseX = mouseX; initialMouseY = mouseY;
            selectedVertices.clear(); selectedEdges.clear(); selectedFaces.clear(); // Deselect components
            isComponentPreSelected = false; dragging = true; // Start dragging state
          }
          keyPressHandled_S = true; // Mark key press as handled
        } else if (!gameWindow.isKeyPressed(GLFW_KEY_S)) keyPressHandled_S = false; // Reset handled flag
      }

      // Axis and Plane Locking (X, Y, Z keys with/without Shift) - Only if a transform/translation is active
      if (currentTransformationState != TransformationState.NONE || translatingGlobalAxis > 0) {
        boolean xPressed = gameWindow.isKeyPressed(GLFW_KEY_X);
        boolean yPressed = gameWindow.isKeyPressed(GLFW_KEY_Y);
        boolean zPressed = gameWindow.isKeyPressed(GLFW_KEY_Z);
        if (xPressed && !keyPressHandled_X) { handleAxisLock(0, shiftDown); keyPressHandled_X = true; }
        else if (!xPressed) { keyPressHandled_X = false; if (lastKeyPressedForTransform == 0 && !yPressed && !zPressed) lastKeyPressedForTransform = -1;}
        if (yPressed && !keyPressHandled_Y) { handleAxisLock(1, shiftDown); keyPressHandled_Y = true; }
        else if (!yPressed) { keyPressHandled_Y = false; if (lastKeyPressedForTransform == 1 && !xPressed && !zPressed) lastKeyPressedForTransform = -1;}
        if (zPressed && !keyPressHandled_Z) { handleAxisLock(2, shiftDown); keyPressHandled_Z = true; }
        else if (!zPressed) { keyPressHandled_Z = false; if (lastKeyPressedForTransform == 2 && !xPressed && !yPressed) lastKeyPressedForTransform = -1;}
      } else {
        // Reset axis lock state if no transformation is active
        isAxisLocked = false; isPlaneLocked = false; lockedAxis = -1; lastKeyPressedForTransform = -1;
      }

      // Duplicate Selected Objects (Ctrl + D)
      boolean dPressed = gameWindow.isKeyPressed(GLFW_KEY_D);
      if (ctrlDown && dPressed) {
        if (!keyPressHandled_CtrlD) {
          if (!selectedObjects.isEmpty()) {
            saveStateForUndo(); // Save state before duplication
            duplicateSelectedObject(mouseX, mouseY);
            saveStateForUndo(); // Save state after duplication
          }
          keyPressHandled_CtrlD = true; // Mark key press as handled
        }
      } else { keyPressHandled_CtrlD = false; } // Reset handled flag

      // Camera Zoom (Q/E keys)
      if (gameWindow.isKeyPressed(GLFW_KEY_Q)) {
        targetCameraZoom *= zoomSpeed; if (targetCameraZoom > 20.0f) targetCameraZoom = 20.0f; // Zoom in, limit max zoom
      } else if (gameWindow.isKeyPressed(GLFW_KEY_E)) {
        targetCameraZoom /= zoomSpeed; if (targetCameraZoom < 0.05f) targetCameraZoom = 0.05f; // Zoom out, limit min zoom
      }

      // Undo (Ctrl + Z)
      if (ctrlDown && gameWindow.isKeyPressed(GLFW_KEY_Z)) {
        if (!keyPressHandled_Z_Undo) { undo(); keyPressHandled_Z_Undo = true; }
      } else if (!gameWindow.isKeyPressed(GLFW_KEY_Z)) { keyPressHandled_Z_Undo = false; }

      // Redo (Ctrl + Y)
      if (ctrlDown && gameWindow.isKeyPressed(GLFW_KEY_Y)) {
        if (!keyPressHandled_Y_Redo) { redo(); keyPressHandled_Y_Redo = true; }
      } else if (!gameWindow.isKeyPressed(GLFW_KEY_Y)) { keyPressHandled_Y_Redo = false; }
    } // End of !imGuiWantsKeyboard block

    // Smooth camera zoom interpolation
    float zoomInterpolationFactor = 0.1f;
    cameraZoom += (targetCameraZoom - cameraZoom) * zoomInterpolationFactor;


    // --- Mouse Drag Handling ---
    // Only process engine mouse drag if ImGui doesn't want the mouse
    if (!imGuiWantsMouse) {
      // Check if middle mouse button is down and dragging hasn't started (for camera pan/orbit)
      if (middleMouseDown && !dragging) {
        dragging = true; lastMouseX = mouseX; lastMouseY = mouseY;
      }
      // Check if left mouse button is down, dragging hasn't started, and a component is pre-selected
      else if (leftMouseDown && !dragging && isComponentPreSelected && !selectedObjects.isEmpty() && lastSelectedObjectIndex != -1 && lastSelectedObjectIndex < objects.size()) {
        // Check if mouse has moved beyond the drag start threshold
        double dxScreen = mouseX - preSelectMouseX; double dyScreen = mouseY - preSelectMouseY;
        if ((dxScreen * dxScreen + dyScreen * dyScreen) > DRAG_START_THRESHOLD_SQ) {
          dragging = true; initialMouseX = mouseX; initialMouseY = mouseY; lastMouseX = mouseX; lastMouseY = mouseY;
          saveStateForUndo(); // Save state at the start of the drag
          GameObject currentSelectedObjectForComponents = objects.get(lastSelectedObjectIndex);
          Matrix4f objTransform = getObjectTransformMatrix(currentSelectedObjectForComponents);
          Vector3f[] ray = getRayFromScreen(initialMouseX, initialMouseY);
          // Determine the type of component drag based on selection
          if (!selectedVertices.isEmpty()) {
            currentTransformationState = TransformationState.VERTEX_MOVE;
            initialSelectedVertexPositions.clear(); Vector3f centroid = new Vector3f(0,0,0);
            for(int vtxIdx : selectedVertices) {
              if (vtxIdx < currentSelectedObjectForComponents.getVertices().length) {
                initialSelectedVertexPositions.add(new Vector3f(currentSelectedObjectForComponents.getVertices()[vtxIdx]));
                centroid.add(currentSelectedObjectForComponents.getVertices()[vtxIdx]);
              }
            }
            if (!selectedVertices.isEmpty()) centroid.div(selectedVertices.size());
            initialComponentCentroidWorldPosition.set(centroid).mulPosition(objTransform);
            if (ray != null) initialRayDirectionForPlane.set(ray[1]);
          } else if (!selectedEdges.isEmpty()) {
            currentTransformationState = TransformationState.EDGE_MOVE;
            initialSelectedEdgeStartVertexPositions.clear(); initialSelectedEdgeEndVertexPositions.clear();
            Vector3f centroid = new Vector3f(0,0,0); int vertexCount = 0;
            for(int[] edge : selectedEdges) {
              if (edge[0] < currentSelectedObjectForComponents.getVertices().length && edge[1] < currentSelectedObjectForComponents.getVertices().length) {
                initialSelectedEdgeStartVertexPositions.add(new Vector3f(currentSelectedObjectForComponents.getVertices()[edge[0]]));
                initialSelectedEdgeEndVertexPositions.add(new Vector3f(currentSelectedObjectForComponents.getVertices()[edge[1]]));
                centroid.add(currentSelectedObjectForComponents.getVertices()[edge[0]]).add(currentSelectedObjectForComponents.getVertices()[edge[1]]);
                vertexCount += 2;
              }
            }
            if (vertexCount > 0) centroid.div(vertexCount);
            initialComponentCentroidWorldPosition.set(centroid).mulPosition(objTransform);
            if (ray != null) initialRayDirectionForPlane.set(ray[1]);
          } else if (!selectedFaces.isEmpty()) {
            currentTransformationState = TransformationState.FACE_MOVE;
            initialSelectedFaceVerticesLocal.clear(); Vector3f centroid = new Vector3f(0,0,0); int vertexCount = 0;
            int[] faceVtxIndices = currentSelectedObjectForComponents.getIndices();
            for(int faceIdx : selectedFaces) {
              int baseFaceVtxIdx = faceIdx * 3;
              if (baseFaceVtxIdx + 2 < faceVtxIndices.length) {
                Vector3f[] initialVtxPos = new Vector3f[3];
                for(int j=0; j<3; ++j) {
                  int meshVertexIndex = faceVtxIndices[baseFaceVtxIdx + j];
                  if (meshVertexIndex < currentSelectedObjectForComponents.getVertices().length) {
                    initialVtxPos[j] = new Vector3f(currentSelectedObjectForComponents.getVertices()[meshVertexIndex]);
                    centroid.add(currentSelectedObjectForComponents.getVertices()[meshVertexIndex]);
                    vertexCount++;
                  }
                }
                initialSelectedFaceVerticesLocal.add(initialVtxPos);
              }
            }
            if (vertexCount > 0) centroid.div(vertexCount);
            initialComponentCentroidWorldPosition.set(centroid).mulPosition(objTransform);
            if (ray != null) initialRayDirectionForPlane.set(ray[1]);
          }
          isComponentPreSelected = false; // Component is now being dragged, not just pre-selected
        }
      }
      // Stop dragging if mouse buttons are released and no transformation is active
      else if (!leftMouseDown && !middleMouseDown && dragging && currentTransformationState == TransformationState.NONE && translatingGlobalAxis == 0) {
        dragging = false; isComponentPreSelected = false;
      }
      // Start dragging for camera orbit/pan if left mouse is down, not already dragging, and no component is pre-selected
      else if (leftMouseDown && !dragging && currentTransformationState == TransformationState.NONE && translatingGlobalAxis == 0 && !isComponentPreSelected) {
        dragging = true; lastMouseX = mouseX; lastMouseY = mouseY;
      }

      // Apply transformations if dragging or a transformation state is active
      if (dragging || currentTransformationState != TransformationState.NONE || translatingGlobalAxis != 0) {
        float deltaX = (float) (mouseX - lastMouseX);
        float deltaY = (float) (mouseY - lastMouseY);

        // Camera Orbit/Pan (Middle Mouse or Left Mouse when no transformation/translation is active)
        if (middleMouseDown || (leftMouseDown && currentTransformationState == TransformationState.NONE && translatingGlobalAxis == 0)) {
          float cameraOrbitSpeed = 0.2f;
          if (shiftDown) { // Pan with Shift + Middle Mouse or Shift + Left Mouse (when not transforming)
            float cameraPanSpeed = 0.01f;
            cameraPanOffset.x -= deltaX * cameraPanSpeed * cameraDistance * 0.1f;
            cameraPanOffset.y += deltaY * cameraPanSpeed * cameraDistance * 0.1f;
          } else { // Orbit with Middle Mouse or Left Mouse (when not transforming)
            cameraYaw += deltaX * cameraOrbitSpeed;
            cameraPitch -= deltaY * cameraOrbitSpeed;
            cameraPitch = Math.max(-89.9f, Math.min(89.9f, cameraPitch));
          }
        }
        // Global Axis Translation (Left Mouse down on a global axis)
        else if (translatingGlobalAxis > 0 && !selectedObjects.isEmpty() && lastSelectedObjectIndex != -1 && lastSelectedObjectIndex < objects.size()) {
          // Apply global translation to all selected objects
          float translationSpeed = 0.01f;
          applyGlobalTranslationToSelectedObjects(deltaX, deltaY, translationSpeed);
        }
        // Object Transformations (Move, Rotate, Scale initiated by G, R, S keys)
        else if (!selectedObjects.isEmpty() &&
            (currentTransformationState == TransformationState.MOVE ||
                currentTransformationState == TransformationState.ROTATE ||
                currentTransformationState == TransformationState.SCALE)) {
          applyTransformationToSelectedObjects((float)(mouseX - initialMouseX), (float)(mouseY - initialMouseY));
        }
        // Component Transformations (Vertex, Edge, Face Move initiated by G key on selected components)
        else if (!selectedObjects.isEmpty() && lastSelectedObjectIndex != -1 && lastSelectedObjectIndex < objects.size() &&
            (currentTransformationState == TransformationState.VERTEX_MOVE ||
                currentTransformationState == TransformationState.EDGE_MOVE ||
                currentTransformationState == TransformationState.FACE_MOVE)) {
          GameObject currentSelectedObjectForComponents = objects.get(lastSelectedObjectIndex);
          applyVertexEdgeTransformation(currentSelectedObjectForComponents, mouseX, mouseY); // Pass current mouse position
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;
      }
    } // End of !imGuiWantsMouse block for drag handling


    // Update the status information to be displayed in the ImGui UI
    String sObjName = "None";
    if (!selectedObjects.isEmpty()) {
      if (selectedObjects.size() == 1 && selectedObjects.get(0) < objects.size()) {
        sObjName = objects.get(selectedObjects.get(0)).getName();
      } else if (!selectedObjects.isEmpty() && selectedObjects.get(0) < objects.size()) { // Ensure first object is valid
        sObjName = objects.get(selectedObjects.get(0)).getName() + " (+" + (selectedObjects.size() - 1) + ")";
      }
    }

    String sModeStr = currentMode.toString();
    String tStateStr = currentTransformationState.toString();
    String selModeStr = currentSelectionMode.toString();
    String lockAxStr = (lockedAxis == -1 ? "N" : (lockedAxis == 0 ? "X" : (lockedAxis == 1 ? "Y" : "Z"))) + (isPlaneLocked ? "P" : (isAxisLocked ? "" : ""));
    String globAxStr = (translatingGlobalAxis == 0 ? "N" : (translatingGlobalAxis == 1 ? "X" : (translatingGlobalAxis == 2 ? "Y" : "Z")));
    String objToTransName = (!selectedObjects.isEmpty() && lastSelectedObjectIndex != -1 && lastSelectedObjectIndex < objects.size()) ? objects.get(lastSelectedObjectIndex).getName() : "N/A";
    String compInfo = "";
    if (!selectedVertices.isEmpty()) compInfo += " Vtx: " + selectedVertices.size();
    if (!selectedEdges.isEmpty()) compInfo += " Edge: " + selectedEdges.size();
    if (!selectedFaces.isEmpty()) compInfo += " Face: " + selectedFaces.size();
    if (!compInfo.isEmpty()) compInfo = " (" + compInfo.trim() + ")";

    // Pass the updated status information to the ImGuiLayer
    imGuiLayer.updateStatusInfo(selModeStr, sObjName, compInfo, sModeStr, tStateStr, lockAxStr, globAxStr, objToTransName, cameraZoom, undoStack.size(), redoStack.size());

  }

  // The main render method, called from the game loop
  private void render() {
    // Clear the window's framebuffer
    gameWindow.clear(); // Use the Window's clear method

    // --- Render the 3D Scene ---
    // Set the clear color for the 3D scene (can be controlled by ImGui via clearColor field)
    GL11.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
    // Clear depth and color buffers before rendering the 3D scene
    GL11.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    // Set up the viewport using the Window's dimensions
    GL11.glViewport(0, 0, gameWindow.getWidth(), gameWindow.getHeight());
    // Update engine's internal width/height for projection calculations
    currentWidth = gameWindow.getWidth();
    currentHeight = gameWindow.getHeight();


    GL11.glMatrixMode(GL_PROJECTION); GL11.glLoadIdentity();
    // Use currentWidth/Height from the engine
    gluPerspective(45.0f, (float)currentWidth / currentHeight, 0.1f, 200.0f);

    GL11.glMatrixMode(GL_MODELVIEW); GL11.glLoadIdentity();
    GL11.glTranslatef(0,0, -cameraDistance * cameraZoom);
    GL11.glRotatef(cameraPitch,1,0,0); GL11.glRotatef(cameraYaw,0,1,0);
    GL11.glTranslatef(cameraPanOffset.x, cameraPanOffset.y, cameraPanOffset.z);

    renderGrid();
    renderAxes();

    for (int i = 0; i < objects.size(); i++) {
      GameObject obj = objects.get(i);
      GL11.glPushMatrix();
      GL11.glTranslatef(obj.getPosition().x, obj.getPosition().y, obj.getPosition().z);
      Vector3f rot = obj.getRotationAngles();
      // Apply rotation in YXZ order (Yaw, Pitch, Roll)
      GL11.glRotatef(rot.y,0,1,0); // Yaw around Y
      GL11.glRotatef(rot.x,1,0,0); // Pitch around X
      GL11.glRotatef(rot.z,0,0,1); // Roll around Z
      GL11.glScalef(obj.getScale().x, obj.getScale().y, obj.getScale().z);

      boolean isSelected = selectedObjects.contains(i);
      if (isSelected) {
        if (currentTransformationState != TransformationState.NONE && currentSelectionMode == SelectionMode.OBJECT) {
          switch(currentTransformationState){
            case MOVE: GL11.glColor3f(0.2f,1,0.2f); break; // Green for Move
            case ROTATE: GL11.glColor3f(1,0.2f,0.2f); break; // Red for Rotate
            case SCALE: GL11.glColor3f(0.2f,0.2f,1); break; // Blue for Scale
            default: GL11.glColor3f(1,0.6f,0); break; // Orange for selected but not transforming
          }
        } else if (isSelected && translatingGlobalAxis != 0) { // Highlight object when translating via global axis
          GL11.glColor3f(0.5f,0.8f,1); // Light blue
        }
        else GL11.glColor3f(1,0.6f,0); // Orange for selected object
      } else GL11.glColor3f(0.6f,0.6f,0.6f); // Grey for unselected object

      shapeRenderer.renderMesh(obj);

      // Render components for the last selected object if in component selection mode
      if (i == lastSelectedObjectIndex && currentSelectionMode != SelectionMode.OBJECT) {
        renderSelectedObjectComponents(obj);
      }
      GL11.glPopMatrix();
    }

    // Render mini-axes in the corner
    renderMiniAxes(currentWidth, currentHeight, new Vector3f(cameraPitch, cameraYaw, 0));

    // --- Render ImGui UI ---
    // Call the ImGuiLayer's process method to render the UI
    imGuiLayer.process();
  }


  private Matrix4f getObjectTransformMatrix(GameObject obj) {
    if (obj == null) return new Matrix4f().identity();
    // Build the transformation matrix in the correct order (Scale, Rotate, Translate)
    // Rotation order YXZ (Yaw, Pitch, Roll)
    return new Matrix4f()
        .translate(obj.getPosition())
        .rotateY((float) Math.toRadians(obj.getRotationAngles().y))
        .rotateX((float) Math.toRadians(obj.getRotationAngles().x))
        .rotateZ((float) Math.toRadians(obj.getRotationAngles().z))
        .scale(obj.getScale());
  }


  private void saveStateForUndo() {
    // Only save state if there's a change to track (e.g., not just camera movement)
    // A simple check is to see if the current state is different from the top of the stack,
    // but for simplicity, we'll save on specific actions (transform start, add object, delete object, property change).
    List<GameObjectState> currentState = new ArrayList<>();
    for (int i = 0; i < objects.size(); i++) {
      currentState.add(new GameObjectState(i, objects.get(i)));
    }
    // Avoid saving duplicate states if no actual change occurred (e.g., clicking without dragging)
    if (!undoStack.isEmpty() && statesAreEqual(undoStack.peek(), currentState)) {
      return;
    }
    undoStack.push(currentState);
    while (undoStack.size() > MAX_UNDO_STATES) {
      undoStack.remove(0); // Remove the oldest state
    }
    redoStack.clear(); // Clear redo stack on new action
    System.out.println("Undo state saved. Undo stack size: " + undoStack.size());
  }

  // Helper to check if two lists of GameObjectState are equal (shallow check on state values)
  private boolean statesAreEqual(List<GameObjectState> state1, List<GameObjectState> state2) {
    if (state1.size() != state2.size()) return false;
    for (int i = 0; i < state1.size(); i++) {
      GameObjectState s1 = state1.get(i);
      GameObjectState s2 = state2.get(i);
      if (s1.objectIndex != s2.objectIndex ||
          !s1.position.equals(s2.position) ||
          !s1.rotationAngles.equals(s2.rotationAngles) ||
          !s1.scale.equals(s2.scale) ||
          s1.shapeType != s2.shapeType ||
          !s1.name.equals(s2.name)) {
        return false;
      }
      // For vertices and indices, a deep comparison might be needed if they change independently of position/rotation/scale
      // For now, assuming vertices/indices only change during component editing, which is handled separately or implies a new object state.
    }
    return true;
  }


  private void applyState(List<GameObjectState> stateToApply) {
    objects.clear();
    for (GameObjectState savedState : stateToApply) {
      // When applying state, recreate the GameObject with saved vertices and indices
      GameObject obj = new GameObject(savedState.shapeType, savedState.name, savedState.vertices, savedState.indices);
      savedState.applyTo(obj); // Apply position, rotation, scale
      objects.add(obj);
    }
    selectedObjects.clear(); lastSelectedObjectIndex = -1; // Clear selection on undo/redo
    selectedVertices.clear(); selectedEdges.clear(); selectedFaces.clear(); // Clear component selection
    currentTransformationState = TransformationState.NONE; translatingGlobalAxis = 0; // Reset transformation state
    isComponentPreSelected = false; dragging = false; // Reset drag state
    isAxisLocked = false; isPlaneLocked = false; lockedAxis = -1; lastKeyPressedForTransform = -1; // Reset axis lock
    System.out.println("State applied. Current objects: " + objects.size());
  }

  private void undo() {
    if (undoStack.size() > 1) { // Need at least the initial state + one more to undo
      List<GameObjectState> currentStateForRedo = new ArrayList<>();
      for (int i = 0; i < objects.size(); i++) {
        currentStateForRedo.add(new GameObjectState(i, objects.get(i)));
      }
      redoStack.push(currentStateForRedo);
      undoStack.pop(); // Remove the current state
      List<GameObjectState> previousState = undoStack.peek(); // Get the state before the current one
      applyState(previousState);
      System.out.println("Undo performed. Undo stack size: " + undoStack.size() + ", Redo stack size: " + redoStack.size());
    } else {
      System.out.println("Cannot undo. Undo stack size: " + undoStack.size());
    }
  }

  private void redo() {
    if (!redoStack.isEmpty()) {
      List<GameObjectState> stateToRedo = redoStack.pop();
      List<GameObjectState> currentStateForUndo = new ArrayList<>();
      for (int i = 0; i < objects.size(); i++) {
        currentStateForUndo.add(new GameObjectState(i, objects.get(i)));
      }
      undoStack.push(currentStateForUndo);
      applyState(stateToRedo);
      System.out.println("Redo performed. Undo stack size: " + undoStack.size() + ", Redo stack size: " + redoStack.size());
    } else {
      System.out.println("Cannot redo. Redo stack size: " + redoStack.size());
    }
  }

  /**
   * Requests an undo state save when an object's property is changed via UI.
   * This method is called by the ImGui layer.
   *
   * @param objectIndex The index of the object whose property was changed.
   */
  public void requestUndoSaveForObjectPropertyChange(int objectIndex) {
    // We save the state of *all* objects to ensure consistency,
    // as changing one object's properties might affect the scene.
    // A more optimized approach could involve saving only the state
    // of the modified object and its dependencies, but for simplicity,
    // saving the full scene state is implemented here.
    saveStateForUndo();
    System.out.println("Undo state saved due to property change for object index: " + objectIndex);
  }


  private void handleAxisLock(int axis, boolean planeLock) {
    isPlaneLocked = planeLock;
    isAxisLocked = !planeLock;
    if (lastKeyPressedForTransform == axis && currentTransformationState != TransformationState.NONE && !isPlaneLocked) {
      lockedAxis = axis;
      // When double-pressing an axis key (e.g., XX), switch to local mode for that axis
      if (currentTransformationState == TransformationState.MOVE ||
          currentTransformationState == TransformationState.ROTATE ||
          currentTransformationState == TransformationState.SCALE) {
        currentMode = TransformMode.LOCAL;
      }
    } else {
      lockedAxis = axis;
      // Single press of an axis key defaults to global mode for that axis (or plane)
      if (currentTransformationState != TransformationState.NONE) {
        currentMode = TransformMode.GLOBAL;
      }
    }
    lastKeyPressedForTransform = axis;
    System.out.printf("Axis Lock: %d, Plane Lock: %b, Mode: %s%n", lockedAxis, isPlaneLocked, currentMode);
  }


  // Apply global translation to ALL selected objects
  private void applyGlobalTranslationToSelectedObjects(float deltaX, float deltaY, float speed) {
    if (selectedObjects.isEmpty() || initialSelectedObjectPositions.size() != selectedObjects.size()) {
      return;
    }

    // Calculate the total translation delta based on mouse movement and the locked axis
    Vector3f translationDelta = new Vector3f(0, 0, 0);
    // The direction of translation depends on the camera's orientation and the locked axis
    // For simplicity, global axis translation is applied along the world axes.
    // DeltaX affects the X-axis, DeltaY affects the Y-axis (inverted for screen Y).
    // If translating along Z, DeltaY affects the Z-axis (inverted).

    // This is a simplified global translation based on screen movement,
    // not a projection onto the world axes based on camera view.
    // A more accurate implementation would project mouse movement onto the world plane defined by the camera and the locked axis/plane.

    // Let's refine this to be more intuitive: DeltaX moves along the screen's horizontal, DeltaY along the screen's vertical.
    // We need to map this screen movement to world space translation along the selected global axis.

    // Get the current view matrix
    Matrix4f viewMatrix = new Matrix4f();
    viewMatrix.translate(0,0, -cameraDistance * cameraZoom);
    viewMatrix.rotateY((float) Math.toRadians(cameraYaw));
    viewMatrix.rotateX((float) Math.toRadians(cameraPitch));
    viewMatrix.translate(cameraPanOffset);
    Matrix4f invView = new Matrix4f(viewMatrix).invert();

    // Create a small movement vector in screen space
    Vector4f screenMove = new Vector4f(deltaX, -deltaY, 0, 0).mul(0.01f * cameraDistance * 0.1f); // Scale by distance and a sensitivity factor

    // Transform the screen movement vector into world space (direction only)
    Vector3f worldMoveDirection = invView.transformDirection(new Vector3f(screenMove.x, screenMove.y, 0)).normalize();

    // Apply the movement along the locked global axis
    Vector3f finalTranslation = new Vector3f(0,0,0);
    float moveAmount = (float) Math.sqrt(deltaX*deltaX + deltaY*deltaY) * speed * cameraDistance * 0.1f;

    switch (translatingGlobalAxis) {
      case 1: // X-axis
        finalTranslation.x = worldMoveDirection.x * moveAmount * Math.signum(deltaX); // Use sign of deltaX for direction
        break;
      case 2: // Y-axis
        finalTranslation.y = worldMoveDirection.y * moveAmount * Math.signum(-deltaY); // Use sign of -deltaY for direction
        break;
      case 3: // Z-axis
        finalTranslation.z = worldMoveDirection.z * moveAmount * Math.signum(-deltaY); // Use sign of -deltaY for direction
        break;
    }

    // Apply the calculated translation to all selected objects
    for (int i = 0; i < selectedObjects.size(); ++i) {
      int objIdx = selectedObjects.get(i);
      if (objIdx < objects.size() && i < initialSelectedObjectPositions.size()) {
        objects.get(objIdx).getPosition().set(initialSelectedObjectPositions.get(i)).add(finalTranslation);
      }
    }
  }


  private void captureInitialObjectStates() {
    initialSelectedObjectPositions.clear();
    initialSelectedObjectRotations.clear();
    initialSelectedObjectScales.clear();
    for(int objIdx : selectedObjects) {
      if (objIdx < objects.size()) {
        initialSelectedObjectPositions.add(new Vector3f(objects.get(objIdx).getPosition()));
        initialSelectedObjectRotations.add(new Vector3f(objects.get(objIdx).getRotationAngles()));
        initialSelectedObjectScales.add(new Vector3f(objects.get(objIdx).getScale()));
      }
    }
    System.out.println("Captured initial states for " + initialSelectedObjectPositions.size() + " objects.");
  }


  private void applyTransformationToSelectedObjects(float totalDeltaX, float totalDeltaY) {
    if (selectedObjects.isEmpty() || initialSelectedObjectPositions.size() != selectedObjects.size() ||
        initialSelectedObjectRotations.size() != selectedObjects.size() || initialSelectedObjectScales.size() != selectedObjects.size()) {
      return;
    }

    float sensitivity = 0.01f; float rotationSensitivity = 0.5f; float scaleSensitivity = 0.01f;

    // Calculate the transformation delta based on total mouse movement
    Vector3f transformDelta = new Vector3f();
    switch (currentTransformationState) {
      case MOVE:
        if (isAxisLocked && !isPlaneLocked) {
          float moveAmount = (totalDeltaX - totalDeltaY) * sensitivity * 5f;
          if (lockedAxis == 0) transformDelta.x = moveAmount;
          else if (lockedAxis == 1) transformDelta.y = moveAmount;
          else if (lockedAxis == 2) transformDelta.z = moveAmount;
          if (currentMode == TransformMode.LOCAL && !selectedObjects.isEmpty() && selectedObjects.get(0) < objects.size()) {
            // For local move, transform the delta by the first selected object's initial rotation
            Matrix4f initialRotMat = new Matrix4f()
                .rotateY((float)Math.toRadians(initialSelectedObjectRotations.get(0).y))
                .rotateX((float)Math.toRadians(initialSelectedObjectRotations.get(0).x))
                .rotateZ((float)Math.toRadians(initialSelectedObjectRotations.get(0).z));
            transformDelta.mulDirection(initialRotMat);
          }
        } else if (isPlaneLocked) {
          // Plane movement is tricky with just 2D mouse input.
          // A common approach is to project mouse movement onto a plane defined by the camera and the locked axis.
          // For simplicity here, let's use a simplified mapping based on the locked axis.
          // This might not be perfectly intuitive but provides some plane-like movement.
          if (lockedAxis == 0) { // YZ plane
            transformDelta.y = -totalDeltaY * sensitivity;
            transformDelta.z = totalDeltaX * sensitivity * (getViewportAspect() > 1 ? getViewportAspect() : 1);
          } else if (lockedAxis == 1) { // XZ plane
            transformDelta.x = totalDeltaX * sensitivity * (getViewportAspect() > 1 ? getViewportAspect() : 1);
            transformDelta.z = -totalDeltaY * sensitivity;
          } else { // XY plane
            transformDelta.x = totalDeltaX * sensitivity * (getViewportAspect() > 1 ? getViewportAspect() : 1);
            transformDelta.y = -totalDeltaY * sensitivity;
          }
        } else { // Free move (screen space)
          transformDelta.x = totalDeltaX * sensitivity;
          transformDelta.y = -totalDeltaY * sensitivity;
          // For free move, also consider movement along the depth axis based on mouse Y
          // This is a very simplified approach; a proper implementation would use ray-plane intersection
          // transformDelta.z = -totalDeltaY * sensitivity * 0.5f; // Example: map Y movement partially to Z
        }
        break;
      case ROTATE:
        float rotAmount = (totalDeltaX - totalDeltaY) * rotationSensitivity * 0.2f;
        if (isAxisLocked && !isPlaneLocked) {
          if (lockedAxis == 0) transformDelta.x = rotAmount;
          else if (lockedAxis == 1) transformDelta.y = rotAmount;
          else if (lockedAxis == 2) transformDelta.z = rotAmount;
        } else { // Free rotate (around screen X and Y axes relative to object)
          transformDelta.y = totalDeltaX * rotationSensitivity;
          transformDelta.x = -totalDeltaY * rotationSensitivity; // Note: mouse X maps to Y rotation, mouse Y maps to X rotation for camera-like orbit
        }
        break;
      case SCALE:
        float scaleAmount = 1.0f + ((-totalDeltaY + totalDeltaX) * 0.5f * scaleSensitivity);
        scaleAmount = Math.max(0.01f, scaleAmount); // Prevent scaling to zero or negative
        if (isAxisLocked && !isPlaneLocked) {
          if (lockedAxis == 0) transformDelta.x = scaleAmount; // Store the scale factor, not delta
          else if (lockedAxis == 1) transformDelta.y = scaleAmount;
          else if (lockedAxis == 2) transformDelta.z = scaleAmount;
        } else if (isPlaneLocked) {
          if (lockedAxis == 0) { transformDelta.y = scaleAmount; transformDelta.z = scaleAmount; }
          else if (lockedAxis == 1) { transformDelta.x = scaleAmount; transformDelta.z = scaleAmount; }
          else { transformDelta.x = scaleAmount; transformDelta.y = scaleAmount; }
        }
        else {
          transformDelta.set(scaleAmount); // Store the uniform scale factor
        }
        break;
      default: return; // Should not happen
    }

    // Apply the calculated delta to each selected object based on its initial state
    for (int i = 0; i < selectedObjects.size(); ++i) {
      int objIdx = selectedObjects.get(i);
      if (objIdx >= objects.size()) continue;
      GameObject obj = objects.get(objIdx);
      Vector3f initialPos = initialSelectedObjectPositions.get(i);
      Vector3f initialRot = initialSelectedObjectRotations.get(i);
      Vector3f initialScale = initialSelectedObjectScales.get(i);

      switch (currentTransformationState) {
        case MOVE:
          if (isAxisLocked && !isPlaneLocked && currentMode == TransformMode.LOCAL && !selectedObjects.isEmpty() && selectedObjects.get(0) < objects.size()) {
            // For local move, the delta is already transformed by the first selected object's initial rotation
            obj.getPosition().set(initialPos).add(transformDelta);
          } else {
            // For global move, or local move without axis lock, or plane lock, apply the delta directly
            obj.getPosition().set(initialPos).add(transformDelta);
          }
          break;
        case ROTATE:
          // Rotation is always applied relative to the object's current orientation
          // For global rotation, we rotate around world axes. For local, around local axes.
          Vector3f currentRotation = new Vector3f(initialRot);
          if (isAxisLocked && !isPlaneLocked) {
            if (lockedAxis == 0) currentRotation.x += transformDelta.x;
            else if (lockedAxis == 1) currentRotation.y += transformDelta.y;
            else if (lockedAxis == 2) currentRotation.z += transformDelta.z;
          } else {
            currentRotation.y += transformDelta.y;
            currentRotation.x += transformDelta.x; // Note: mouse X maps to Y rotation, mouse Y maps to X rotation for camera-like orbit
          }
          obj.getRotationAngles().set(currentRotation);
          break;
        case SCALE:
          Vector3f currentScale = new Vector3f(initialScale);
          if (isAxisLocked && !isPlaneLocked) {
            if (lockedAxis == 0) currentScale.x *= transformDelta.x;
            else if (lockedAxis == 1) currentScale.y *= transformDelta.y;
            else if (lockedAxis == 2) currentScale.z *= transformDelta.z;
          } else if (isPlaneLocked) {
            if (lockedAxis == 0) { currentScale.y *= transformDelta.y; currentScale.z *= transformDelta.z; }
            else if (lockedAxis == 1) { currentScale.x *= transformDelta.x; currentScale.z *= transformDelta.z; }
            else { currentScale.x *= transformDelta.x; currentScale.y *= transformDelta.y; }
          }
          else {
            currentScale.mul(transformDelta.x); // Apply uniform scale factor
          }
          obj.getScale().set(currentScale).max(new Vector3f(0.01f)); // Ensure scale doesn't go below a minimum
          break;
        default: break;
      }
    }
  }


  // Update viewport dimensions based on Window size (handled by Window's callback)
  // This method is no longer needed as the Window class manages the viewport.
  // public void updateViewportSize(int width, int height) {
  //     this.currentWidth = width;
  //     this.currentHeight = height;
  // }

  private float getViewportAspect() {
    // Get current width and height from the Window
    if (gameWindow == null || gameWindow.getHeight() == 0) return 1.0f;
    return (float) gameWindow.getWidth() / gameWindow.getHeight();
  }


  private void applyVertexEdgeTransformation(GameObject obj, double currentMouseX, double currentMouseY) {
    if (obj == null || currentTransformationState == TransformationState.NONE) return;
    if (!(currentTransformationState == TransformationState.VERTEX_MOVE ||
        currentTransformationState == TransformationState.EDGE_MOVE ||
        currentTransformationState == TransformationState.FACE_MOVE)) {
      return;
    }

    Vector3f[] ray = getRayFromScreen(currentMouseX, currentMouseY);
    if (ray == null) return;

    Vector3f currentRayOrigin = ray[0]; Vector3f currentRayDirection = ray[1];
    Vector3f planePoint = initialComponentCentroidWorldPosition;
    Vector3f planeNormal = new Vector3f(initialRayDirectionForPlane).normalize(); // Use the ray direction from initial click as the plane normal

    // Calculate intersection of the current ray with the plane defined at the start of the drag
    float t = 0; float denominator = currentRayDirection.dot(planeNormal);
    if (Math.abs(denominator) > 1e-6f) {
      t = new Vector3f(planePoint).sub(currentRayOrigin).dot(planeNormal) / denominator;
    } else return; // Ray is parallel to the plane
    // If t is negative, the intersection is behind the ray origin, which shouldn't happen with a ray from the camera
    if (Float.isNaN(t) || t < 0) return;

    // Calculate the new world position of the centroid on the plane
    Vector3f newCentroidWorldPosition = new Vector3f(currentRayOrigin).add(new Vector3f(currentRayDirection).mul(t));

    // Calculate the movement delta in world space
    Vector3f movementDeltaWorld = new Vector3f(newCentroidWorldPosition).sub(initialComponentCentroidWorldPosition);

    // Transform the movement delta into the object's local space at the time of the drag start
    // We need the inverse of the object's transformation matrix at the start of the drag.
    // Assuming initialObjectPosition, initialObjectRotation, initialObjectScale were captured for the object.
    // A more robust way would be to store the object's transform matrix at the start of component drag.
    // For now, let's use the initial object state captured for object transformations.
    // This assumes component dragging is only allowed for the last selected object.
    Matrix4f initialObjectTransform = new Matrix4f()
        .translate(initialObjectPosition) // Assuming initialObjectPosition is for the current object
        .rotateY((float) Math.toRadians(initialObjectRotation.y))
        .rotateX((float) Math.toRadians(initialObjectRotation.x))
        .rotateZ((float) Math.toRadians(initialObjectRotation.z))
        .scale(initialObjectScale);

    Matrix4f invInitialObjectTransform = new Matrix4f(initialObjectTransform).invert();
    Vector3f movementDeltaLocal = movementDeltaWorld.mulDirection(invInitialObjectTransform);

    // Apply the local movement delta to the selected components' initial local positions
    if (currentTransformationState == TransformationState.VERTEX_MOVE && !selectedVertices.isEmpty()) {
      for(int i=0; i<selectedVertices.size(); ++i) {
        int vtxIdx = selectedVertices.get(i);
        if (vtxIdx < obj.getVertices().length && i < initialSelectedVertexPositions.size()) {
          obj.getVertices()[vtxIdx].set(initialSelectedVertexPositions.get(i)).add(movementDeltaLocal);
        }
      }
    } else if (currentTransformationState == TransformationState.EDGE_MOVE && !selectedEdges.isEmpty()) {
      for(int i=0; i<selectedEdges.size(); ++i) {
        int[] edge = selectedEdges.get(i);
        if (edge[0] < obj.getVertices().length && edge[1] < obj.getVertices().length &&
            i < initialSelectedEdgeStartVertexPositions.size() && i < initialSelectedEdgeEndVertexPositions.size()) {
          obj.getVertices()[edge[0]].set(initialSelectedEdgeStartVertexPositions.get(i)).add(movementDeltaLocal);
          obj.getVertices()[edge[1]].set(initialSelectedEdgeEndVertexPositions.get(i)).add(movementDeltaLocal);
        }
      }
    } else if (currentTransformationState == TransformationState.FACE_MOVE && !selectedFaces.isEmpty()) {
      int[] faceVtxIndices = obj.getIndices();
      GameObject currentSelectedObjectForComponents = obj; // This is the same object
      for(int i=0; i<selectedFaces.size(); ++i) {
        int faceIdx = selectedFaces.get(i);
        int baseFaceVtxIdx = faceIdx * 3;
        if (baseFaceVtxIdx + 2 < faceVtxIndices.length && i < initialSelectedFaceVerticesLocal.size()) {
          Vector3f[] initialVtxPos = initialSelectedFaceVerticesLocal.get(i);
          for(int j=0; j<3; ++j) {
            int meshVertexIndex = faceVtxIndices[baseFaceVtxIdx + j];
            if (meshVertexIndex < currentSelectedObjectForComponents.getVertices().length) {
              obj.getVertices()[meshVertexIndex].set(initialVtxPos[j]).add(movementDeltaLocal);
            }
          }
        }
      }
    }
    // After modifying vertices, the object's mesh needs to be updated or re-uploaded to the GPU if using VBOs.
    // In this fixed-function pipeline with immediate mode or display lists (assuming ShapeRenderer uses something like that),
    // rendering the object again with the modified vertices should be sufficient.
  }


  private int pickObject(double mouseX, double mouseY) {
    int pickedIndex = -1; float closestDistance = Float.MAX_VALUE;
    Vector3f[] ray = getRayFromScreen(mouseX, mouseY);
    if (ray == null) return -1;
    Vector3f rayOrigin = ray[0]; Vector3f rayDirection = ray[1];

    // Prioritize picking the last selected object first for component selection context
    int prioritizedObjectIndex = (!selectedObjects.isEmpty() && lastSelectedObjectIndex != -1 && lastSelectedObjectIndex < objects.size()) ? lastSelectedObjectIndex : -1;

    if (prioritizedObjectIndex != -1) {
      GameObject prioritizedObj = objects.get(prioritizedObjectIndex);
      Matrix4f objTransform = getObjectTransformMatrix(prioritizedObj);
      int[] indices = prioritizedObj.getIndices(); Vector3f[] vertices = prioritizedObj.getVertices();
      // Iterate through triangles of the prioritized object for precise picking
      for (int i = 0; i < indices.length; i += 3) {
        // Ensure indices are within bounds
        if (i + 2 >= indices.length || indices[i] >= vertices.length || indices[i+1] >= vertices.length || indices[i+2] >= vertices.length) continue;
        // Get world coordinates of the triangle vertices
        Vector3f v0 = new Vector3f(vertices[indices[i]]).mulPosition(objTransform);
        Vector3f v1 = new Vector3f(vertices[indices[i+1]]).mulPosition(objTransform);
        Vector3f v2 = new Vector3f(vertices[indices[i+2]]).mulPosition(objTransform);
        // Intersect ray with triangle
        float dist = Intersectionf.intersectRayTriangle(rayOrigin, rayDirection, v0, v1, v2, 1e-5f);
        // If intersection occurs and is closer than the current closest
        if (dist >= 0 && dist < closestDistance) {
          closestDistance = dist;
          pickedIndex = prioritizedObjectIndex;
        }
      }
      // If the prioritized object was hit, return its index immediately
      if(pickedIndex != -1) return pickedIndex;
    }

    // If the prioritized object was not hit, or no object was prioritized, check all objects using bounding spheres
    closestDistance = Float.MAX_VALUE; pickedIndex = -1; // Reset for general object picking
    for (int i = 0; i < objects.size(); i++) {
      // Skip the prioritized object if it was already checked
      if (i == prioritizedObjectIndex) continue;

      GameObject obj = objects.get(i);
      Vector3f sphereCenter = obj.getPosition();
      float sphereRadius;
      float maxScale = Math.max(obj.getScale().x, Math.max(obj.getScale().y, obj.getScale().z));

      // Estimate a bounding sphere radius based on shape type and scale
      if (obj.getShapeType() == GameObject.SHAPE_CUBE) {
        // For a cube, use half the diagonal length as a rough estimate
        sphereRadius = 0.5f * (float) Math.sqrt(obj.getScale().x*obj.getScale().x + obj.getScale().y*obj.getScale().y + obj.getScale().z*obj.getScale().z) * 1.05f; // Add tolerance
      } else if (obj.getShapeType() == GameObject.SHAPE_SPHERE) {
        // For a sphere, use half the maximum scale as the radius
        sphereRadius = 0.5f * maxScale * 1.05f; // Add tolerance
      } else {
        // Default to half the maximum scale for other shapes
        sphereRadius = 0.5f * maxScale * 1.05f; // Add tolerance
      }
      // Ensure a minimum radius to make very small objects pickable
      sphereRadius = Math.max(sphereRadius, 0.05f);

      Vector2f result = new Vector2f(); // JOML stores the near/far intersection distances here
      // Intersect ray with the object's bounding sphere
      if (Intersectionf.intersectRaySphere(rayOrigin, rayDirection, sphereCenter, sphereRadius * sphereRadius, result)) {
        // Get the closest intersection point in front of the ray origin
        float t = (result.x < 0) ? result.y : result.x;
        // If intersection occurs in front of the origin and is closer than the current closest
        if (t >= 0 && t < closestDistance) {
          closestDistance = t;
          pickedIndex = i;
        }
      }
    }
    return pickedIndex;
  }

  private int pickGlobalAxis(double mouseX, double mouseY) {
    float pickTolerance = 15.0f; // Tolerance in screen pixels
    // Get current OpenGL state for projection
    glGetIntegerv(GL_VIEWPORT, viewport);
    glGetDoublev(GL_MODELVIEW_MATRIX, modelviewMatrixDoubleBuffer);
    glGetDoublev(GL_PROJECTION_MATRIX, projectionMatrixDoubleBuffer);

    // Convert double matrices to float matrices for JOML
    for (int i = 0; i < 16; i++) {
      modelviewMatrixFloatBuffer[i] = (float)modelviewMatrixDoubleBuffer[i];
      projectionMatrixFloatBuffer[i] = (float)projectionMatrixDoubleBuffer[i];
    }
    Matrix4f currentModelview = new Matrix4f().set(modelviewMatrixFloatBuffer);
    Matrix4f currentProjection = new Matrix4f().set(projectionMatrixFloatBuffer);

    // Invert mouse Y coordinate for OpenGL's bottom-left origin
    double invertedMouseY = viewport[3] - mouseY;

    float axisRenderLength = GRID_LINES * GRID_SPACING; // Length of the rendered axes

    // Define the start and end points of the global axes in world space
    Vector3f origin = new Vector3f(0,0,0);
    Vector3f[] axisEnds = {
        new Vector3f(axisRenderLength, 0, 0), // +X
        new Vector3f(0, axisRenderLength, 0), // +Y
        new Vector3f(0, 0, axisRenderLength), // +Z
        new Vector3f(-axisRenderLength, 0, 0), // -X
        new Vector3f(0, -axisRenderLength, 0), // -Y
        new Vector3f(0, 0, -axisRenderLength)  // -Z
    };

    // Project the world origin to screen space
    Vector3f originScreen = projectWorldToScreen(origin, currentModelview, currentProjection, viewport);

    // If the origin is not on screen or projection fails, cannot pick axes
    if (originScreen == null || Float.isNaN(originScreen.x)) return 0;

    // Check positive axes
    for(int i=0; i<3; ++i) {
      Vector3f axisEndScreenPos = projectWorldToScreen(axisEnds[i], currentModelview, currentProjection, viewport);
      // Check if the axis end is on screen and projection is valid
      if (axisEndScreenPos != null && !Float.isNaN(axisEndScreenPos.x)) {
        // Check distance from mouse to the line segment in screen space
        if (distanceToLineSegment(mouseX, invertedMouseY, originScreen.x, originScreen.y, axisEndScreenPos.x, axisEndScreenPos.y) < pickTolerance) {
          return i + 1; // Return 1 for X, 2 for Y, 3 for Z
        }
      }
    }

    // Check negative axes
    for(int i=0; i<3; ++i) {
      Vector3f axisEndScreenNeg = projectWorldToScreen(axisEnds[i+3], currentModelview, currentProjection, viewport);
      if (axisEndScreenNeg != null && !Float.isNaN(axisEndScreenNeg.x)) {
        if (distanceToLineSegment(mouseX, invertedMouseY, originScreen.x, originScreen.y, axisEndScreenNeg.x, axisEndScreenNeg.y) < pickTolerance) {
          return i + 1; // Return 1 for -X, 2 for -Y, 3 for -Z (using the same axis index as positive)
        }
      }
    }

    return 0; // No axis picked
  }

  private List<Integer> pickVertices(double mouseX, double mouseY, GameObject obj) {
    List<Integer> pickedVertices = new ArrayList<>();
    if (obj == null || obj.getVertices() == null) return pickedVertices;
    float pickRadiusSq = 15.0f * 15.0f; // Picking tolerance squared in screen pixels
    Matrix4f objTransform = getObjectTransformMatrix(obj);

    // Get current OpenGL state for projection
    Matrix4f currentModelview = new Matrix4f(); Matrix4f currentProjection = new Matrix4f();
    glGetDoublev(GL_MODELVIEW_MATRIX, modelviewMatrixDoubleBuffer);
    glGetDoublev(GL_PROJECTION_MATRIX, projectionMatrixDoubleBuffer);
    glGetIntegerv(GL_VIEWPORT, viewport);

    // Convert double matrices to float matrices for JOML
    for (int i = 0; i < 16; i++) {
      modelviewMatrixFloatBuffer[i] = (float)modelviewMatrixDoubleBuffer[i];
      projectionMatrixFloatBuffer[i] = (float)projectionMatrixDoubleBuffer[i];
    }
    currentModelview.set(modelviewMatrixFloatBuffer);
    currentProjection.set(projectionMatrixFloatBuffer);

    // Combine modelview and object transform for world-to-view-to-object space
    Matrix4f fullTransform = new Matrix4f(currentModelview).mul(objTransform);

    // Invert mouse Y coordinate for OpenGL's bottom-left origin
    double invMouseY = viewport[3] - mouseY;

    List<PickedComponent<Integer>> potentialPicks = new ArrayList<>();

    // Iterate through all vertices of the object
    for (int i = 0; i < obj.getVertices().length; i++) {
      Vector3f vertexLocal = obj.getVertices()[i];
      // Project the vertex from local space to screen space
      Vector3f screenPos = projectWorldToScreen(vertexLocal, fullTransform, currentProjection, viewport);

      // Check if the projected point is valid and within the viewport's depth range
      if (screenPos != null && !Float.isNaN(screenPos.x) && screenPos.z >= 0 && screenPos.z <= 1) {
        // Calculate squared distance from mouse cursor to the projected vertex
        double dx = mouseX - screenPos.x;
        double dy = invMouseY - screenPos.y;
        double distSq = dx * dx + dy * dy;

        // If the distance is within the pick radius, consider it a potential pick
        if (distSq < pickRadiusSq) {
          potentialPicks.add(new PickedComponent<>(i, distSq));
        }
      }
    }
    // Sort potential picks by distance (closest first)
    Collections.sort(potentialPicks);
    // Add the component indices of the sorted picks to the result list
    for(PickedComponent<Integer> pick : potentialPicks) {
      pickedVertices.add(pick.component);
    }
    return pickedVertices;
  }

  private List<int[]> pickEdges(double mouseX, double mouseY, GameObject obj) {
    List<int[]> pickedEdges = new ArrayList<>();
    if (obj == null || obj.getVertices() == null || obj.getIndices() == null) return pickedEdges;
    float pickTolerance = 10.0f; // Picking tolerance in screen pixels
    Matrix4f objTransform = getObjectTransformMatrix(obj);

    // Get current OpenGL state for projection
    Matrix4f currentModelview = new Matrix4f(); Matrix4f currentProjection = new Matrix4f();
    glGetDoublev(GL_MODELVIEW_MATRIX, modelviewMatrixDoubleBuffer);
    glGetDoublev(GL_PROJECTION_MATRIX, projectionMatrixDoubleBuffer);
    glGetIntegerv(GL_VIEWPORT, viewport);

    // Convert double matrices to float matrices for JOML
    for (int i = 0; i < 16; i++) {
      modelviewMatrixFloatBuffer[i] = (float)modelviewMatrixDoubleBuffer[i];
      projectionMatrixFloatBuffer[i] = (float)projectionMatrixDoubleBuffer[i];
    }
    currentModelview.set(modelviewMatrixFloatBuffer);
    currentProjection.set(projectionMatrixFloatBuffer);

    // Combine modelview and object transform
    Matrix4f fullTransform = new Matrix4f(currentModelview).mul(objTransform);

    // Invert mouse Y coordinate for OpenGL's bottom-left origin
    double invMouseY = viewport[3] - mouseY;

    List<int[]> edgesToTest = new ArrayList<>();
    Set<List<Integer>> processed = new HashSet<>(); // To avoid processing the same edge twice

    // Collect unique edges from the object's indices
    if (obj.getShapeType() == GameObject.SHAPE_CUBE) {
      // Hardcoded edges for a cube (assuming a specific vertex order)
      int[][] cubeEdges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
      for(int[] e : cubeEdges) edgesToTest.add(e);
    } else {
      // Extract edges from triangle indices
      for(int i=0; i<obj.getIndices().length; i+=3){
        for(int j=0; j<3; ++j){
          int v1 = obj.getIndices()[i+j];
          int v2 = obj.getIndices()[i+(j+1)%3];
          // Create a canonical representation of the edge (sorted vertex indices)
          List<Integer> edgeKey = Arrays.asList(Math.min(v1, v2), Math.max(v1, v2));
          // Add the edge to the list if not already processed
          if(processed.add(edgeKey)) {
            edgesToTest.add(new int[]{v1, v2});
          }
        }
      }
    }

    List<PickedComponent<int[]>> potentialPicks = new ArrayList<>();

    // Iterate through all edges to test for picking
    for (int[] edgeVertIndices : edgesToTest) {
      // Ensure vertex indices are within bounds
      if (edgeVertIndices[0] >= obj.getVertices().length || edgeVertIndices[1] >= obj.getVertices().length) continue;

      Vector3f v1 = obj.getVertices()[edgeVertIndices[0]];
      Vector3f v2 = obj.getVertices()[edgeVertIndices[1]];

      // Project the edge vertices to screen space
      Vector3f sPos1 = projectWorldToScreen(v1, fullTransform, currentProjection, viewport);
      Vector3f sPos2 = projectWorldToScreen(v2, fullTransform, currentProjection, viewport);

      // Check if both projected points are valid and within the viewport's depth range
      if (sPos1 != null && sPos2 != null && !Float.isNaN(sPos1.x) && !Float.isNaN(sPos2.x) &&
          sPos1.z >= 0 && sPos1.z <= 1 && sPos2.z >= 0 && sPos2.z <= 1) {
        // Calculate the distance from the mouse cursor to the screen space line segment
        double dist = distanceToLineSegment(mouseX, invMouseY, sPos1.x, sPos1.y, sPos2.x, sPos2.y);

        // If the distance is within the pick tolerance, consider it a potential pick
        if (dist < pickTolerance) {
          potentialPicks.add(new PickedComponent<>(edgeVertIndices, dist));
        }
      }
    }
    // Sort potential picks by distance (closest first)
    Collections.sort(potentialPicks, (a, b) -> Double.compare(a.sortValue, b.sortValue)); // Explicit Comparator for clarity
    // Add the component indices of the sorted picks to the result list
    for(PickedComponent<int[]> pick : potentialPicks) {
      pickedEdges.add(pick.component);
    }
    return pickedEdges;
  }

  private List<Integer> pickFaces(double mouseX, double mouseY, GameObject obj) {
    List<Integer> pickedFaces = new ArrayList<>();
    if (obj == null || obj.getVertices() == null || obj.getIndices() == null) return pickedFaces;

    Vector3f[] ray = getRayFromScreen(mouseX, mouseY);
    if (ray == null) return pickedFaces;
    Vector3f rayOrigin = ray[0];
    Vector3f rayDirection = ray[1];

    Matrix4f objTransform = getObjectTransformMatrix(obj);

    List<PickedComponent<Integer>> potentialPicks = new ArrayList<>();

    // Get the inverse of the current view matrix to find the camera's world position
    Matrix4f invView = new Matrix4f();
    glGetDoublev(GL_MODELVIEW_MATRIX, modelviewMatrixDoubleBuffer);
    for (int i = 0; i < 16; i++) modelviewMatrixFloatBuffer[i] = (float)modelviewMatrixDoubleBuffer[i];
    new Matrix4f().set(modelviewMatrixFloatBuffer).invert(invView);
    Vector3f cameraPositionWorld = invView.transformPosition(new Vector3f(0, 0, 0)); // Camera is at (0,0,0) in view space

    // Iterate through all triangles (faces) of the object
    for (int i = 0; i < obj.getIndices().length; i += 3) {
      // Ensure indices are within bounds
      if (i + 2 >= obj.getIndices().length || obj.getIndices()[i] >= obj.getVertices().length ||
          obj.getIndices()[i+1] >= obj.getVertices().length || obj.getIndices()[i+2] >= obj.getVertices().length) continue;

      // Get world coordinates of the triangle vertices
      Vector3f v0 = new Vector3f(obj.getVertices()[obj.getIndices()[i]]).mulPosition(objTransform);
      Vector3f v1 = new Vector3f(obj.getVertices()[obj.getIndices()[i+1]]).mulPosition(objTransform);
      Vector3f v2 = new Vector3f(obj.getVertices()[obj.getIndices()[i+2]]).mulPosition(objTransform);

      // Calculate the face normal in world space
      Vector3f normal = new Vector3f(v1).sub(v0).cross(new Vector3f(v2).sub(v0)).normalize();

      // Calculate the face centroid in world space
      Vector3f faceCentroidWorld = new Vector3f(v0).add(v1).add(v2).div(3.0f);

      // Calculate the direction from the camera to the face centroid
      Vector3f viewDirection = new Vector3f(cameraPositionWorld).sub(faceCentroidWorld).normalize();

      // Check if the face is facing towards the camera (dot product > 0)
      // This helps prevent picking back-facing faces
      if (normal.dot(viewDirection) > 0) {
        // Intersect ray with triangle
        float dist = Intersectionf.intersectRayTriangle(rayOrigin, rayDirection, v0, v1, v2, 1e-5f);

        // If intersection occurs and is in front of the ray origin
        if (dist >= 0) {
          potentialPicks.add(new PickedComponent<>(i / 3, dist)); // Store face index and distance
        }
      }
    }
    // Sort potential picks by distance (closest first)
    Collections.sort(potentialPicks, (a, b) -> Double.compare(a.sortValue, b.sortValue)); // Explicit Comparator for clarity
    // Add the component indices of the sorted picks to the result list
    for(PickedComponent<Integer> pick : potentialPicks) {
      pickedFaces.add(pick.component);
    }
    return pickedFaces;
  }

  // Helper class for storing picked components with their distance for sorting
  private static class PickedComponent<T> implements Comparable<PickedComponent<T>> {
    T component; // The picked component (e.g., vertex index, edge array, face index)
    double sortValue; // The value used for sorting (e.g., distance from ray origin)

    PickedComponent(T component, double sortValue) {
      this.component = component;
      this.sortValue = sortValue;
    }

    @Override
    public int compareTo(PickedComponent<T> other) {
      // Compare based on the sortValue (distance)
      return Double.compare(this.sortValue, other.sortValue);
    }
  }


  // Projects a world coordinate to screen coordinates
  private Vector3f projectWorldToScreen(Vector3f worldPos, Matrix4f modelView, Matrix4f projection, int[] viewport) {
    // Transform the world coordinate to clip space
    Vector4f clipSpacePos = new Vector4f(worldPos, 1.0f).mul(modelView).mul(projection);

    // Perform perspective division
    if (clipSpacePos.w == 0.0f) {
      return null; // Cannot project if w is zero
    }
    clipSpacePos.div(clipSpacePos.w);

    // Transform to screen coordinates
    float winX = (clipSpacePos.x + 1.0f) / 2.0f * viewport[2] + viewport[0];
    float winY = (clipSpacePos.y + 1.0f) / 2.0f * viewport[3] + viewport[1]; // Invert Y for screen coordinates (top-left is 0,0)
    float winZ = (clipSpacePos.z + 1.0f) / 2.0f; // Depth value between 0 and 1

    return new Vector3f(winX, winY, winZ);
  }

  // Generates a ray from the camera through the given screen coordinates
  private Vector3f[] getRayFromScreen(double screenX, double screenY) {
    // Get current OpenGL matrices and viewport
    glGetDoublev(GL_MODELVIEW_MATRIX, modelviewMatrixDoubleBuffer);
    glGetDoublev(GL_PROJECTION_MATRIX, projectionMatrixDoubleBuffer);
    glGetIntegerv(GL_VIEWPORT, viewport);

    // Convert double matrices to float matrices for JOML
    for (int i = 0; i < 16; i++) {
      modelviewMatrixFloatBuffer[i] = (float)modelviewMatrixDoubleBuffer[i];
      projectionMatrixFloatBuffer[i] = (float)projectionMatrixDoubleBuffer[i];
    }
    Matrix4f currentModelview = new Matrix4f().set(modelviewMatrixFloatBuffer);
    Matrix4f currentProjection = new Matrix4f().set(projectionMatrixFloatBuffer);

    // Create the inverse of the combined View-Projection matrix
    Matrix4f invVP = new Matrix4f(currentProjection).mul(currentModelview).invert();

    // Check if the inverse is valid
    if (invVP.determinant() == 0) {
      return null; // Cannot generate ray if matrix is not invertible
    }

    // Convert screen coordinates to Normalized Device Coordinates (NDC)
    float ndcX = (float) (2.0 * screenX) / viewport[2] - 1.0f;
    float ndcY = (float) (1.0 - (2.0 * screenY) / viewport[3]); // Invert Y for NDC (bottom-left is -1,-1)

    // Create points at the near and far planes in NDC space
    Vector4f near = new Vector4f(ndcX, ndcY, -1.0f, 1.0f);
    Vector4f far  = new Vector4f(ndcX, ndcY,  1.0f, 1.0f);

    // Transform the NDC points to world space
    near = invVP.transform(near);
    far  = invVP.transform(far);

    // Perform perspective division for world coordinates
    if (near.w == 0.0f || far.w == 0.0f) {
      return null; // Cannot generate ray if w is zero after transformation
    }
    near.div(near.w);
    far.div(far.w);

    // The ray origin is the camera position (which is the 'near' point in world space)
    Vector3f origin = new Vector3f(near.x, near.y, near.z);
    // The ray direction is the vector from the near point to the far point, normalized
    Vector3f dir = new Vector3f(far.x - near.x, far.y - near.y, far.z - near.z).normalize();

    return new Vector3f[]{origin, dir}; // Return ray origin and direction
  }

  // Calculates the shortest distance from a point to a line segment in 2D
  private double distanceToLineSegment(double px, double py, double x1, double y1, double x2, double y2) {
    double l2 = (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2); // Squared length of the line segment
    if (l2 == 0) { // If the segment is a point
      return Math.sqrt((px-x1)*(px-x1) + (py-y1)*(py-y1)); // Distance to the point
    }
    // Project the point onto the line defined by the segment
    double t = ((px-x1)*(x2-x1) + (py-y1)*(y2-y1)) / l2;
    // Clamp t to the range [0, 1] to find the closest point on the segment
    t = Math.max(0, Math.min(1, t));
    // Calculate the coordinates of the closest point on the segment
    double closestX = x1 + t*(x2-x1);
    double closestY = y1 + t*(y2-y1);
    // Return the distance from the point to the closest point on the segment
    return Math.sqrt(Math.pow(px - closestX, 2) + Math.pow(py - closestY, 2));
  }

  private void duplicateSelectedObject(double mouseX, double mouseY) {
    if (selectedObjects.isEmpty()) return;

    // Calculate the centroid of the currently selected objects
    Vector3f originalSelectionCentroid = getSelectionCentroid();

    // Generate a ray from the mouse position
    Vector3f[] ray = getRayFromScreen(mouseX, mouseY);
    Vector3f baseIntersectionPoint = new Vector3f(0, 0, 0); // Default intersection point (origin)

    if (ray != null) {
      Vector3f rayOrigin = ray[0];
      Vector3f rayDirection = ray[1];

      // Intersect the ray with the Y=0 plane (the grid plane)
      // The equation of the plane is y = 0. A point on the ray is Origin + t * Direction.
      // We want the point where the y-component is 0: Origin.y + t * Direction.y = 0
      // So, t = -Origin.y / Direction.y
      if (Math.abs(rayDirection.y) > 1e-6) { // Avoid division by zero if ray is parallel to the plane
        float t = -rayOrigin.y / rayDirection.y;
        if (t >= 0) { // Ensure the intersection is in front of the ray origin
          baseIntersectionPoint = new Vector3f(rayOrigin).add(new Vector3f(rayDirection).mul(t));
        } else {
          // If intersection is behind, use a point on the ray at a fixed distance and set Y to 0
          baseIntersectionPoint = new Vector3f(rayOrigin).add(new Vector3f(rayDirection).mul(cameraDistance * 0.5f));
          baseIntersectionPoint.y = 0;
        }
      } else {
        // If ray is parallel to the plane, use a point on the ray at a fixed distance and set Y to 0
        baseIntersectionPoint = new Vector3f(rayOrigin).add(new Vector3f(rayDirection).mul(cameraDistance * 0.5f));
        baseIntersectionPoint.y = 0;
      }
    }

    // Store the original selection and clear it
    List<Integer> originalSelectedIndices = new ArrayList<>(selectedObjects);
    selectedObjects.clear();
    int originalLastSelectedObjectIndex = lastSelectedObjectIndex;
    lastSelectedObjectIndex = -1;

    // Store original component selections for the last selected object
    GameObject originalObjectWithComponentSelection = null;
    List<Integer> originalSelectedVertices = new ArrayList<>(selectedVertices);
    List<int[]> originalSelectedEdges = new ArrayList<>(selectedEdges);
    List<Integer> originalSelectedFaces = new ArrayList<>(selectedFaces);

    // Find the original object that had component selections (if any)
    if (!originalSelectedIndices.isEmpty() && originalSelectedIndices.contains(originalLastSelectedObjectIndex) && originalLastSelectedObjectIndex < objects.size()) {
      originalObjectWithComponentSelection = objects.get(originalLastSelectedObjectIndex);
    }

    // Clear component selections before duplicating
    selectedVertices.clear();
    selectedEdges.clear();
    selectedFaces.clear();

    List<GameObject> newDuplicates = new ArrayList<>();
    List<Integer> newDuplicateIndices = new ArrayList<>();

    // Duplicate each selected object
    for(int objIdx : originalSelectedIndices) {
      if (objIdx < objects.size()) {
        GameObject selectedObj = objects.get(objIdx);
        // Create a new GameObject with copied data
        GameObject duplicate = new GameObject(selectedObj.getShapeType(), selectedObj.getName() + " (Copy)",
            Arrays.copyOf(selectedObj.getVertices(), selectedObj.getVertices().length), // Deep copy vertices
            Arrays.copyOf(selectedObj.getIndices(), selectedObj.getIndices().length)); // Deep copy indices

        // Calculate the offset of the original object from the original selection centroid
        Vector3f originalOffsetFromCentroid = new Vector3f(selectedObj.getPosition()).sub(originalSelectionCentroid);

        // Position the duplicate relative to the base intersection point on the grid, maintaining the original offset
        duplicate.getPosition().set(baseIntersectionPoint).add(originalOffsetFromCentroid);

        // Copy rotation and scale
        duplicate.getRotationAngles().set(selectedObj.getRotationAngles());
        duplicate.getScale().set(selectedObj.getScale());

        newDuplicates.add(duplicate);
      }
    }

    // Add the new duplicates to the main objects list and select them
    int firstNewObjectIndex = objects.size();
    objects.addAll(newDuplicates);

    for(int i = 0; i < newDuplicates.size(); ++i) {
      int newIndex = firstNewObjectIndex + i;
      selectedObjects.add(newIndex);
      newDuplicateIndices.add(newIndex);
    }

    // Restore component selection for the corresponding duplicate object
    if (originalObjectWithComponentSelection != null) {
      int originalIndexInList = originalSelectedIndices.indexOf(originalLastSelectedObjectIndex);
      int duplicateIndex = -1;
      if (originalIndexInList != -1 && originalIndexInList < newDuplicateIndices.size()) {
        duplicateIndex = newDuplicateIndices.get(originalIndexInList);
      }

      if (duplicateIndex != -1 && duplicateIndex < objects.size()) {
        lastSelectedObjectIndex = duplicateIndex;
        // Restore component selections to the duplicate
        selectedVertices.addAll(originalSelectedVertices);
        selectedEdges.addAll(originalSelectedEdges);
        selectedFaces.addAll(originalSelectedFaces);
      } else {
        // If the corresponding duplicate wasn't found, clear component selections
        selectedVertices.clear();
        selectedEdges.clear();
        selectedFaces.clear();
        // Set lastSelectedObjectIndex to the last added object if available
        if (!selectedObjects.isEmpty()) lastSelectedObjectIndex = selectedObjects.get(selectedObjects.size() - 1);
        else lastSelectedObjectIndex = -1;
      }
    } else {
      // If no component selection was active, just set lastSelectedObjectIndex to the last added object
      if (!selectedObjects.isEmpty()) lastSelectedObjectIndex = selectedObjects.get(selectedObjects.size() - 1);
      else lastSelectedObjectIndex = -1;
    }

    System.out.println("Duplicated " + newDuplicates.size() + " objects.");
  }

  private Vector3f getSelectionCentroid() {
    Vector3f centroid = new Vector3f(0,0,0);
    if (selectedObjects.isEmpty()) return centroid;
    for(int objIdx : selectedObjects) {
      if (objIdx < objects.size()) {
        centroid.add(objects.get(objIdx).getPosition());
      }
    }
    centroid.div(selectedObjects.size());
    return centroid;
  }


  // Custom perspective projection function
  private void gluPerspective(float fovY, float aspect, float zNear, float zFar) {
    float f = (float)Math.tan(Math.toRadians(fovY) / 2.0);
    GL11.glFrustum(-f * zNear * aspect, f * zNear * aspect, -f * zNear, f * zNear, zNear, zFar);
  }

  // Render the grid on the XZ plane
  private void renderGrid() {
    float half = GRID_LINES * GRID_SPACING;
    GL11.glLineWidth(1.0f);
    GL11.glColor3f(0.3f, 0.3f, 0.3f); // Grey color for grid lines
    GL11.glBegin(GL_LINES);
    // Draw lines parallel to the Z-axis
    for (int i = -GRID_LINES; i <= GRID_LINES; i++) {
      float p = i * GRID_SPACING;
      GL11.glVertex3f(p, 0, -half);
      GL11.glVertex3f(p, 0, half);
    }
    // Draw lines parallel to the X-axis
    for (int i = -GRID_LINES; i <= GRID_LINES; i++) {
      float p = i * GRID_SPACING;
      GL11.glVertex3f(-half, 0, p);
      GL11.glVertex3f(half, 0, p);
    }
    GL11.glEnd();
  }

  // Render the global axes (X, Y, Z)
  private void renderAxes() {
    float len = GRID_LINES * GRID_SPACING; // Length of the axes, matching grid size
    GL11.glLineWidth(2.0f);
    GL11.glBegin(GL_LINES);

    // X-axis (Red) - Highlight if translating along X
    GL11.glColor3f(translatingGlobalAxis == 1 ? 1.0f : 1.0f, translatingGlobalAxis == 1 ? 1.0f : 0.0f, 0.0f);
    GL11.glVertex3f(-len,0,0); // Draw negative X axis
    GL11.glVertex3f(len,0,0);  // Draw positive X axis

    // Y-axis (Green) - Highlight if translating along Y
    GL11.glColor3f(translatingGlobalAxis == 2 ? 1.0f : 0.0f, translatingGlobalAxis == 2 ? 1.0f : 1.0f, 0.0f);
    GL11.glVertex3f(0,-len,0); // Draw negative Y axis
    GL11.glVertex3f(0,len,0);  // Draw positive Y axis

    // Z-axis (Blue) - Highlight if translating along Z
    GL11.glColor3f(0.0f, translatingGlobalAxis == 3 ? 1.0f : 0.0f, translatingGlobalAxis == 3 ? 1.0f : 1.0f);
    GL11.glVertex3f(0,0,-len); // Draw negative Z axis
    GL11.glVertex3f(0,0,len);  // Draw positive Z axis

    GL11.glEnd();
  }

  // Render mini-axes in the corner of the viewport
  private void renderMiniAxes(int w, int h, Vector3f camRot) {
    if (w <= 0 || h <= 0) return;
    int size = Math.min(w/10, h/10); // Size of the mini-axes viewport
    size = Math.max(size,40); // Minimum size
    size = Math.min(size,100); // Maximum size
    int padding = 10; // Padding from the corner

    GL11.glPushAttrib(GL_ALL_ATTRIB_BITS); // Save current OpenGL attributes
    // Set viewport for mini-axes (bottom-right corner) relative to the main window size
    GL11.glViewport(w-size-padding, h-size-padding, size, size);

    GL11.glMatrixMode(GL_PROJECTION); GL11.glPushMatrix(); GL11.glLoadIdentity();
    GL11.glOrtho(-1.5,1.5,-1.5,1.5,-10,10); // Orthographic projection for the mini-axes

    GL11.glMatrixMode(GL_MODELVIEW); GL11.glPushMatrix(); GL11.glLoadIdentity();
    GL11.glClear(GL_DEPTH_BUFFER_BIT); // Clear depth buffer for mini-axes to render on top
    GL11.glDisable(GL_LIGHTING); // Disable lighting for simple colored lines

    // Apply camera rotation to the mini-axes
    GL11.glRotatef(camRot.x,1,0,0); // Pitch
    GL11.glRotatef(camRot.y,0,1,0); // Yaw
    // No roll (camRot.z) for standard camera

    GL11.glLineWidth(2f); // Line width for axes
    GL11.glBegin(GL_LINES);
    GL11.glColor3f(1,0,0); GL11.glVertex3f(0,0,0); GL11.glVertex3f(1,0,0); // X-axis (Red)
    GL11.glColor3f(0,1,0); GL11.glVertex3f(0,0,0); GL11.glVertex3f(0,1,0); // Y-axis (Green)
    GL11.glColor3f(0,0,1); GL11.glVertex3f(0,0,0); GL11.glVertex3f(0,0,1); // Z-axis (Blue)
    GL11.glEnd();

    GL11.glMatrixMode(GL_PROJECTION); GL11.glPopMatrix(); // Restore previous projection matrix
    GL11.glMatrixMode(GL_MODELVIEW); GL11.glPopMatrix(); // Restore previous modelview matrix
    GL11.glPopAttrib(); // Restore saved OpenGL attributes

    // The main viewport will be set again at the start of the next render cycle.
  }

  // Render selected vertices, edges, and faces for the given object
  private void renderSelectedObjectComponents(GameObject obj) {
    if (obj == null || obj.getVertices() == null || obj.getIndices() == null) return;

    GL11.glPushAttrib(GL_ALL_ATTRIB_BITS); // Save current OpenGL attributes
    GL11.glDisable(GL_DEPTH_TEST); // Disable depth test to ensure components are visible
    GL11.glDisable(GL_LIGHTING); // Disable lighting for component rendering

    Vector3f[] verts = obj.getVertices();
    int[] inds = obj.getIndices();

    // Render selected faces with transparency
    if (!selectedFaces.isEmpty()) {
      GL11.glEnable(GL_BLEND); // Enable blending for transparency
      GL11.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); // Standard alpha blending
      GL11.glColor4f(1,1,0,0.3f); // Yellow with transparency
      GL11.glBegin(GL_TRIANGLES);
      for(int faceIndex : selectedFaces) {
        int base = faceIndex*3;
        // Ensure indices are within bounds
        if(base+2 < inds.length && inds[base] < verts.length && inds[base+1] < verts.length && inds[base+2] < verts.length) {
          GL11.glVertex3fv(toFloatBuffer(verts[inds[base]]));
          GL11.glVertex3fv(toFloatBuffer(verts[inds[base+1]]));
          GL11.glVertex3fv(toFloatBuffer(verts[inds[base+2]]));
        }
      }
      GL11.glEnd();
      GL11.glDisable(GL_BLEND); // Disable blending
    }

    // Render edges
    if (currentSelectionMode != SelectionMode.OBJECT) { // Render edges if not in object mode
      GL11.glLineWidth(1.5f); // Default line width
      GL11.glBegin(GL_LINES);
      Set<List<Integer>> drawnEdges = new HashSet<>(); // To avoid drawing duplicate edges

      // Get edges based on shape type
      if (obj.getShapeType() == GameObject.SHAPE_CUBE) {
        int[][] cubeEdges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        for(int[] edge : cubeEdges) {
          int v1 = edge[0]; int v2 = edge[1];
          if (v1 >= verts.length || v2 >= verts.length) continue; // Ensure vertex indices are valid

          boolean isSelected = false;
          // Check if the current edge is in the list of selected edges
          for(int[] selectedEdge : selectedEdges) {
            if ((selectedEdge[0] == v1 && selectedEdge[1] == v2) || (selectedEdge[0] == v2 && selectedEdge[1] == v1)) {
              isSelected = true;
              break;
            }
          }

          if (isSelected) { GL11.glColor3f(1,1,0); GL11.glLineWidth(3f); } // Yellow and thicker if selected
          else { GL11.glColor3f(0.7f,0.7f,0.7f); GL11.glLineWidth(1.5f); } // Grey and default thickness if not selected

          GL11.glVertex3fv(toFloatBuffer(verts[v1]));
          GL11.glVertex3fv(toFloatBuffer(verts[v2]));
        }
      } else {
        // Extract and draw edges from triangle indices for non-cube shapes
        for(int i=0; i<inds.length; i+=3){
          for(int j=0; j<3; ++j){
            int v1 = inds[i+j];
            int v2 = inds[i+(j+1)%3];
            if (v1 >= verts.length || v2 >= verts.length) continue; // Ensure vertex indices are valid

            List<Integer> edgeKey = Arrays.asList(Math.min(v1, v2), Math.max(v1, v2));
            // Only draw if this edge hasn't been drawn yet
            if (drawnEdges.add(edgeKey)) {
              boolean isSelected = false;
              // Check if the current edge is in the list of selected edges
              for(int[] selectedEdge : selectedEdges) {
                if ((selectedEdge[0] == v1 && selectedEdge[1] == v2) || (selectedEdge[0] == v2 && selectedEdge[1] == v1)) {
                  isSelected = true;
                  break;
                }
              }

              if (isSelected) { GL11.glColor3f(1,1,0); GL11.glLineWidth(3f); } // Yellow and thicker if selected
              else { GL11.glColor3f(0.7f,0.7f,0.7f); GL11.glLineWidth(1.5f); } // Grey and default thickness if not selected

              GL11.glVertex3fv(toFloatBuffer(verts[v1]));
              GL11.glVertex3fv(toFloatBuffer(verts[v2]));
            }
          }
        }
      }
      GL11.glEnd();
    }

    // Render vertices
    if (currentSelectionMode != SelectionMode.OBJECT) { // Render vertices if not in object mode
      GL11.glPointSize(8f); // Default point size
      GL11.glBegin(GL_POINTS);
      for(int i=0; i<verts.length; ++i){
        boolean highlighted = false;
        // Highlight vertex if it's directly selected
        if(selectedVertices.contains(i)) {
          GL11.glColor3f(1,0.5f,0); // Orange
          GL11.glPointSize(12f); // Larger size
          highlighted=true;
        } else {
          // Highlight vertex if it's part of a selected edge
          for(int[] selectedEdge : selectedEdges) {
            if (i == selectedEdge[0] || i == selectedEdge[1]) {
              GL11.glColor3f(1,1,0); // Yellow
              GL11.glPointSize(10f); // Medium size
              highlighted = true;
              break;
            }
          }
        }
        // Highlight vertex if it's part of a selected face
        if(!highlighted) {
          for(int faceIndex : selectedFaces) {
            int base = faceIndex * 3;
            if (base + 2 < inds.length && inds[base] < verts.length && inds[base+1] < verts.length && inds[base+2] < verts.length &&
                (i == inds[base] || i == inds[base+1] || i == inds[base+2])) {
              GL11.glColor3f(1,1,0); // Yellow
              GL11.glPointSize(10f); // Medium size
              highlighted = true;
              break;
            }
          }
        }
        // Default color and size if not highlighted
        if(!highlighted) {
          GL11.glColor3f(0.2f,0.2f,0.2f); // Dark grey
          GL11.glPointSize(8f); // Default size
        }
        GL11.glVertex3fv(toFloatBuffer(verts[i]));
      }
      GL11.glEnd();
    }
    GL11.glPopAttrib(); // Restore saved OpenGL attributes
  }

  // Helper to convert a JOML Vector3f to a Java FloatBuffer
  private FloatBuffer toFloatBuffer(Vector3f vec) {
    FloatBuffer fb = BufferUtils.createFloatBuffer(3);
    fb.put(vec.x).put(vec.y).put(vec.z);
    fb.flip(); // Flip the buffer to prepare for reading
    return fb;
  }

  // Method to calculate the centroid of a face (not currently used but kept)
  private Vector3f calculateFaceCentroid(GameObject obj, int faceIndex) {
    if (obj == null || obj.getVertices() == null || obj.getIndices() == null || faceIndex < 0) return new Vector3f();
    int base = faceIndex*3;
    int[] inds = obj.getIndices();
    Vector3f[] verts = obj.getVertices();
    // Ensure indices are within bounds
    if (base+2 >= inds.length || inds[base] >= verts.length || inds[base+1] >= verts.length || inds[base+2] >= verts.length) return new Vector3f();
    // Calculate the average of the face's vertices
    return new Vector3f(verts[inds[base]]).add(verts[inds[base+1]]).add(verts[inds[base+2]]).div(3.0f);
  }

  // Cleanup method, called after the main loop exits
  private void cleanup() {
    // Cleanup ImGui resources
    if (imGuiLayer != null) {
      imGuiLayer.cleanup();
    }
    // Cleanup Window resources
    if (gameWindow != null) {
      gameWindow.cleanup();
    }
    System.out.println("Engine cleanup complete.");
  }


  // --- Methods for ImGuiLayer to interact with the engine ---

  // Add a new Cube object to the scene
  public void addCube() {
    GameObject cube = shapeRenderer.createCube("Cube" + (objects.size() + 1));
    // Default position slightly offset or at origin
    cube.getPosition().set(0, 0.5f, 0);
    objects.add(cube);
    // Select the newly added object
    selectedObjects.clear();
    selectedObjects.add(objects.size() - 1);
    lastSelectedObjectIndex = objects.size() - 1;
    saveStateForUndo(); // Save state after adding
    System.out.println("Added new Cube via UI.");
  }

  // Add a new Sphere object to the scene
  public void addSphere() {
    GameObject sphere = shapeRenderer.createSphere("Sphere" + (objects.size() + 1));
    sphere.getPosition().set(1, 0.5f, 1); // Example position
    objects.add(sphere);
    // Select the newly added object
    selectedObjects.clear();
    selectedObjects.add(objects.size() - 1);
    lastSelectedObjectIndex = objects.size() - 1;
    saveStateForUndo(); // Save state after adding
    System.out.println("Added new Sphere via UI.");
  }

  // Get the current selection mode
  public SelectionMode getCurrentSelectionMode() {
    return currentSelectionMode;
  }

  // Set the current selection mode
  public void setCurrentSelectionMode(SelectionMode mode) {
    if (this.currentSelectionMode != mode) {
      this.currentSelectionMode = mode;
      // Deselect components when changing selection mode
      selectedVertices.clear();
      selectedEdges.clear();
      selectedFaces.clear();
      isComponentPreSelected = false;
      dragging = false; // Stop any ongoing component drag
      System.out.println("Selection Mode changed to: " + mode);
    }
  }

  // Get the list of selected object indices
  public List<Integer> getSelectedObjects() {
    return selectedObjects;
  }

  // Get the list of all game objects in the scene
  public List<GameObject> getObjects() {
    return objects;
  }

  // Get the last selected object index (useful for property display)
  public int getLastSelectedObjectIndex() {
    return lastSelectedObjectIndex;
  }

  // Set the last selected object index
  public void setLastSelectedObjectIndex(int index) {
    this.lastSelectedObjectIndex = index;
  }


  // Get the current transformation mode
  public TransformMode getCurrentMode() {
    return currentMode;
  }

  // Set the current transformation mode
  public void setCurrentMode(TransformMode mode) {
    if (this.currentMode != mode) {
      this.currentMode = mode;
      System.out.println("Transform Mode changed to: " + mode);
    }
  }

  // Method to delete selected objects
  public void deleteSelectedObjects() {
    if (selectedObjects.isEmpty()) return;

    saveStateForUndo(); // Save state before deletion

    // Sort indices in descending order to avoid issues with removal
    List<Integer> sortedSelectedObjects = new ArrayList<>(selectedObjects);
    Collections.sort(sortedSelectedObjects, Collections.reverseOrder());

    for (int index : sortedSelectedObjects) {
      if (index >= 0 && index < objects.size()) {
        objects.remove(index);
      }
    }

    selectedObjects.clear(); // Clear selection after deletion
    lastSelectedObjectIndex = -1;
    selectedVertices.clear(); // Clear component selections
    selectedEdges.clear();
    selectedFaces.clear();
    currentTransformationState = TransformationState.NONE; // Reset transformation state
    translatingGlobalAxis = 0;
    isComponentPreSelected = false;
    dragging = false;

    saveStateForUndo(); // Save state after deletion
    System.out.println("Deleted selected objects.");
  }

  // Method to get the current camera pitch for mini-axes rendering
  public float getCameraPitch() {
    return cameraPitch;
  }

  // Method to get the current camera yaw for mini-axes rendering
  public float getCameraYaw() {
    return cameraYaw;
  }

  // Method to get the current camera zoom
  public float getCameraZoom() {
    return cameraZoom;
  }

  // Method to get the current camera pan offset
  public Vector3f getCameraPanOffset() {
    return cameraPanOffset;
  }


  // --- End of ImGuiLayer interaction methods ---


  public static void main(String[] args) {
    // The main entry point now just creates and starts the engine.
    // The engine's start() method will initialize and run the main game loop.
    new PhoenixGameEngine().start();
  }
}
