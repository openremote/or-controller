package org.openremote.beta.client.editor.flow.node;

import com.google.gwt.core.client.js.JsType;
import org.openremote.beta.shared.event.Event;

@JsType
public class NodePropertiesUpdatedEvent extends Event {

    final public String nodeId;
    final public String nodeProperties;

    public NodePropertiesUpdatedEvent(String nodeId, String nodeProperties) {
        this.nodeId = nodeId;
        this.nodeProperties = nodeProperties;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getNodeProperties() {
        return nodeProperties;
    }
}