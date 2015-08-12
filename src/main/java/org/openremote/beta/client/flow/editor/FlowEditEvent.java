package org.openremote.beta.client.flow.editor;

import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsType;
import org.openremote.beta.client.flow.FlowEvent;
import org.openremote.beta.shared.flow.Flow;

@JsExport
@JsType
public class FlowEditEvent extends FlowEvent {

    public FlowEditEvent(Flow flow) {
        super(flow);
    }

}