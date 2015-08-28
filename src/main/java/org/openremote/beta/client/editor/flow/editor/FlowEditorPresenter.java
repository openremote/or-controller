package org.openremote.beta.client.editor.flow.editor;

import com.ait.lienzo.client.core.types.Transform;
import com.ait.lienzo.client.widget.LienzoPanel;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTMLPanel;
import elemental.dom.Element;
import org.openremote.beta.client.console.ConsoleMessageSendEvent;
import org.openremote.beta.client.console.ConsoleWidgetUpdatedEvent;
import org.openremote.beta.client.editor.flow.NodeCodec;
import org.openremote.beta.client.editor.flow.control.FlowControlStartEvent;
import org.openremote.beta.client.editor.flow.control.FlowControlStopEvent;
import org.openremote.beta.client.editor.flow.crud.FlowDeletedEvent;
import org.openremote.beta.client.editor.flow.designer.FlowDesigner;
import org.openremote.beta.client.editor.flow.designer.FlowDesignerNodeSelectedEvent;
import org.openremote.beta.client.editor.flow.designer.FlowEditorViewportMediator;
import org.openremote.beta.client.editor.flow.node.*;
import org.openremote.beta.client.shared.request.RequestPresenter;
import org.openremote.beta.client.shared.session.message.MessageReceivedEvent;
import org.openremote.beta.client.shared.session.message.MessageSendEvent;
import org.openremote.beta.shared.event.MessageEvent;
import org.openremote.beta.shared.flow.Flow;
import org.openremote.beta.shared.flow.Node;
import org.openremote.beta.shared.flow.Slot;
import org.openremote.beta.shared.flow.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@JsExport
@JsType
public class FlowEditorPresenter extends RequestPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(FlowEditorPresenter.class);

    private static final NodeCodec NODE_CODEC = GWT.create(NodeCodec.class);

    public Flow flow;
    public FlowDesigner flowDesigner;

    protected boolean isFlowNodeOpen;
    protected LienzoPanel flowDesignerPanel;
    protected Transform flowDesignerInitialTransform;

    public FlowEditorPresenter(com.google.gwt.dom.client.Element view) {
        super(view);

        addEventListener(FlowEditEvent.class, event -> {
            this.flow = event.getFlow();
            notifyPath("flow");

            dispatchEvent("#flowControl", new FlowControlStartEvent(flow, event.isUnsaved()));

            dispatchEvent("#flowNode", new FlowNodeCloseEvent());
            isFlowNodeOpen = false;

            startFlowDesigner();
        });

        addEventListener(FlowUpdatedEvent.class, event -> {
            dispatchEvent("#flowControl", event);
        });

        addEventListener(FlowDeletedEvent.class, event -> {
            this.flow = null;
            this.notifyPathNull("flow");
            dispatchEvent("#flowControl", new FlowControlStopEvent());

            dispatchEvent("#flowNode", new FlowNodeCloseEvent());
            isFlowNodeOpen = false;

            stopFlowDesigner();
        });

        addEventListener(FlowDesignerNodeSelectedEvent.class, event -> {
            dispatchEvent("#flowNode", new FlowNodeEditEvent(flow, event.getNode()));
            isFlowNodeOpen = true;
        });

        addEventListener(NodeUpdatedEvent.class, event -> {
            if (flowDesigner != null && flow != null && flow.getId().equals(event.getFlow().getId())) {
                flowDesigner.updateNodeShape(event.getNode());
                dispatchEvent(new FlowUpdatedEvent(flow));
            }
        });

        addEventListener(NodeDeletedEvent.class, event -> {
            if (flowDesigner != null && flow != null && flow.getId().equals(event.getFlow().getId())) {
                Node node = event.getNode();
                LOG.debug("Removing node shape from flow designer: " + node);
                flowDesigner.deleteNodeShape(node);
                dispatchEvent(new FlowUpdatedEvent(flow));
            }
        });

        addEventListener(NodeDuplicateEvent.class, event -> {
            if (flowDesigner != null && flow != null && flow.getId().equals(event.getFlow().getId())) {
                duplicateNode(event.getNode());
            }
        });

        addEventListener(MessageReceivedEvent.class, event -> {
            onMessageEvent(event.getMessageEvent());
        });

        addEventListener(MessageSendEvent.class, event -> {
            onMessageEvent(event.getMessageEvent());
        });

        addEventListener(ConsoleMessageSendEvent.class, event -> {
            onMessageEvent(event.getMessageEvent());
        });

        addEventListener(ConsoleWidgetUpdatedEvent.class, event -> {
            if (flow != null && flowDesigner != null) {
                Node node = flow.findNode(event.getNodeId());
                if (node != null) {
                    LOG.debug("Received console widget update, persistent state modified of: " + node);
                    node.setProperties(event.getProperties());

                    // Careful, this should not bounce back to the console!
                    dispatchEvent("#flowControl", new FlowUpdatedEvent(flow));
                    if (isFlowNodeOpen) {
                        dispatchEvent("#flowNode", new NodePropertiesRefreshEvent(node.getId()));
                    }
                }
            }
        });
    }

    @Override
    public void attached() {
        super.attached();
        initFlowDesigner();
    }

    public void createNode(String nodeType, double positionX, double positionY) {
        if (flowDesigner == null || flow == null)
            return;

        String flowId = flow.getId();

        sendRequest(
            false, false,
            resource("catalog", "node", nodeType).get(),
            new ObjectResponseCallback<Node>("Create node", NODE_CODEC) {
                @Override
                protected void onResponse(Node node) {
                    // Check if this is still the same flow designer instance as before the request
                    if (flowDesigner != null && flow != null && flow.getId().equals(flowId)) {

                        flow.addNode(node);
                        dispatchEvent(new FlowUpdatedEvent(flow));

                        // Calculate the offset with the current transform (zoom, panning)
                        // TODO If I would know maths, I could probably do this with the transform matrices
                        Transform currentTransform = flowDesignerPanel.getViewport().getAbsoluteTransform();
                        double x = (positionX - currentTransform.getTranslateX()) * currentTransform.getInverse().getScaleX();
                        double y = (positionY - currentTransform.getTranslateY()) * currentTransform.getInverse().getScaleY();
                        node.getEditorSettings().setPositionX(x);
                        node.getEditorSettings().setPositionY(y);

                        LOG.debug("Adding node shape to flow designer: " + node);
                        flowDesigner.addNodeShape(node);
                        dispatchEvent(new FlowDesignerNodeSelectedEvent(node));
                    }
                }
            }
        );
    }

    protected void duplicateNode(Node node) {
        if (flowDesigner == null || flow == null)
            return;

        final String flowId = flow.getId();
        int requiredIds = node.getSlots().length + 1;
        final List<String> generatedIds = new ArrayList<>();

        new IdGeneratorRunnable(
            flowId,
            requiredIds,
            generatedIds,
            new DuplicationRunnable(generatedIds, node)
        ).run();
    }

    protected class DuplicationRunnable implements Runnable {

        final List<String> generatedIds;
        final protected Node node;

        public DuplicationRunnable(List<String> generatedIds, Node node) {
            this.generatedIds = generatedIds;
            this.node = node;
        }

        @Override
        public void run() {
            LOG.debug("Duplicating node: " + node);

            Node dupe = NODE_CODEC.decode(NODE_CODEC.encode(node));

            Iterator<String> it = generatedIds.iterator();
            for (Slot slot : dupe.getSlots()) {
                slot.getIdentifier().setId(it.next());
            }
            dupe.getIdentifier().setId(it.next());

            if (dupe.getLabel() != null) {
                dupe.setLabel(dupe.getLabel() + " (Copy)");
            } else {
                dupe.setLabel("(Copy)");
            }

            dupe.getEditorSettings().setPositionX(dupe.getEditorSettings().getPositionX() + 20);
            dupe.getEditorSettings().setPositionY(dupe.getEditorSettings().getPositionY() + 20);

            flow.addNode(dupe);
            dispatchEvent(new FlowUpdatedEvent(flow));

            LOG.debug("Adding duplicated node shape to flow designer: " + dupe);
            flowDesigner.addNodeShape(dupe);

            dispatchEvent(new FlowDesignerNodeSelectedEvent(dupe));
        }
    }

    protected class IdGeneratorRunnable implements Runnable {

        final String flowId;
        final int requiredIds;
        final List<String> generatedIds;
        final protected Runnable duplicationRunnable;

        public IdGeneratorRunnable(String flowId, int requiredIds, List<String> generatedIds, Runnable duplicationRunnable) {
            this.flowId = flowId;
            this.requiredIds = requiredIds;
            this.generatedIds = generatedIds;
            this.duplicationRunnable = duplicationRunnable;
        }

        @Override
        public void run() {
            LOG.debug("Retrieving a batch of GUIDs to duplicate node");
            sendRequest(
                false, false,
                resource("catalog", "guid").get(),
                new ArrayResponseCallback("Generate batch of GUIDs") {
                    @Override
                    protected void onResponse(JSONArray array) {
                        // Check if this is still the same flow designer instance as before the request
                        if (flowDesigner != null && flow != null && flow.getId().equals(flowId)) {
                            for (int i = 0; i < array.size(); i++) {
                                generatedIds.add(array.get(i).isString().stringValue());
                            }
                            if (generatedIds.size() < requiredIds) {
                                LOG.debug("Still need more GUIDs: " + (requiredIds - generatedIds.size()));
                                new IdGeneratorRunnable(
                                    flowId, requiredIds, generatedIds, duplicationRunnable
                                ).run();
                            } else {
                                duplicationRunnable.run();
                            }
                        }
                    }
                }
            );
        }
    }

    protected void initFlowDesigner() {
        Element container = getRequiredElement("#flowDesigner");
        this.flowDesignerPanel = new LienzoPanel();

        flowDesignerPanel.setSelectCursor(Style.Cursor.MOVE);

        Window.addResizeHandler(event -> flowDesignerPanel.setPixelSize(container.getClientWidth(), container.getClientHeight()));
        flowDesignerPanel.setPixelSize(container.getClientWidth(), container.getClientHeight());

        // The viewport is "global", only add listeners once or you leak memory!
        flowDesignerPanel.getViewport().pushMediator(new FlowEditorViewportMediator());
        flowDesignerPanel.getViewport().addViewportTransformChangedHandler(event -> {
            if (flowDesigner != null) {
                flowDesigner.viewPortChanged();
            }
        });

        this.flowDesignerInitialTransform = flowDesignerPanel.getViewport().getTransform();

        // Needed for event propagation
        HTMLPanel containerPanel = HTMLPanel.wrap((com.google.gwt.dom.client.Element) container);
        containerPanel.add(flowDesignerPanel);
    }

    protected void startFlowDesigner() {

        flowDesignerPanel.getScene().removeAll();
        flowDesignerPanel.getViewport().setTransform(flowDesignerInitialTransform);

        flowDesigner = new FlowDesigner(flow, flowDesignerPanel.getScene()) {
            @Override
            protected void onSelection(Node node) {
                dispatchEvent(new FlowDesignerNodeSelectedEvent(node));
            }

            @Override
            protected void onMoved(Node node) {
                dispatchEvent(new FlowUpdatedEvent(flowDesigner.getFlow()));
            }

            @Override
            protected void onAddition(Wire wire) {
                dispatchEvent(new FlowUpdatedEvent(flowDesigner.getFlow()));
            }

            @Override
            protected void onRemoval(Wire wire) {
                dispatchEvent(new FlowUpdatedEvent(flowDesigner.getFlow()));
            }
        };
        flowDesignerPanel.draw();
    }

    protected void stopFlowDesigner() {
        flowDesignerPanel.getScene().removeAll();
        flowDesigner = null;
    }

    protected void onMessageEvent(MessageEvent event) {
        if (flowDesigner != null && flow != null) {
            Flow ownerFlow = flow.findOwnerFlowOfSlot(event.getSlotId());
            if (ownerFlow != null && ownerFlow.getId().equals(flow.getId())) {
                flowDesigner.receiveMessageEvent(event);
            }
        }
    }
}
