package org.openremote.beta.shared.flow;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.gwt.core.client.js.JsType;
import org.openremote.beta.shared.model.Identifier;
import org.openremote.beta.shared.model.Properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion.NON_NULL;
import static org.openremote.beta.shared.model.PropertyDescriptor.TYPE_BOOLEAN;

@JsType
@JsonSerialize(include = NON_NULL)
@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE, isGetterVisibility = NONE)
public class Node extends FlowObject {

    public static final String PROPERTY_CLIENT_ACCESS = "clientAccess";
    public static final String PROPERTY_PRE_ENDPOINT = "preEndpoint";
    public static final String PROPERTY_POST_ENDPOINT = "postEndpoint";

    public static final String EDITOR_PROPERTIES = "editor";
    public static final String EDITOR_PROPERTY_COMPONENT= "component";
    public static final String EDITOR_PROPERTY_X = "x";
    public static final String EDITOR_PROPERTY_Y = "y";
    public static final String EDITOR_PROPERTY_COLOR = "color";
    public static final String EDITOR_PROPERTY_TYPE_LABEL = "typeLabel";

    public static final String TYPE_SUBFLOW = "urn:org-openremote:flow:node:subflow";
    public static final String TYPE_SUBFLOW_LABEL = "Flow";

    public Slot[] slots = new Slot[0];
    public Map<String, Object> properties;

    public Node() {
    }

    public Node(String label, Identifier identifier) {
        super(label, identifier);
    }

    public Node(String label, Identifier identifier, Slot... slots) {
        super(label, identifier);
        this.slots = slots;
    }

    public Node(String label, Identifier identifier, Slot[] slots, Map<String, Object> properties) {
        super(label, identifier);
        this.slots = slots;
        this.properties = properties;
    }

    public Slot[] getSlots() {
        return slots;
    }

    public Map<String, Object> getProperties() {
        if (properties == null) {
            properties = Properties.create();
        }
        return properties;
    }

   public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public boolean hasProperties() {
        return properties != null;
    }

    public Map<String, Object> getEditorProperties() {
        if (Properties.getProperties(getProperties(), EDITOR_PROPERTIES)  == null) {
            Properties.create(getProperties(), EDITOR_PROPERTIES);
        }
        return Properties.getProperties(getProperties(), EDITOR_PROPERTIES);
    }

    public boolean isClientAccessEnabled() {
        // Should we accept client message events for this node's sinks and should
        // we send client message events when this node received a message?
        return Properties.isTrue(getProperties(), TYPE_BOOLEAN, PROPERTY_CLIENT_ACCESS);
    }

    public Slot findSlot(String slotId) {
        for (Slot slot : getSlots()) {
            if (slot.getId().equals(slotId))
                return slot;
        }
        return null;
    }

    public Slot[] findSlots(String type) {
        List<Slot> list = new ArrayList<>();
        for (Slot slot : getSlots()) {
            if (slot.isOfType(type))
                list.add(slot);
        }
        return list.toArray(new Slot[list.size()]);
    }

    public Slot[] findConnectableSlots(String type) {
        List<Slot> list = new ArrayList<>();
        for (Slot slot : getSlots()) {
            if (slot.isOfType(type) && slot.isConnectable())
                list.add(slot);
        }
        return list.toArray(new Slot[list.size()]);
    }

    public Slot findSlotByPosition(int position, String type) {
        if (position > getSlots().length-1)
            return null;
        if (getSlots()[position].getIdentifier().getType().equals(type))
            return getSlots()[position];
        return null;
    }
}
