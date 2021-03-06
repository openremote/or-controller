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

package org.openremote.server.web;

import io.undertow.server.HttpHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.*;
import io.undertow.websockets.jsr.DefaultContainerConfigurator;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.openremote.shared.Constants;
import org.openremote.server.web.socket.WebsocketAdapter;
import org.openremote.server.web.socket.WebsocketCORSFilter;
import org.openremote.server.web.socket.WebsocketComponent;
import org.openremote.server.web.socket.WebsocketConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.websocket.server.ServerEndpointConfig;
import java.util.Map;

public class UndertowWebsocketComponent extends WebsocketComponent {

    private static final Logger LOG = LoggerFactory.getLogger(UndertowWebsocketComponent.class);

    final protected ServletContainer servletContainer = Servlets.defaultContainer();
    protected DeploymentInfo deploymentInfo;
    protected DeploymentManager deploymentManager;

    protected UndertowService getUndertowService() {
        UndertowService undertowService = getCamelContext().hasService(UndertowService.class);
        if (undertowService == null)
            throw new IllegalStateException("Please configure and add " + UndertowService.class.getName());
        return undertowService;
    }

    @Override
    protected void deploy() throws Exception {
        LOG.info("Deploying websocket endpoints: " + getConsumers().keySet());

        WebSocketDeploymentInfo websocketDeploymentInfo = new WebSocketDeploymentInfo();

        for (Map.Entry<String, WebsocketConsumer> entry : getConsumers().entrySet()) {
            websocketDeploymentInfo.addEndpoint(
                ServerEndpointConfig.Builder.create(WebsocketAdapter.class, "/" + entry.getKey())
                    .configurator(new DefaultContainerConfigurator() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                            return (T) new WebsocketAdapter(entry.getValue());
                        }
                    })
                    .build()
            );
        }

        deploymentInfo = new DeploymentInfo()
            .setDeploymentName("WebSocket Deployment")
            .setContextPath(Constants.WEBSOCKET_SERVICE_CONTEXT_PATH)
            .addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, websocketDeploymentInfo)
            .setClassLoader(WebsocketComponent.class.getClassLoader());

        // TODO Per-endpoint CORS filter
        if (getAllowedOrigin() != null) {
            WebsocketCORSFilter websocketCORSFilter = new WebsocketCORSFilter();
            FilterInfo filterInfo = new FilterInfo("WebSocket CORS Filter", WebsocketCORSFilter.class, () -> new InstanceHandle<Filter>() {
                @Override
                public Filter getInstance() {
                    return websocketCORSFilter;
                }

                @Override
                public void release() {
                }
            }).addInitParam(WebsocketCORSFilter.ALLOWED_ORIGIN, getAllowedOrigin());
            deploymentInfo.addFilter(filterInfo);
            deploymentInfo.addFilterUrlMapping(filterInfo.getName(), "/*", DispatcherType.REQUEST);
        }

        deploymentManager = servletContainer.addDeployment(deploymentInfo);
        deploymentManager.deploy();

        HttpHandler handler = deploymentManager.start();

        getUndertowService().getPathHandler().addPrefixPath(Constants.WEBSOCKET_SERVICE_CONTEXT_PATH, handler);
    }

    @Override
    protected void undeploy() throws Exception {
        if (deploymentManager != null) {
            deploymentManager.stop();
            deploymentManager.undeploy();
            servletContainer.removeDeployment(deploymentInfo);
            getUndertowService().getPathHandler().removePrefixPath(Constants.WEBSOCKET_SERVICE_CONTEXT_PATH);

            deploymentInfo = null;
            deploymentManager = null;
        }
    }
}
