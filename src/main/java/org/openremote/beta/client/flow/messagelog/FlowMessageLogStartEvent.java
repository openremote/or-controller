package org.openremote.beta.client.flow.messagelog;

import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsType;
import org.openremote.beta.client.flow.FlowEvent;
import org.openremote.beta.shared.flow.Flow;

@JsExport
@JsType
public class FlowMessageLogStartEvent extends FlowEvent {

    public FlowMessageLogStartEvent(Flow flow) {
        super(flow);
    }
}
