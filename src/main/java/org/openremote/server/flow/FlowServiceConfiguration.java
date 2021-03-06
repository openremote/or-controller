/*
 * Copyright 2015, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.openremote.server.flow;

import org.apache.camel.CamelContext;
import org.openremote.server.Configuration;
import org.openremote.server.Environment;
import org.openremote.server.route.procedure.FlowProcedureException;
import org.openremote.server.web.WebserverConfiguration.RestRouteBuilder;
import org.openremote.server.inventory.ClientPresetService;
import org.openremote.server.route.RouteManagementService;
import org.openremote.shared.flow.Flow;
import org.openremote.shared.flow.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.camel.Exchange.HTTP_RESPONSE_CODE;
import static org.openremote.server.Environment.DEV_MODE;
import static org.openremote.server.Environment.DEV_MODE_DEFAULT;

public class FlowServiceConfiguration implements Configuration {

    private static final Logger LOG = LoggerFactory.getLogger(FlowServiceConfiguration.class);

    class FlowServiceRouteBuilder extends RestRouteBuilder {

        public FlowServiceRouteBuilder(boolean debug) {
            super(debug);
        }

        @Override
        public void configure() throws Exception {
            super.configure();

            rest("/flow")

                .get()
                .route().id("GET all flows")
                .bean(getContext().hasService(FlowService.class), "getFlows")
                .endRest()

                .post()
                .consumes("application/json")
                .type(Flow.class)
                .route().id("POST new flow")
                .bean(getContext().hasService(FlowService.class), "postFlow")
                .setHeader(HTTP_RESPONSE_CODE, constant(201))
                .endRest()

                .get("/template")
                .route().id("GET flow template")
                .bean(getContext().hasService(FlowService.class), "getFlowTemplate")
                .endRest()

                .get("/preset")
                .route().id("GET flow by preset")
                .bean(getContext().hasService(FlowService.class), "getPresetFlow")
                .to("direct:restStatusNotFound")
                .endRest()

                .get("/{id}/subflow")
                .route().id("GET new subflow node by ID")
                .bean(getContext().hasService(FlowService.class), "createSubflowNode")
                .to("direct:restStatusNotFound")
                .endRest()

                .get("{id}")
                .route().id("GET flow by ID")
                .bean(getContext().hasService(FlowService.class), "getFlow")
                .to("direct:restStatusNotFound")
                .endRest()

                .post("/duplicate/node")
                .consumes("application/json")
                .type(Node.class)
                .route().id("POST node to duplicate")
                .process(exchange -> {
                    Node node = exchange.getIn().getBody(Node.class);
                    if (node != null) {
                        getContext().hasService(FlowService.class).resetCopy(node);
                        exchange.getOut().setBody(node);
                        exchange.getOut().setHeader(HTTP_RESPONSE_CODE, 200);
                    } else {
                        exchange.getOut().setHeader(HTTP_RESPONSE_CODE, 204);
                    }
                })
                .endRest()

                .post("/resolve")
                .consumes("application/json")
                .type(Flow.class)
                .route().id("POST flow to resolve its dependencies")
                .process(exchange -> {
                    Flow flow = exchange.getIn().getBody(Flow.class);
                    if (flow != null) {
                        boolean hydrateSubs = exchange.getIn().getHeader("hydrateSubs", false, Boolean.class);

                        exchange.getOut().setBody(
                            getContext().hasService(FlowService.class).getResolvedFlow(flow, hydrateSubs)
                        );
                        exchange.getOut().setHeader(HTTP_RESPONSE_CODE, 200);
                    } else {
                        exchange.getOut().setHeader(HTTP_RESPONSE_CODE, 204);
                    }
                })
                .endRest()

                .delete("{id}")
                .route().id("DELETE flow by ID")
                .process(exchange -> {
                    String flowId = exchange.getIn().getHeader("id", String.class);
                    try {
                        FlowService flowService = getContext().hasService(FlowService.class);
                        flowService.deleteFlow(flowId);
                        exchange.getOut().setHeader(HTTP_RESPONSE_CODE, 204);
                    } catch (RuntimeException ex) {
                        if (ex.getCause() != null && ex.getCause() instanceof FlowProcedureException) {
                            LOG.debug("Error deleting/stopping flow '" + flowId + "'", ex);
                            exchange.getIn().setBody("Error stopping flow '" + flowId + ":" + ex.getMessage());
                            exchange.getIn().setHeader(HTTP_RESPONSE_CODE, 409);
                        }
                    }
                })
                .endRest()

                .put("/{id}")
                .consumes("application/json")
                .type(Flow.class)
                .route().id("PUT flow by ID")
                .process(exchange -> {
                    Flow flow = exchange.getIn().getBody(Flow.class);
                    boolean found = getContext().hasService(FlowService.class).putFlow(flow);
                    exchange.getOut().setHeader(HTTP_RESPONSE_CODE, found ? 204 : 404);
                })
                .endRest();
        }
    }

    @Override
    public void apply(Environment environment, CamelContext context) throws Exception {

        FlowService flowService = new FlowService(
            context,
            context.hasService(RouteManagementService.class),
            context.hasService(ClientPresetService.class)
        );
        context.addService(flowService);

        context.addRoutes(
            new FlowServiceRouteBuilder(
                Boolean.valueOf(environment.getProperty(DEV_MODE, DEV_MODE_DEFAULT))
            )
        );
    }

}
