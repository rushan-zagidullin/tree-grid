package org.vaadin.treegrid;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.logging.Logger;

import org.vaadin.treegrid.client.TreeGridState;
import org.vaadin.treegrid.container.ContainerCollapsibleWrapper;
import org.vaadin.treegrid.container.IndexedContainerHierarchicalWrapper;
import org.vaadin.treegrid.event.CollapseEvent;
import org.vaadin.treegrid.event.ExpandEvent;

import com.vaadin.data.Collapsible;
import com.vaadin.data.Container;
import com.vaadin.server.EncodeResult;
import com.vaadin.server.JsonCodec;
import com.vaadin.server.ServerRpcManager;
import com.vaadin.shared.MouseEventDetails;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.grid.GridColumnState;
import com.vaadin.shared.ui.grid.GridConstants;
import com.vaadin.shared.ui.grid.GridServerRpc;
import com.vaadin.ui.ConnectorTracker;
import com.vaadin.ui.Grid;

import elemental.json.JsonObject;

/**
 * A grid component for displaying tabular hierarchical data.
 * <p>
 * Grid is always bound to a {@link com.vaadin.data.Container.Indexed} but is not a Container of any kind on itself.
 * <p>
 * For more information please see {@link Grid}'s documentation.
 */
public class TreeGrid extends Grid {

    private static final Logger logger = Logger.getLogger(TreeGrid.class.getName());

    public TreeGrid() {
        super();

        // Attaches hierarchy data to the row
        HierarchyDataGenerator.extend(this);

        // Override keyboard navigation
        NavigationExtension.extend(this);
    }

    @Override
    public void setContainerDataSource(Container.Indexed container) {
        if (container != null) {
            if (!(container instanceof Container.Hierarchical)) {
                container = new IndexedContainerHierarchicalWrapper(container);
            }

            if (!(container instanceof Collapsible)) {
                container = new ContainerCollapsibleWrapper(container);
            }
        }
        super.setContainerDataSource(container);
    }

    public void setHierarchyColumn(Object propertyId) {

        Column hierarchyColumn = getColumn(propertyId);

        if (hierarchyColumn == null) {
            logger.warning(String.format("Column does not exist with propertyId: %s", propertyId.toString()));
            return;
        }

        GridColumnState columnState = null;

        // Using reflection to access Grid.Column's private getState() method
        try {
            Method getStateMethod = Grid.Column.class.getDeclaredMethod("getState");
            getStateMethod.setAccessible(true);
            columnState = (GridColumnState) getStateMethod.invoke(hierarchyColumn);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (columnState != null) {
            getState().hierarchyColumnId = columnState.id;
        }
    }

    /**
     * Adds an ExpandListener to this TreeGrid.
     *
     * @param listener
     *         the listener to add
     * @since 0.7.6
     */
    public void addExpandListener(ExpandEvent.ExpandListener listener) {
        addListener(ExpandEvent.class, listener, ExpandEvent.ExpandListener.EXPAND_METHOD);
    }

    /**
     * Removes an ExpandListener from this TreeGrid.
     *
     * @param listener
     *         the listener to remove
     * @since 0.7.6
     */
    public void removeExpandListener(ExpandEvent.ExpandListener listener) {
        removeListener(ExpandEvent.class, listener, ExpandEvent.ExpandListener.EXPAND_METHOD);
    }

    /**
     * Adds a CollapseListener to this TreeGrid.
     *
     * @param listener
     *         the listener to add
     * @since 0.7.6
     */
    public void addCollapseListener(CollapseEvent.CollapseListener listener) {
        addListener(CollapseEvent.class, listener, CollapseEvent.CollapseListener.COLLAPSE_METHOD);
    }

    /**
     * Removes a CollapseListener from this TreeGrid.
     *
     * @param listener
     *         the listener to remove
     * @since 0.7.6
     */
    public void removeCollapseListener(CollapseEvent.CollapseListener listener) {
        removeListener(CollapseEvent.class, listener, CollapseEvent.CollapseListener.COLLAPSE_METHOD);
    }


    @Override
    protected TreeGridState getState() {
        return (TreeGridState) super.getState();
    }

    void toggleExpansion(Object itemId) {
        if (getContainerDataSource() instanceof Collapsible) {
            Collapsible container = (Collapsible) getContainerDataSource();

            boolean collapsed = container.isCollapsed(itemId);

            // Expand or collapse the item
            container.setCollapsed(itemId, !collapsed); // Collapsible

            // Fire expand or collapse event
            if (collapsed) {
                fireEvent(new ExpandEvent(this, getContainerDataSource().getItem(itemId), itemId));
            } else {
                fireEvent(new CollapseEvent(this, getContainerDataSource().getItem(itemId), itemId));
            }
        }
    }
}
