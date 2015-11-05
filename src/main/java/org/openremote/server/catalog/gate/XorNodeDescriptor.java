package org.openremote.server.catalog.gate;

import org.apache.camel.CamelContext;
import org.openremote.server.catalog.NodeDescriptor;
import org.openremote.server.route.NodeRoute;
import org.openremote.server.util.IdentifierUtil;
import org.openremote.shared.flow.Flow;
import org.openremote.shared.flow.Node;
import org.openremote.shared.flow.Slot;

import java.util.List;

public class XorNodeDescriptor extends NodeDescriptor {

    public static final String TYPE = "urn:openremote:flow:node:xor";
    public static final String TYPE_LABEL = "XOR";

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
        return new XorRoute(context, flow, node);
    }

    @Override
    public void addSlots(List<Slot> slots) {
        super.addSlots(slots);
        slots.add(new Slot("A", IdentifierUtil.generateGlobalUniqueId(), Slot.TYPE_SINK));
        slots.add(new Slot("B", IdentifierUtil.generateGlobalUniqueId(), Slot.TYPE_SINK));
        slots.add(new Slot(IdentifierUtil.generateGlobalUniqueId(), Slot.TYPE_SOURCE));
    }
}
