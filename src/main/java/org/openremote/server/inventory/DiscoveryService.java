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

package org.openremote.server.inventory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.*;
import org.apache.camel.impl.ParameterConfiguration;
import org.apache.camel.impl.UriEndpointComponent;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.UriParam;
import org.apache.camel.util.CamelContextHelper;
import org.openremote.devicediscovery.domain.DiscoveredDeviceAttrDTO;
import org.openremote.devicediscovery.domain.DiscoveredDeviceDTO;
import org.openremote.server.event.EventService;
import org.openremote.server.util.IdentifierUtil;
import org.openremote.shared.component.ValidationGroupDiscovery;
import org.openremote.shared.event.InventoryDevicesUpdatedEvent;
import org.openremote.shared.inventory.Adapter;
import org.openremote.shared.inventory.Device;
import org.seamless.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Properties;

// TODO Part of this class should be moved to zwave project
import static org.openremote.protocol.zwave.model.commandclasses.DeviceDiscoveryCommandClassVisitor.*;
import static org.openremote.server.util.JsonUtil.JSON;

public class DiscoveryService implements StaticService {

    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryService.class);

    public static final String COMPONENT_TYPE = "org-openremote-component-type";
    public static final String COMPONENT_LABEL = "org-openremote-component-label";
    public static final String COMPONENT_DISCOVERY_ENDPOINT = "org-openremote-component-discoveryEndpoint";

    public static final String DEVICE_PROPERTY_DISCOVERY_ENDPOINT = "discoveryEndpoint";

    final protected CamelContext context;
    final protected DeviceService deviceService;
    final protected DeviceLibraryService deviceLibraryService;

    final protected List<Adapter> adapters = new ArrayList<>();

    public DiscoveryService(CamelContext context, DeviceService deviceService, DeviceLibraryService deviceLibraryService) {
        this.context = context;
        this.deviceService = deviceService;
        this.deviceLibraryService = deviceLibraryService;
    }

    @Override
    public void start() throws Exception {
        // TODO Persistence! We should maintain adapter properties in the database, don't forget to clean database when an adapter is gone.
        this.adapters.clear();
        this.adapters.addAll(Arrays.asList(findAdapters(context)));
    }

    @Override
    public void stop() throws Exception {
    }

    public Adapter[] getAdapters() {
        synchronized (adapters) {
            return adapters.toArray(new Adapter[adapters.size()]);
        }
    }

    public Adapter getAdapter(@Header("id") String id) {
        synchronized (adapters) {
            for (Adapter adapter : getAdapters()) {
                if (adapter.getId().equals(id))
                    return adapter;
            }
            return null;
        }
    }

    public boolean putAdapter(Adapter adapter) {
        LOG.debug("Putting adapter: " + adapter);
        synchronized (adapters) {
            boolean found = false;
            Iterator<Adapter> it = adapters.iterator();
            while (it.hasNext()) {
                Adapter existingAdapter = it.next();
                if (existingAdapter.getId().equals(adapter.getId())) {
                    it.remove();
                    found = true;
                    break;
                }
            }
            if (found) {
                adapters.add(adapter);
            }
            return found;
        }
    }

    public void triggerDiscovery(Adapter adapter) {
        LOG.debug("Trigger discovery for adapter: " + adapter);
        String discoveryEndpointUri = createDiscoveryEndpointUri(adapter);

        RouteDefinition routeDefinition = createDiscoveryRoute(adapter, discoveryEndpointUri);
        String routeId = routeDefinition.getId();

        try {

            if (context.getRoute(routeId) == null) {
                try {
                    LOG.debug("Adding and starting discovery route: " + discoveryEndpointUri);
                    context.addRouteDefinition(routeDefinition);
                    context.startRoute(routeId);
                } catch (ConstraintViolationException ex) {
                    LOG.debug("Adapter endpoint constraint violation: " + ex);
                    StringBuilder sb = new StringBuilder();
                    for (ConstraintViolation<?> constraintViolation : ex.getConstraintViolations()) {
                        sb.append(constraintViolation.getPropertyPath())
                            .append(" => ")
                            .append(constraintViolation.getMessage());
                    }
                    throw new IllegalArgumentException("Missing properties: " + sb.toString());
                } catch (Exception ex) {
                    LOG.debug("Error starting route: " + discoveryEndpointUri, ex);
                    throw new IllegalStateException(ex);
                }
            }

            LOG.debug("Triggering discovery endpoint: " + discoveryEndpointUri);
            try {
                context.createProducerTemplate().sendBody(discoveryEndpointUri, null);
            } catch (Exception ex) {
                LOG.debug("Error triggering discovery: " + discoveryEndpointUri, ex);
                Throwable cause = Exceptions.unwrap(ex);
                throw new IllegalArgumentException(cause.getMessage());
            }

        } catch (RuntimeException ex) {
            // Must be able to retry later, so remove
            try {
                context.removeRoute(routeId);
            } catch (Exception e) {
                // Ignore
            }
            throw ex;
        }
    }

    protected RouteDefinition createDiscoveryRoute(Adapter adapter, String discoveryEndpointUri) {
        String routeId = "discovery-" + discoveryEndpointUri.hashCode();
        return new RouteDefinition(discoveryEndpointUri)
            .id(routeId)
            .autoStartup(false)
            .process(exchange -> {

                Object discoveryResult = exchange.getIn().getBody();
                if (discoveryResult instanceof List) {
                    List resultList = (List) discoveryResult;
                    if (resultList.size() == 0)
                        return;

                    LOG.debug("Processing discovered device list: " + resultList.size());
                    List<Device> devices = new ArrayList<>();

                    // TODO Ugly hack to support v2 and v3 device model
                    Object firstResult = resultList.get(0);
                    if (firstResult instanceof DiscoveredDeviceDTO) {
                        // V2
                        List<DiscoveredDeviceDTO> discoveredDeviceDTOs = exchange.getIn().getBody(List.class);

                        for (DiscoveredDeviceDTO deviceDTO : discoveredDeviceDTOs) {
                            List<DiscoveredDeviceAttrDTO> attributes = deviceDTO.getDeviceAttrs();

                            String discoveryCommand = null;
                            for (DiscoveredDeviceAttrDTO attribute : attributes) {
                                if (attribute.getName().equals(ATTR_NAME_DEVICE_DISCOVERY_COMMAND)) {
                                    discoveryCommand = attribute.getValue();
                                    break;
                                }
                            }

                            if (discoveryCommand == null) {
                                LOG.warn("Missing discovery command attribute, skipping discovered: " + deviceDTO);
                                continue;
                            }

                            Device converted = convertV2Device(adapter, deviceDTO, discoveryEndpointUri);

                            if (ATTR_VALUE_DEVICE_DISCOVERY_COMMAND_ADD.equals(discoveryCommand.toLowerCase(Locale.ROOT))) {
                                devices.add(converted);
                            } else if (ATTR_VALUE_DEVICE_DISCOVERY_COMMAND_DELETE.equals(discoveryCommand.toLowerCase(Locale.ROOT))) {
                                deviceService.setDeviceOffline(converted.getId());
                            } else {
                                LOG.warn("Unsupported discovery command attribute, skipping discovered: " + deviceDTO);
                            }
                        }

                    } else if (firstResult instanceof Device) {
                        // V3
                        devices = (List<Device>) exchange.getIn().getBody(List.class);
                    }

                    Device[] initializedDevices = deviceLibraryService.initializeDevices(adapter, devices);
                    deviceService.addDevices(initializedDevices);

                    EventService eventService = context.hasService(EventService.class);
                    if (eventService != null)
                        eventService.sendEvent(new InventoryDevicesUpdatedEvent());
                }
            });
    }

    protected String createDiscoveryEndpointUri(Adapter adapter) {
        ComponentConfiguration discoveryConfig =
            context.getComponent(adapter.getId()).createComponentConfiguration();
        discoveryConfig.setBaseUri(adapter.getDiscoveryEndpoint());
        try {
            ObjectNode properties = JSON.readValue(adapter.getProperties(), ObjectNode.class);
            Iterator<String> it = properties.fieldNames();
            while (it.hasNext()) {
                String propertyName = it.next();
                JsonNode property = properties.get(propertyName);
                if (property.hasNonNull("value")) {
                    discoveryConfig.setParameter(propertyName, property.get("value").asText());
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        LOG.debug("Using discovery URI: " + discoveryConfig.getUriString());
        return discoveryConfig.getUriString();
    }

    protected Adapter[] findAdapters(CamelContext context) throws Exception {
        List<Adapter> result = new ArrayList<>();
        for (Map.Entry<String, Properties> entry : CamelContextHelper.findComponents(context).entrySet()) {
            Object componentType = entry.getValue().get(COMPONENT_TYPE);
            if (componentType != null) {
                Component component = context.getComponent(entry.getKey());
                if (component == null) {
                    throw new IllegalStateException("Configured component not available in context: " + entry.getKey());
                }
                if (component instanceof UriEndpointComponent) {
                    UriEndpointComponent uriEndpointComponent = (UriEndpointComponent) component;
                    result.add(createAdapter(
                        entry.getKey(),
                        componentType.toString(),
                        uriEndpointComponent,
                        entry.getValue()
                    ));
                } else {
                    LOG.warn(
                        "Component should implement " +
                            UriEndpointComponent.class.getName() + ": " + entry.getKey()
                    );
                }
            }
        }
        return result.toArray(new Adapter[result.size()]);
    }

    protected Adapter createAdapter(String name, String componentType, UriEndpointComponent component, Properties componentProperties) {
        LOG.info("Creating adapter for component: " + name);
        Class<? extends Endpoint> endpointClass = component.getEndpointClass();

        String label = componentProperties.containsKey(COMPONENT_LABEL)
            ? componentProperties.get(COMPONENT_LABEL).toString()
            : null;

        if (label == null)
            throw new RuntimeException("Component missing label property: " + name);

        String discoveryEndpoint = componentProperties.containsKey(COMPONENT_DISCOVERY_ENDPOINT)
            ? componentProperties.get(COMPONENT_DISCOVERY_ENDPOINT).toString()
            : null;

        Adapter adapter = new Adapter(label, name, componentType, discoveryEndpoint);

        ComponentConfiguration config = component.createComponentConfiguration();
        ObjectNode properties = JSON.createObjectNode();
        for (Map.Entry<String, ParameterConfiguration> configEntry : config.getParameterConfigurationMap().entrySet()) {
            try {
                Field field = endpointClass.getDeclaredField(configEntry.getKey());
                if (field.isAnnotationPresent(UriParam.class)) {
                    UriParam uriParam = field.getAnnotation(UriParam.class);

                    ObjectNode property = JSON.createObjectNode();

                    if (uriParam.label().length() > 0) {
                        property.put("label", uriParam.label());
                    }

                    if (uriParam.description().length() > 0) {
                        property.put("description", uriParam.description());

                    }

                    if (uriParam.defaultValue().length() > 0) {
                        property.put("defaultValue", uriParam.defaultValue());
                    }

                    if (uriParam.defaultValueNote().length() > 0) {
                        property.put("defaultValueNote", uriParam.defaultValueNote());
                    }

                    if (String.class.isAssignableFrom(field.getType())) {
                        property.put("type", "string");
                    } else if (Long.class.isAssignableFrom(field.getType())) {
                        property.put("type", "long");
                    } else if (Integer.class.isAssignableFrom(field.getType())) {
                        property.put("type", "integer");
                    } else if (Double.class.isAssignableFrom(field.getType())) {
                        property.put("type", "double");
                    } else if (Boolean.class.isAssignableFrom(field.getType())) {
                        property.put("type", "boolean");
                    } else {
                        throw new RuntimeException(
                            "Unsupported type of adapter endpoint property '" + name + "': " + field.getType()
                        );
                    }

                    if (field.isAnnotationPresent(NotNull.class)) {
                        for (Class<?> group : field.getAnnotation(NotNull.class).groups()) {
                            if (ValidationGroupDiscovery.class.isAssignableFrom(group)) {
                                property.put("required", true);
                                break;
                            }
                        }
                    }

                    String propertyName = uriParam.name().length() != 0 ? uriParam.name() : field.getName();
                    LOG.debug("Adding adapter property '" + propertyName + "': " + property);
                    properties.set(propertyName, property);
                }
            } catch (NoSuchFieldException ex) {
                // Ignoring config parameter if there is no annotated field on endpoint class
                // TODO: Inheritance of endpoint classes? Do we care?
            }
        }

        try {
            adapter.setProperties(JSON.writeValueAsString(properties));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return adapter;
    }

    protected Device convertV2Device(Adapter adapter, DiscoveredDeviceDTO deviceDTO, String discoveryEndpointUri) {
        List<DiscoveredDeviceAttrDTO> attributes = deviceDTO.getDeviceAttrs();

        String deviceId = null;
        // TODO: Will all protocols have an identifier attribute?
        for (DiscoveredDeviceAttrDTO attribute : attributes) {
            if (ATTR_NAME_NODE_ID.equals(attribute.getName())) {
                deviceId = attribute.getValue();
                break;
            }
        }
        if (deviceId == null) {
            LOG.warn("Missing device identifier attribute, skipping discovered: " + deviceDTO);
            return null;
        }

        String isRootDevice = null;
        // TODO: Will all protocols have this attribute?
        for (DiscoveredDeviceAttrDTO attribute : attributes) {
            if (ATTR_NAME_IS_ROOT.equals(attribute.getName())) {
                isRootDevice = attribute.getValue();
                break;
            }
        }

        String deviceLabel =
            deviceDTO.getName() != null && deviceDTO.getName().length() > 0
                ? deviceDTO.getName()
                : deviceId;

        // TODO How do we get a stable unique ID?
        try {
            deviceId = IdentifierUtil.getEncodedHash(
                adapter.getId().getBytes("utf-8"),
                deviceId.getBytes("utf-8"),
                deviceLabel.getBytes("utf-8"),
                isRootDevice != null ? isRootDevice.getBytes("utf-8") : new byte[0]
            );
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        };

        Device device = new Device(
            deviceLabel,
            deviceId,
            "urn:openremote:adapter:" + adapter.getId()
        );

        device.setStatus(Device.Status.UNINITIALIZED);

        ObjectNode properties = JSON.createObjectNode();

        properties.put(DEVICE_PROPERTY_DISCOVERY_ENDPOINT, discoveryEndpointUri);

        for (DiscoveredDeviceAttrDTO attribute : attributes) {
            if (ATTR_NAME_DEVICE_DISCOVERY_COMMAND.equals(attribute.getName()))
                continue;
            if (attribute.getName() != null && attribute.getValue() != null) {
                properties.put(attribute.getName(), attribute.getValue());
            }
        }

        try {
            device.setProperties(JSON.writeValueAsString(properties));
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }

        return device;
    }

}
