package org.openremote.beta.server.catalog.change;

import org.apache.camel.CamelContext;
import org.openremote.beta.server.catalog.NodeDescriptor;
import org.openremote.beta.server.route.NodeRoute;
import org.openremote.beta.server.util.IdentifierUtil;
import org.openremote.beta.shared.flow.Flow;
import org.openremote.beta.shared.flow.Node;
import org.openremote.beta.shared.flow.Slot;
import org.openremote.beta.shared.model.Identifier;

import java.util.List;

public class ChangeNodeDescriptor extends NodeDescriptor {

    public static final String TYPE = "urn:org-openremote:flow:node:change";
    public static final String TYPE_LABEL = "Change";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getTypeLabel() {
        return TYPE_LABEL;
    }

    @Override
    public NodeRoute createRoute(CamelContext context, Flow flow, Node node) {
        return new ChangeRoute(context, flow, node);
    }

    @Override
    public void addSlots(List<Slot> slots) {
        super.addSlots(slots);
        slots.add(new Slot(new Identifier(IdentifierUtil.generateGlobalUniqueId(), Slot.TYPE_SINK)));
        slots.add(new Slot(new Identifier(IdentifierUtil.generateGlobalUniqueId(), Slot.TYPE_SOURCE)));
    }

    @Override
    public void addEditorComponents(List<String> editorComponents) {
        super.addEditorComponents(editorComponents);
        editorComponents.add("or-editor-node-change");
    }

    @Override
    protected void addPersistentPropertyPaths(List<String> propertyPaths) {
        super.addPersistentPropertyPaths(propertyPaths);
        propertyPaths.add("prepend");
        propertyPaths.add("append");
    }
}