package org.openremote.beta.server.inventory.discovery;

import org.apache.camel.*;
import org.apache.camel.impl.ParameterConfiguration;
import org.apache.camel.impl.UriEndpointComponent;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.UriParam;
import org.apache.camel.util.CamelContextHelper;
import org.openremote.beta.server.util.RouteManagementUtil;
import org.openremote.beta.shared.inventory.Adapter;
import org.openremote.beta.shared.inventory.Identifier;
import org.openremote.beta.shared.inventory.Property;
import org.openremote.component.ValidationGroupDiscovery;
import org.openremote.devicediscovery.domain.DiscoveredDeviceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Properties;

public class AdapterDiscoveryService implements StaticService {

    private static final Logger LOG = LoggerFactory.getLogger(AdapterDiscoveryService.class);

    public static final String COMPONENT_TYPE = "org-openremote-component-type";
    public static final String COMPONENT_LABEL = "org-openremote-component-label";
    public static final String COMPONENT_DISCOVERY_ENDPOINT = "org-openremote-component-discoveryEndpoint";
    public static final String COMPONENT_DISCOVERY_ENDPOINT_REQ_PROPERTIES = "org-openremote-component-discoveryEndpointRequiredProperties";

    final protected CamelContext context;
    protected Adapter[] adapters;

    public AdapterDiscoveryService(CamelContext context) {
        this.context = context;
    }

    @Override
    public void start() throws Exception {
        this.adapters = findAdapters(context);
    }

    @Override
    public void stop() throws Exception {
    }

    public Adapter[] getAdapters() {
        return adapters;
    }

    public Adapter getAdapter(@Header("id") String id) {
        for (Adapter adapter : getAdapters()) {
            if (adapter.getIdentifier().getId().equals(id))
                return adapter;
        }
        return null;
    }

    public void updateDiscoveryRoute(Adapter adapter, boolean startRoute) throws Exception {
        String routeId = adapter.getIdentifier().getId() + "-discovery";
        RouteDefinition routeDefinition = createDiscoveryRoute(adapter, routeId);
        RouteManagementUtil.updateRoute(context, routeDefinition, startRoute);
    }

    public void triggerDiscovery(Adapter adapter) throws Exception {
        context.createProducerTemplate().sendBody(getDiscoveryUri(adapter), null);
    }

    protected RouteDefinition createDiscoveryRoute(Adapter adapter, String routeId) {
        return new RouteDefinition(getDiscoveryUri(adapter))
            .id(routeId)
            .autoStartup(false)
            .process(exchange -> {
                List<DiscoveredDeviceDTO> devices = exchange.getIn().getBody(List.class);
                context.hasService(InboxService.class).addDiscoveredDevices(devices);
            });
    }

    protected String getDiscoveryUri(Adapter adapter) {
        ComponentConfiguration discoveryConfig =
            context.getComponent(adapter.getIdentifier().getId()).createComponentConfiguration();
        discoveryConfig.setBaseUri(adapter.getDiscoveryEndpoint());
        for (Map.Entry<String, Property> entry : adapter.getExtra().entrySet()) {
            if (entry.getValue().hasValue()) {
                discoveryConfig.setParameter(entry.getKey(), entry.getValue().getValue());
            }
        }
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
        LOG.debug("Creating adapter for component: " + name);
        Class<? extends Endpoint> endpointClass = component.getEndpointClass();

        String label = componentProperties.containsKey(COMPONENT_LABEL)
            ? componentProperties.get(COMPONENT_LABEL).toString()
            : null;

        if (label == null)
            throw new RuntimeException("Component missing label property: " + name);

        String discoveryEndpoint = componentProperties.containsKey(COMPONENT_DISCOVERY_ENDPOINT)
            ? componentProperties.get(COMPONENT_DISCOVERY_ENDPOINT).toString()
            : null;

        String discoveryEndpointRequiredProperties = componentProperties.containsKey(COMPONENT_DISCOVERY_ENDPOINT_REQ_PROPERTIES)
            ? componentProperties.get(COMPONENT_DISCOVERY_ENDPOINT_REQ_PROPERTIES).toString()
            : null;
        Set<String> requiredProperties = new HashSet<>();
        if (discoveryEndpointRequiredProperties != null) {
            Collections.addAll(requiredProperties, discoveryEndpointRequiredProperties.split(" "));
        }

        Adapter adapter = new Adapter(label, new Identifier(name, componentType), discoveryEndpoint);

        ComponentConfiguration config = component.createComponentConfiguration();
        for (Map.Entry<String, ParameterConfiguration> configEntry : config.getParameterConfigurationMap().entrySet()) {
            try {
                Field field = endpointClass.getDeclaredField(configEntry.getKey());
                if (field.isAnnotationPresent(UriParam.class)) {
                    UriParam uriParam = field.getAnnotation(UriParam.class);

                    String propertyName = uriParam.name().length() != 0 ? uriParam.name() : field.getName();
                    Property property = new Property();

                    if (String.class.isAssignableFrom(field.getType())) {
                        property.setType(Property.Type.STRING);
                    } else if (Number.class.isAssignableFrom(field.getType())) {
                        property.setType(Property.Type.NUMBER);
                    } else if (Boolean.class.isAssignableFrom(field.getType())) {
                        property.setType(Property.Type.BOOLEAN);
                    } else {
                        throw new RuntimeException(
                            "Unsupported type of adapter endpoint property '" + name + "': " + field.getType()
                        );
                    }

                    if (field.isAnnotationPresent(NotNull.class)) {
                        for (Class<?> group : field.getAnnotation(NotNull.class).groups()) {
                            if (ValidationGroupDiscovery.class.isAssignableFrom(group)) {
                                property.setRequired(true);
                            }
                        }
                    }

                    property.setLabel(uriParam.label().length() > 0 ? uriParam.label() : null);
                    property.setDescription(uriParam.description().length() > 0 ? uriParam.description() : null);
                    property.setDefaultValue(uriParam.defaultValue().length() > 0 ? uriParam.defaultValue() : null);
                    property.setDefaultValueNote(uriParam.defaultValueNote().length() > 0 ? uriParam.defaultValueNote() : null);

                    adapter.getExtra().put(propertyName, property);
                }
            } catch (NoSuchFieldException ex) {
                // Ignoring config parameter if there is no annotated field on endpoint class
                // TODO: Inheritance of endpoint classes? Do we care?
            }
        }

        return adapter;
    }

}
