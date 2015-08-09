package org.openremote.beta.shared.flow;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.gwt.core.client.js.JsType;
import org.openremote.beta.shared.model.Identifier;

import java.util.*;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion.NON_NULL;

@JsType
@JsonSerialize(include= NON_NULL)
@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
public class Flow extends FlowObject {

    public static final String TYPE = "urn:org-openremote:flow";

    public Node[] nodes = new Node[0];
    public Wire[] wires = new Wire[0];

    public Flow() {
    }

    public Flow(String label, Identifier identifier) {
        super(label, identifier);
    }

    public Flow(String label, Identifier identifier, Node... nodes) {
        super(label, identifier);
        this.nodes = nodes;
    }

    public Flow(String label, Identifier identifier, Node[] nodes, Wire[] wires) {
        super(label, identifier);
        this.nodes = nodes;
        this.wires = wires;
    }

    public Node[] getNodes() {
        return nodes;
    }

    public void addNode(Node node) {
        Set<Node> collection = new HashSet<>(Arrays.asList(getNodes()));
        collection.add(node);
        this.nodes = collection.toArray(new Node[collection.size()]);
    }

    public void removeNode(Node node) {
        Set<Node> collection = new HashSet<>(Arrays.asList(getNodes()));
        collection.remove(node);
        this.nodes = collection.toArray(new Node[collection.size()]);
    }

    public Wire[] getWires() {
        return wires;
    }

    public void addWireBetweenSlots(Slot sourceSlot, Slot sinkSlot) {
        addWire(new Wire(sourceSlot.getId(), sinkSlot.getId()));
    }

    public void addWire(Wire wire) {
        Set<Wire> collection = new HashSet<>(Arrays.asList(getWires()));
        collection.add(wire);
        this.wires = collection.toArray(new Wire[collection.size()]);
    }

    public void removeWire(Slot sourceSlot, Slot sinkSlot) {
        Set<Wire> collection = new HashSet<>(Arrays.asList(getWires()));
        Iterator<Wire> it = collection.iterator();
        while (it.hasNext()) {
            Wire wire = it.next();
            if (wire.getSourceId().equals(sourceSlot.getId())
                && wire.getSinkId().equals(sinkSlot.getId())) {
                it.remove();
            }
        }
        this.wires = collection.toArray(new Wire[collection.size()]);
    }

    public Slot findSlot(String slotId) {
        for (Node node : getNodes()) {
            Slot slot = node.findSlot(slotId);
            if (slot != null)
                return slot;
        }
        return null;
    }

    public boolean hasWires(String slotId) {
        for (Wire wire : getWires()) {
            if (wire.getSourceId().equals(slotId))
                return true;
            if (wire.getSinkId().equals(slotId))
                return true;
        }
        return false;
    }

    public Wire[] findWiresForSource(String slotId) {
        List<Wire> list = new ArrayList<>();
        for (Wire wire : getWires()) {
            if (wire.getSourceId().equals(slotId))
                list.add(wire);
        }
        return list.toArray(new Wire[list.size()]);
    }

    public Wire[] findWiresForSink(String slotId) {
        List<Wire> list = new ArrayList<>();
        for (Wire wire : getWires()) {
            if (wire.getSinkId().equals(slotId))
                list.add(wire);
        }
        return list.toArray(new Wire[list.size()]);
    }

    public Node findNode(String nodeId) {
        for (Node node : getNodes()) {
            if (node.getId().equals(nodeId))
                return node;
        }
        return null;
    }

    public boolean isNodeWiredToNodeOfType(Node node, String nodeType) {
        for (Slot source : node.findSlots(Slot.TYPE_SOURCE)) {
            Wire[] wires = findWiresForSource(source.getId());
            for (Wire wire : wires) {
                Node otherSide = findOwnerNode(wire.getSinkId());
                if (!otherSide.getIdentifier().getType().equals(nodeType))
                    return true;
            }
        }
        for (Slot sink : node.findSlots(Slot.TYPE_SINK)) {
            Wire[] wires = findWiresForSink(sink.getId());
            for (Wire wire : wires) {
                Node otherSide = findOwnerNode(wire.getSourceId());
                if (!otherSide.getIdentifier().getType().equals(Node.TYPE_CLIENT))
                    return true;
            }
        }
        return false;
    }

    public Node findOwnerNode(String slotId) {
        for (Node node : getNodes()) {
            Slot slot = node.findSlot(slotId);
            if (slot != null)
                return node;
        }
        return null;
    }
}
