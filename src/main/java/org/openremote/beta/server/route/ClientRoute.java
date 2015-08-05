package org.openremote.beta.server.route;

import org.apache.camel.CamelContext;
import org.apache.camel.model.RouteDefinition;
import org.openremote.beta.shared.flow.Flow;
import org.openremote.beta.shared.flow.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientRoute extends NodeRoute {

    private static final Logger LOG = LoggerFactory.getLogger(ClientRoute.class);

    public ClientRoute(CamelContext context, Flow flow, Node node) {
        super(context, flow, node);
    }

    @Override
    protected void configureProcessing(RouteDefinition routeDefinition) throws Exception {
        // Do nothing
    }

    @Override
    protected boolean isServerRoutingEnabled() {
        // If there is another node wired with this client node, and it's not a client node, we need
        // a server-side route. This means the boundaries of the flow graph handled by the server are
        // client nodes. Any client nodes wired to exclusively other client nodes are handled
        // exclusively on client-side.
        return getFlow().isNodeWiredToNodeOfType(getNode(), Node.TYPE_CLIENT);
    }

    @Override
    protected boolean isPublishingMessageEvents() {
        return true;
    }

}
