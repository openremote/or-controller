package org.openremote.beta.client.flow.control;

import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsType;
import org.openremote.beta.client.flow.FlowEvent;
import org.openremote.beta.shared.flow.Flow;

@JsExport
@JsType
public class FlowControlStartEvent extends FlowEvent {

    public FlowControlStartEvent(Flow flow) {
        super(flow);
    }
}
