package com.app.subly.component;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Central undo/redo stack for all table edits.
 * Push Edits after you apply the change to the model.
 */
public final class EditHistory {

    public interface Edit {
        void undo();

        void redo();

        default boolean isNoOp() {
            return false;
        }
    }

    public static Edit of(Runnable undo, Runnable redo) {
        return new Edit() {
            @Override
            public void undo() {
                if (undo != null) undo.run();
            }

            @Override
            public void redo() {
                if (redo != null) redo.run();
            }
        };
    }

    public static final class CompoundEdit implements Edit {
        private final List<Edit> children = new ArrayList<>();

        public CompoundEdit add(Edit e) {
            if (e != null && !e.isNoOp()) children.add(e);
            return this;
        }

        public boolean isEmpty() {
            return children.isEmpty();
        }

        @Override
        public void undo() {
            for (int i = children.size() - 1; i >= 0; i--) children.get(i).undo();
        }

        @Override
        public void redo() {
            for (int i = 0; i < children.size(); i++) children.get(i).redo();
        }
    }

    private final Deque<Edit> undoStack = new ArrayDeque<>();
    private final Deque<Edit> redoStack = new ArrayDeque<>();
    private final ReadOnlyBooleanWrapper canUndo = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper canRedo = new ReadOnlyBooleanWrapper(false);
    private final Runnable postApply; // e.g., ensureTrailingBlankRow + table.refresh
    private final Runnable markDirty;

    public EditHistory(Runnable postApply, Runnable markDirty) {
        this.postApply = postApply != null ? postApply : () -> {
        };
        this.markDirty = markDirty != null ? markDirty : () -> {
        };
    }

    public void push(Edit e) {
        if (e == null || e.isNoOp()) return;
        undoStack.push(e);
        redoStack.clear();
        updateFlags();
        markDirty.run();
    }

    public void undo() {
        if (undoStack.isEmpty()) return;
        Edit e = undoStack.pop();
        e.undo();
        redoStack.push(e);
        updateFlags();
        postApply.run();
        markDirty.run();
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        Edit e = redoStack.pop();
        e.redo();
        undoStack.push(e);
        updateFlags();
        postApply.run();
        markDirty.run();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
        updateFlags();
    }

    public ReadOnlyBooleanProperty canUndoProperty() {
        return canUndo.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty canRedoProperty() {
        return canRedo.getReadOnlyProperty();
    }

    public boolean canUndo() {
        return canUndo.get();
    }

    public boolean canRedo() {
        return canRedo.get();
    }

    private void updateFlags() {
        canUndo.set(!undoStack.isEmpty());
        canRedo.set(!redoStack.isEmpty());
    }
}