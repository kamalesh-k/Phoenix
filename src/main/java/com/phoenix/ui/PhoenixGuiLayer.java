package com.phoenix.ui;

import imgui.ImFontAtlas;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.gl3.ImGuiImplGl3; // Import ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw; // Import ImGuiImplGlfw
import imgui.flag.ImGuiCond; // For setting window position/size conditions
import imgui.type.ImBoolean; // Use imgui.type.ImBoolean
// Removed import imgui.ImGuiFreeTypeBuilderFlags; // Removed as it's not used and might require extra dependencies

// Removed ImGui.app imports as we are not extending Application
// import imgui.app.Application;
// import imgui.app.Configuration;

import com.phoenix.core.GameObject;
import com.phoenix.core.PhoenixGameEngine; // To access game objects and state
import org.joml.Vector3f; // For accessing object properties

import java.util.List; // Import List for selected objects

// PhoenixGuiLayer is no longer an Application, it's a UI component
public class PhoenixGuiLayer {

    private final PhoenixGameEngine engine;
    private ImGuiImplGlfw imGuiGlfw; // Renderer for GLFW
    private ImGuiImplGl3 imGuiGl3;   // Renderer for OpenGL 3+

    // Fields to control window visibility
    private boolean showDemoWindow = false; // Underlying boolean state
    // ImBoolean wrapper is needed for ImGui.checkbox when not using ImGui.app.Application's built-in handling
    private ImBoolean imBooleanShowDemoWindow; // Use imgui.type.ImBoolean

    private boolean showObjectPropertiesWindow = false; // Underlying boolean state, managed by ImBoolean pOpen locally in render
    private int objectPropertiesWindowIndex = -1; // Track which object's properties are shown

    // ImGui clear color for the viewport (can be set by ImGui widgets)
    // This color is read by the engine's render method
    private float[] clearColor = new float[]{0.2f, 0.2f, 0.2f, 1.0f}; // Default clear color

    // Status information to display
    private String statusSelMode = "OBJECT";
    private String statusObjName = "None";
    private String statusCompInfo = "";
    private String statusTMode = "GLOBAL";
    private String statusTState = "NONE";
    private String statusLAxis = "N";
    private String statusGAxis = "N";
    private String statusObjToTransName = "N/A";
    private float statusCameraZoom = 1.0f;
    private int statusUndoStackSize = 0;
    private int statusRedoStackSize = 0;


    // The constructor receives the engine instance
    public PhoenixGuiLayer(PhoenixGameEngine engine) {
        this.engine = engine;
        // Initialize the ImBoolean wrapper for showDemoWindow
        // Ensure this uses imgui.type.ImBoolean
        this.imBooleanShowDemoWindow = new ImBoolean(this.showDemoWindow);
    }

    // This method is called by the engine after the window and OpenGL context are created
    public void init(long windowPtr) {
        // Initialize ImGui context
        ImGui.createContext();

        // Get ImGui IO (Input/Output)
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null); // Disable .ini file saving/loading

        // Set up ImGui's GLFW and OpenGL renderers
        // Pass the window handle and set install_callbacks to true so ImGui handles input
        imGuiGlfw = new ImGuiImplGlfw();
        imGuiGl3 = new ImGuiImplGl3();

        // Initialize renderers with the window handle and install callbacks
        // The true parameter tells ImGuiImplGlfw to install its own input callbacks on the window.
        imGuiGlfw.init(windowPtr, true);
        imGuiGl3.init("#version 130"); // Use OpenGL 3.0+ with GLSL version 130

        // --- Font Atlas Building and Uploading ---
        // This is the crucial part to fix the "Font Atlas not built!" assertion
        final ImFontAtlas fontAtlas = io.getFonts(); // Use the imported ImFontAtlas
        // You can optionally add a default font here if needed, otherwise it uses a built-in default
        // fontAtlas.addFontDefault();

        // Build the font atlas - this generates the texture data
        // You can use ImGuiFreeTypeBuilderFlags.FORCE_SET_FONT_DATA_OWNER if using FreeType
        fontAtlas.build();

        // Upload the font texture to the GPU using the OpenGL renderer
        // This method should exist in imgui-java-lwjgl3 version 1.86.3 or 1.89.0
//        imGuiGl3.uploadFonts(true); // Pass true to destroy CPU side data after upload

