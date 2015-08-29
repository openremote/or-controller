package org.openremote.beta.client.editor.flow.control;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsType;
import org.openremote.beta.client.editor.flow.*;
import org.openremote.beta.client.shared.ShowFailureEvent;
import org.openremote.beta.client.shared.ShowInfoEvent;
import org.openremote.beta.client.shared.request.RequestFailure;
import org.openremote.beta.client.shared.request.RequestFailureEvent;
import org.openremote.beta.client.shared.request.RequestPresenter;
import org.openremote.beta.client.shared.session.event.ServerSendEvent;
import org.openremote.beta.shared.event.*;
import org.openremote.beta.shared.flow.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openremote.beta.client.shared.Timeout.debounce;

@JsExport
@JsType
public class FlowControlPresenter extends RequestPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(FlowControlPresenter.class);

    private static final FlowCodec FLOW_CODEC = GWT.create(FlowCodec.class);

    public Flow flow;
    public boolean unsaved;
    public String flowControlTitle;
    public boolean flowControlDirty = false;
    public FlowStatusDetail flowStatusDetail;

    public FlowControlPresenter(com.google.gwt.dom.client.Element view) {
        super(view);

        addEventListener(FlowControlStartEvent.class, event -> {
            flow = event.getFlow();
            notifyPath("flow");
            unsaved = event.isUnsaved();
            notifyPath("unsaved", unsaved);

            setFlowControlDirty(unsaved);
            setFlowStatusDetail(new FlowStatusDetail("REQUESTING FLOW STATUS..."));
            dispatchEvent(false, new FlowRequestStatusEvent(flow.getId()));
        });

        addEventListener(FlowControlStopEvent.class, event -> {
            flow = null;
            notifyPath("flow");
        });

        addEventListener(FlowRequestStatusEvent.class, event -> {
            dispatchEvent(new ServerSendEvent(event));
        });

        addEventListener(FlowDeployEvent.class, event -> {
            dispatchEvent(new ServerSendEvent(event));
        });

        addEventListener(FlowStopEvent.class, event -> {
            dispatchEvent(new ServerSendEvent(event));
        });

        addEventListener(FlowStatusEvent.class, event -> {
            if (isConsumable(event)) {
                setFlowStatusDetail(new FlowStatusDetail(event.getPhase()));
            }
        });

        addEventListener(FlowDeploymentFailureEvent.class, event -> {
            if (isConsumable(event)) {
                setFlowStatusDetail(new FlowStatusDetail(event));
            }
        });

        addEventListener(FlowUpdatedEvent.class, event -> {
            setFlowControlDirty(true);
            if (flowStatusDetail != null)
                setFlowStatusDetail(flowStatusDetail);
        });

        addEventListener(FlowSavedEvent.class, event -> {
            dispatchEvent(new ShowInfoEvent("Flow '" + event.getFlow().getLabel() + "' saved."));
            if (flow != null && flow.getId().equals(event.getFlow().getId())) {
                unsaved = false;
                notifyPath("unsaved", unsaved);
                setFlowControlDirty(false);
            }
        });

        addEventListener(FlowDeletedEvent.class, event -> {
            dispatchEvent(new ShowInfoEvent("Flow '" + event.getFlow().getLabel() + "' deleted."));
        });
    }

    public void flowPropertyChanged() {
        flowControlDirty = true; // Assume this is dirty, now other listeners might run
        debounce("FlowPropertyChange", () -> {
            if (flowControlDirty) { // If it's still dirty (after other listeners executed), tell the user
                dispatchEvent(new FlowUpdatedEvent(flow));
            }
        }, 500);
    }

    protected boolean isConsumable(FlowIdEvent flowIdEvent) {
        return flow != null && flow.getId().equals(flowIdEvent.getFlowId());
    }

    public void redeployFlow() {
        if (flow == null)
            return;
        String flowId = flow.getId();
        dispatchEvent(false, new FlowStopEvent(flowId));
        saveFlow(flow, () -> dispatchEvent(false, new FlowDeployEvent(flowId)));
    }

    public void saveFlow() {
        if (flow == null)
            return;
        saveFlow(flow, () -> {
            dispatchEvent(false, new FlowRequestStatusEvent(flow.getId()));
        });
    }

    public void startFlow() {
        if (flow == null)
            return;
        dispatchEvent(false, new FlowDeployEvent(flow.getId()));
    }

    public void stopFlow() {
        if (flow == null)
            return;
        dispatchEvent(false, new FlowStopEvent(flow.getId()));
    }

    public void deleteFlow() {
        if (flow == null)
            return;
        dispatchEvent(new ConfirmationEvent(
            "Remove Flow",
            flow.getLabel() != null && flow.getLabel().length() > 0
                ? "Are you sure you want to delete flow '" + flow.getLabel() + "'?"
                : "Are you sure you want to delete this flow?",
            () -> {
                sendRequest(
                    false,
                    false,
                    resource("flow", flow.getId()).delete(),
                    new StatusResponseCallback("Delete flow", 204) {
                        @Override
                        protected void onResponse() {
                            dispatchEvent(new FlowDeletedEvent(flow));
                        }

                        @Override
                        public void onFailure(RequestFailure requestFailure) {
                            super.onFailure(requestFailure);
                            if (requestFailure.statusCode == 409) {
                                dispatchEvent(new ShowFailureEvent(
                                    "Flow '" + flow.getLabel() + "' can't be deleted, stopping it failed or it's in use by other flows.",
                                    5000
                                ));
                            } else {
                                dispatchEvent(new RequestFailureEvent(requestFailure));
                            }
                        }
                    }
                );
            }
        ));
    }

    public String exportFlow() {
        if (flow == null)
            return "";
        String json = FLOW_CODEC.encode(flow).toString();
        dispatchEvent(new ShowInfoEvent("Export complete, check your JavaScript console."));
        return json;
    }

    protected void setFlowControlDirty(boolean dirty) {
        flowControlDirty = dirty;
        notifyPath("flowControlDirty", flowControlDirty);
    }

    protected void setFlowStatusDetail(FlowStatusDetail flowStatusDetail) {
        if (flow != null) {

            this.flowStatusDetail = flowStatusDetail;
            notifyPath("flowStatusDetail");

            if (unsaved) {
                flowControlTitle = flow.getLabel() + " (New)";
            } else {
                flowControlTitle = flow.getLabel();
            }
            notifyPath("flowControlTitle", flowControlTitle);
        }
    }

    protected void saveFlow(Flow flow, Runnable callback) {
        if (unsaved) {
            sendRequest(
                resource("flow").post().json(FLOW_CODEC.encode(flow)),
                new StatusResponseCallback("Save new flow", 201) {
                    @Override
                    protected void onResponse() {
                        dispatchEvent(new FlowSavedEvent(flow));
                        callback.run();
                    }
                }
            );
        } else {
            // Make a copy and remove dependencies, they are calculated on the server
            final Flow flowCopy = FLOW_CODEC.decode(FLOW_CODEC.encode(flow));
            flowCopy.clearDependencies();
            sendRequest(
                resource("flow", flowCopy.getId()).put().json(FLOW_CODEC.encode(flowCopy)),
                new StatusResponseCallback("Save flow", 204) {
                    @Override
                    protected void onResponse() {
                        dispatchEvent(new FlowSavedEvent(flow));
                        callback.run();
                    }
                }
            );
        }
    }
}
