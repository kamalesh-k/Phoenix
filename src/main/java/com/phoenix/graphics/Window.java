package com.phoenix.graphics;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWMouseButtonCallback; // Import for mouse button callback
import org.lwjgl.glfw.GLFWScrollCallback;      // Import for scroll callback (potentially useful for ImGui)
import org.lwjgl.glfw.GLFWKeyCallback;         // Import for key callback (potentially useful for ImGui)
import org.lwjgl.glfw.GLFWCharCallback;        // Import for char callback (potentially useful for ImGui)
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Window {
  private long windowHandle;
  private int width;
  private int height;
  private String title;

  private GLFWFramebufferSizeCallback framebufferSizeCallback;
  private GLFWMouseButtonCallback mouseButtonCallback; // Mouse button callback set by this Window class
  // ImGuiImplGlfw will install its own callbacks for mouse, scroll, key, and char events
  // when its init method is called with install_callbacks = true.
  // The engine's update loop should check ImGui.getIO().getWantCaptureMouse()/Keyboard()
  // to decide whether to process its own input or let ImGui handle it.

  // Simple flags to track if a click occurred this frame, set by this Window's mouseButtonCallback
  private boolean leftMouseClicked = false;
  private boolean rightMouseClicked = false;
  private boolean middleMouseClicked = false;


  /**
   * Constructor for the Window.
   * @param initialWidth The initial width of the window.
   * @param initialHeight The initial height of the window.
   * @param title The title of the window.
   */
  public Window(int initialWidth, int initialHeight, String title) {
    this.width = initialWidth;
    this.height = initialHeight;
    this.title = title;

    if (!GLFW.glfwInit()) {
      throw new IllegalStateException("Failed to initialize GLFW");
    }

    glfwDefaultWindowHints();
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
    // For macOS compatibility if using newer OpenGL versions (e.g., 3.2+)
    // glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
    // glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
    // glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
    // glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);


    windowHandle = GLFW.glfwCreateWindow(this.width, this.height, this.title, 0, 0);
    if (windowHandle == 0) {
      GLFW.glfwTerminate();
      throw new RuntimeException("Failed to create GLFW window");
    }

    // Setup framebuffer size callback
    framebufferSizeCallback = new GLFWFramebufferSizeCallback() {
      @Override
      public void invoke(long window, int newWidth, int newHeight) {
        if (newWidth > 0 && newHeight > 0) {
          Window.this.width = newWidth;
          Window.this.height = newHeight;
          GL11.glViewport(0, 0, newWidth, newHeight);
        }
      }
    };
    GLFW.glfwSetFramebufferSizeCallback(windowHandle, framebufferSizeCallback);

    // Setup this Window's mouse button callback to set click flags
    // ImGui will set its own callback that will also be processed.
    mouseButtonCallback = new GLFWMouseButtonCallback() {
      @Override
      public void invoke(long window, int button, int action, int mods) {
        // We are interested in the RELEASE action for click detection
        if (action == GLFW_RELEASE) {
          if (button == GLFW_MOUSE_BUTTON_LEFT) {
            leftMouseClicked = true;
          } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
            rightMouseClicked = true;
          } else if (button == GLFW_MOUSE_BUTTON_MIDDLE) {
            middleMouseClicked = true;
          }
        }
        // Note: If ImGui is active and wants mouse capture, its callback (ImGuiImplGlfw)
        // will likely consume the event first or handle it. The engine logic
        // must check io.WantCaptureMouse.
      }
    };
    GLFW.glfwSetMouseButtonCallback(windowHandle, mouseButtonCallback);


    // Center the window
    GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
    if (vidmode != null) {
      glfwSetWindowPos(
          windowHandle,
          (vidmode.width() - this.width) / 2,
          (vidmode.height() - this.height) / 2
      );
    } else {
      // Fallback position if primary monitor info isn't available
      glfwSetWindowPos(windowHandle, 100, 100);
    }

    GLFW.glfwMakeContextCurrent(windowHandle);
    // Enable v-sync
    GLFW.glfwSwapInterval(1);
    GLFW.glfwShowWindow(windowHandle);

    // This line is critical for LWJGL's interoperation with GLFW's
    // OpenGL context, or any context that is managed externally.
    // It detects the context that is current in the current thread,
    // creates the GLCapabilities instance and makes the OpenGL
    // bindings available for use.
    GL.createCapabilities();

    // Set the clear color (optional, can be done in engine's render)
    // GL11.glClearColor(0.1f, 0.1f, 0.1f, 1.0f); // Example dark grey
    GL11.glViewport(0, 0, this.width, this.height); // Set initial viewport
  }

  /**
   * Swaps the front and back buffers.
   */
  public void swapBuffers() {
    GLFW.glfwSwapBuffers(windowHandle);
  }

  /**
   * Checks if the window should close.
   * @return true if the window should close, false otherwise.
   */
  public boolean shouldClose() {
    return GLFW.glfwWindowShouldClose(windowHandle);
  }

  /**
   * Polls for pending window events and resets click flags.
   * This should be called once per frame BEFORE processing input.
   */
  public void pollEvents() {
    // Reset click flags at the start of polling for the new frame
    // These flags indicate a click (release) happened in the *previous* frame's event processing.
    leftMouseClicked = false;
    rightMouseClicked = false;
    middleMouseClicked = false;

    GLFW.glfwPollEvents(); // This will invoke callbacks, potentially setting the flags above to true for the current frame.
  }

  /**
   * Clears the color and depth buffers.
   * Typically called at the start of a render pass.
   */
  public void clear() {
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
  }

  /**
   * Cleans up GLFW resources.
   * Should be called when the application is shutting down.
   */
  public void cleanup() {
    if (framebufferSizeCallback != null) {
      framebufferSizeCallback.free();
    }
    if (mouseButtonCallback != null) {
      mouseButtonCallback.free(); // Free the mouse button callback
    }
    // Other callbacks like scroll, key, char would also need freeing if set directly here.
    // However, ImGuiImplGlfw manages its own callbacks.

    GLFW.glfwDestroyWindow(windowHandle);
    GLFW.glfwTerminate();
    // glfwSetErrorCallback(null).free(); // Free the error callback if it was set
  }

  /**
   * Sets the title of the window.
   * @param newTitle The new title for the window.
   */
  public void setTitle(String newTitle) {
    this.title = newTitle;
    GLFW.glfwSetWindowTitle(windowHandle, this.title);
  }

  /**
   * Checks if a specific keyboard key is currently pressed.
   * @param glfwKeyCode The GLFW key code (e.g., GLFW_KEY_SPACE).
   * @return true if the key is pressed, false otherwise.
   */
  public boolean isKeyPressed(int glfwKeyCode) {
    return GLFW.glfwGetKey(windowHandle, glfwKeyCode) == GLFW.GLFW_PRESS;
  }

  /**
   * Gets the current X-coordinate of the mouse cursor relative to the window.
   * @return The X-coordinate.
   */
  public double getMouseX() {
    double[] xpos = new double[1];
    GLFW.glfwGetCursorPos(windowHandle, xpos, null);
    return xpos[0];
  }

  /**
   * Gets the current Y-coordinate of the mouse cursor relative to the window.
   * @return The Y-coordinate.
   */
  public double getMouseY() {
    double[] ypos = new double[1];
    GLFW.glfwGetCursorPos(windowHandle, null, ypos);
    return ypos[0];
  }

  /**
   * Checks if a specific mouse button is currently pressed.
   * @param glfwMouseButtonCode The GLFW mouse button code (e.g., GLFW_MOUSE_BUTTON_LEFT).
   * @return true if the button is pressed, false otherwise.
   */
  public boolean isMouseButtonPressed(int glfwMouseButtonCode) {
    return GLFW.glfwGetMouseButton(windowHandle, glfwMouseButtonCode) == GLFW.GLFW_PRESS;
  }

  /**
   * Checks if the left mouse button was clicked (i.e., released in the last polled events).
   * This flag is reset by pollEvents().
   * @return true if clicked since last pollEvents(), false otherwise.
   */
  public boolean isLeftMouseClicked() {
    return leftMouseClicked;
  }

  /**
   * Checks if the right mouse button was clicked (i.e., released in the last polled events).
   * This flag is reset by pollEvents().
   * @return true if clicked since last pollEvents(), false otherwise.
   */
  public boolean isRightMouseClicked() {
    return rightMouseClicked;
  }

  /**
   * Checks if the middle mouse button was clicked (i.e., released in the last polled events).
   * This flag is reset by pollEvents().
   * @return true if clicked since last pollEvents(), false otherwise.
   */
  public boolean isMiddleMouseClicked() {
    return middleMouseClicked;
  }


  // --- Getter Methods ---

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  /**
   * Gets the GLFW window handle.
   * @return The window handle (long).
   */
  public long getWindowHandle() {
    return windowHandle;
  }
}