        System.out.println("ImGui initialized and font atlas built within PhoenixGuiLayer.");
    }

    // This method is called by the engine every frame to render the UI
    public void process() {
        // Start a new ImGui frame
        imGuiGlfw.newFrame();
        ImGui.newFrame();

        // --- Render ImGui Content ---

        // Set a fixed initial position and size for the main tools window
        ImGui.setNextWindowPos(10, 10, ImGuiCond.Once);
        ImGui.setNextWindowSize(300, 400, ImGuiCond.Once);

        ImGui.begin("Phoenix Engine Tools");

        if (ImGui.collapsingHeader("Scene")) {
            if (ImGui.button("Add Cube")) {
                engine.addCube();
            }
            ImGui.sameLine();
            if (ImGui.button("Add Sphere")) {
                engine.addSphere();
            }
            ImGui.sameLine();
            if (ImGui.button("Delete Selected")) {
                engine.deleteSelectedObjects();
            }

            // Clear color is managed by the engine. Get it from the engine to display/edit.
            // ImGui.colorEdit3 directly modifies the float array passed to it.
            // Pass the engine's clear color array to the widget
            // Note: The engine now has a public getClearColor() method to expose this.
            ImGui.colorEdit3("Clear Color", clearColor); // Use the local clearColor field
        }

        if (ImGui.collapsingHeader("Selection")) {
            // Use engine.getCurrentSelectionMode() to get the current mode for the combo box label
            if (ImGui.beginCombo("Selection Mode", engine.getCurrentSelectionMode().toString())) {
                // Iterate through all SelectionMode enum values
                for (PhoenixGameEngine.SelectionMode mode : PhoenixGameEngine.SelectionMode.values()) {
                    // Use ImGui.selectable to allow changing the selection mode
                    boolean isSelected = (engine.getCurrentSelectionMode() == mode);
                    if (ImGui.selectable(mode.toString(), isSelected)) {
                        engine.setCurrentSelectionMode(mode); // Call engine method to change mode
                    }
                    // Set the initial focus when opening the combo (scrolling + keyboard navigation)
                    if (isSelected) {
                        ImGui.setItemDefaultFocus();
                    }
                }
                ImGui.endCombo();
            }

            // Display selected objects and a button to open properties
            List<Integer> selectedObjects = engine.getSelectedObjects();
            if (!selectedObjects.isEmpty()) {
                ImGui.text("Selected Objects:");
                // Create a copy to iterate over, as the list might be modified by selection changes
                List<Integer> currentSelectedObjects = new java.util.ArrayList<>(selectedObjects);
                for (int index : currentSelectedObjects) {
                    // Ensure the index is valid before accessing the object list
                    if (index >= 0 && index < engine.getObjects().size()) {
                        GameObject obj = engine.getObjects().get(index);
                        // Create a unique label for the button using object name and index
                        String buttonLabel = "Properties##" + obj.getName() + "_" + index;
                        ImGui.bulletText(obj.getName() + " (Index: " + index + ")");
                        ImGui.sameLine();
                        // Open properties window for the selected object when the button is clicked
                        if (ImGui.button(buttonLabel)) {
                            objectPropertiesWindowIndex = index; // Set the index for the properties window
                            showObjectPropertiesWindow = true;
                        }
                    }
                }
            } else {
                ImGui.text("No object selected.");
            }
        }

        if (ImGui.collapsingHeader("Transform")) {
            // Use engine.getCurrentMode() to get the current mode for the combo box label
            if (ImGui.beginCombo("Transform Mode", engine.getCurrentMode().toString())) {
                // Iterate through all TransformMode enum values
                for (PhoenixGameEngine.TransformMode mode : PhoenixGameEngine.TransformMode.values()) {
                    // Use ImGui.selectable to allow changing the transform mode
                    boolean isSelected = (engine.getCurrentMode() == mode);
                    if (ImGui.selectable(mode.toString(), isSelected)) {
                        engine.setCurrentMode(mode); // Call engine method to change mode
                    }
                    // Set the initial focus when opening the combo
                    if (isSelected) {
                        ImGui.setItemDefaultFocus();
                    }
                }
                ImGui.endCombo();
            }
            ImGui.text("Shortcuts: G (Move), R (Rotate), S (Scale)");
            ImGui.text("Axis Lock: X, Y, Z (Hold during transform)");
            ImGui.text("Plane Lock: Shift + Axis (Hold during transform)");
            ImGui.text("Duplicate: Ctrl + D");
            ImGui.text("Undo/Redo: Ctrl + Z / Ctrl + Y");
        }

        // Use the ImBoolean wrapper for the demo window checkbox
        // ImGui.checkbox modifies the boolean value inside the ImBoolean object
        if (ImGui.checkbox("Show ImGui Demo Window", imBooleanShowDemoWindow)) {
            this.showDemoWindow = imBooleanShowDemoWindow.get(); // Sync the underlying state
        }

        // Display status information at the bottom of the main tools window
        ImGui.separator();
        ImGui.text(String.format("SelM: %s | Obj: %s%s", statusSelMode, statusObjName, statusCompInfo));
        ImGui.text(String.format("TMode: %s | TState: %s", statusTMode, statusTState));
        ImGui.text(String.format("LAxis: %s | GAxis: %s (%s)", statusLAxis, statusGAxis, statusObjToTransName));
        ImGui.text(String.format("Zoom: %.2f | Undo: %d | Redo: %d", statusCameraZoom, statusUndoStackSize, statusRedoStackSize));


        ImGui.end(); // Phoenix Engine Tools

        // Show the ImGui demo window if the flag is true
        if (this.showDemoWindow) {
            // ImGui.showDemoWindow can take an ImBoolean to allow closing from the window itself
            // Use the same ImBoolean object
            ImGui.showDemoWindow(imBooleanShowDemoWindow);
            this.showDemoWindow = imBooleanShowDemoWindow.get(); // Sync the underlying state if closed by the user
        }

        // Object Properties Window
        // Use a local ImBoolean to manage the window's open/closed state
        // Only show if showObjectPropertiesWindow is true and a valid object index is set
        if (showObjectPropertiesWindow && objectPropertiesWindowIndex != -1 &&
            objectPropertiesWindowIndex < engine.getObjects().size()) {

            GameObject selectedObject = engine.getObjects().get(objectPropertiesWindowIndex);
            // Use a unique title for each object's properties window
            // The ImBoolean manages the window's open/closed state internally
            // Ensure this uses imgui.type.ImBoolean
            ImBoolean pOpenProperties = new ImBoolean(true); // Start open if conditions met
            // Begin the window, passing the ImBoolean to allow closing via the window's 'X' button
            if (ImGui.begin("Object Properties: " + selectedObject.getName() + "##" + objectPropertiesWindowIndex, pOpenProperties)) {

                // Display and edit object properties
                // Use float arrays for ImGui widgets
                float[] position = {selectedObject.getPosition().x, selectedObject.getPosition().y, selectedObject.getPosition().z};
                // ImGui.dragFloat3 modifies the 'position' array directly
                if (ImGui.dragFloat3("Position", position, 0.1f)) {
                    // Update the object's position from the modified array
                    selectedObject.getPosition().set(position[0], position[1], position[2]);
                    // Request undo save when a property is changed via UI
                    engine.requestUndoSaveForObjectPropertyChange(objectPropertiesWindowIndex);
                }

                // Display and edit rotation in degrees for easier user input
                float[] rotationDeg = {
                    (float) Math.toDegrees(selectedObject.getRotationAngles().x),
                    (float) Math.toDegrees(selectedObject.getRotationAngles().y),
                    (float) Math.toDegrees(selectedObject.getRotationAngles().z)
                };
                // ImGui.dragFloat3 modifies the 'rotationDeg' array directly
                if (ImGui.dragFloat3("Rotation (Deg)", rotationDeg, 1.0f)) {
                    // Convert degrees back to radians for the internal representation
                    selectedObject.getRotationAngles().set(
                        (float) Math.toRadians(rotationDeg[0]),
                        (float) Math.toRadians(rotationDeg[1]),
                        (float) Math.toRadians(rotationDeg[2])
                    );
                    engine.requestUndoSaveForObjectPropertyChange(objectPropertiesWindowIndex);
                }

                float[] scale = {selectedObject.getScale().x, selectedObject.getScale().y, selectedObject.getScale().z};
                // ImGui.dragFloat3 modifies the 'scale' array directly, with min/max limits
                if (ImGui.dragFloat3("Scale", scale, 0.01f, 0.01f, 100.0f)) {
                    // Ensure scale values are not zero or negative
                    selectedObject.getScale().set(Math.max(0.01f, scale[0]), Math.max(0.01f, scale[1]), Math.max(0.01f, scale[2]));
                    engine.requestUndoSaveForObjectPropertyChange(objectPropertiesWindowIndex);
                }

                // Button to close the properties window
                if (ImGui.button("Close")) {
                    showObjectPropertiesWindow = false;
                    objectPropertiesWindowIndex = -1; // Reset index when closing
                }

                ImGui.end(); // Object Properties Window

                // If the properties window was closed by its 'X' button (pOpenProperties becomes false),
                // update the flag and reset index
                if (!pOpenProperties.get()) {
                    showObjectPropertiesWindow = false;
                    objectPropertiesWindowIndex = -1;
                }
            } else {
                // If begin() returns false, the window is collapsed or closed by the user via 'X'
                // In this manual integration, we rely on the ImBoolean to track the 'X' button state.
                // If begin returned false, it means the window was likely closed via 'X' in the previous frame.
                // The state is already updated by !pOpenProperties.get() check above.
                // However, if begin() returns false due to collapsing, we don't want to close it.
                // A more robust approach would be to check pOpenProperties.get() AFTER ImGui.end().
                // Let's rely on the check after ImGui.end() for now.
            }
        } else if (showObjectPropertiesWindow) {
            // If showObjectPropertiesWindow was true but the selected object is no longer valid (e.g., deleted)
            // Close the properties window and reset the index
            showObjectPropertiesWindow = false;
            objectPropertiesWindowIndex = -1;
        }


        // Rendering
        ImGui.render(); // End ImGui frame and build draw data
        imGuiGl3.renderDrawData(ImGui.getDrawData()); // Render ImGui draw data using OpenGL
    }

    // This method is called by the engine when the application is shutting down
    public void cleanup() {
        // Dispose of ImGui renderers and context
        // When manually integrating, ImGui.destroyContext() should handle the cleanup
        // of the renderers as well. Explicit dispose calls are not typically needed
        // for ImGuiImplGlfw and ImGuiImplGl3 in this setup.
        // if (imGuiGl3 != null) imGuiGl3.dispose(); // Removed
        // if (imGuiGlfw != null) imGuiGlfw.dispose(); // Removed
        ImGui.destroyContext();
        System.out.println("ImGui cleaned up within PhoenixGuiLayer.");
    }

    // Method to update the status information displayed in the UI
    public void updateStatusInfo(String selMode, String objName, String compInfo, String tMode, String tState, String lAxis, String gAxis, String objToTransName, float cameraZoom, int undoSize, int redoSize) {
        this.statusSelMode = selMode;
        this.statusObjName = objName;
        this.statusCompInfo = compInfo;
        this.statusTMode = tMode;
        this.statusTState = tState;
        this.statusLAxis = lAxis;
        this.statusGAxis = gAxis;
        this.statusObjToTransName = objToTransName;
        this.statusCameraZoom = cameraZoom;
        this.statusUndoStackSize = undoSize;
        this.statusRedoStackSize = redoSize;
    }

    // Method to get the clear color from the UI (used by the engine)
    // The engine calls this to get the color set in the ImGui UI
    public float[] getClearColor() {
        return clearColor;
    }

    // Method to set whether the object properties window should be shown
    // Called by the engine when an object is selected/deselected
    public void setShowObjectPropertiesWindow(boolean show, int objectIndex) {
        this.showObjectPropertiesWindow = show;
        this.objectPropertiesWindowIndex = objectIndex;
    }

    // Removed the local ImBoolean class definition to avoid conflict with imgui.type.ImBoolean
    // private static class ImBoolean {
    //     private boolean value;
    //     public ImBoolean(boolean value) { this.value = value; }
    //     public boolean get() { return value; }
    //     public void set(boolean value) { this.value = value; }
    // }
}
