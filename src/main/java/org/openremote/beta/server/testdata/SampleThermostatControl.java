package org.openremote.beta.server.testdata;

import org.openremote.beta.server.catalog.filter.FilterNodeDescriptor;
import org.openremote.beta.server.catalog.function.FunctionNodeDescriptor;
import org.openremote.beta.server.catalog.widget.PushButtonNodeDescriptor;
import org.openremote.beta.server.catalog.widget.TextLabelNodeDescriptor;
import org.openremote.beta.server.route.ConsumerRoute;
import org.openremote.beta.server.route.ProducerRoute;
import org.openremote.beta.shared.flow.*;
import org.openremote.beta.shared.model.Identifier;
import org.openremote.beta.shared.model.Properties;
import org.openremote.beta.shared.widget.PushButton;
import org.openremote.beta.shared.widget.TextLabel;
import org.openremote.beta.shared.widget.Widget;

import java.io.IOException;
import java.util.Map;

import static org.openremote.beta.server.util.IdentifierUtil.generateGlobalUniqueId;
import static org.openremote.beta.server.util.JsonUtil.JSON;
import static org.openremote.beta.shared.flow.Node.TYPE_SUBFLOW;
import static org.openremote.beta.shared.flow.Slot.TYPE_SINK;
import static org.openremote.beta.shared.flow.Slot.TYPE_SOURCE;

public class SampleThermostatControl {

    /* ###################################################################################### */

    public static Slot TEMPERATURE_CONSUMER_SOURCE = new Slot(new Identifier(generateGlobalUniqueId(), TYPE_SOURCE));
    public static Slot TEMPERATURE_CONSUMER_SINK = new Slot("Temperature", new Identifier(generateGlobalUniqueId(), TYPE_SINK), false);
    public static Node TEMPERATURE_CONSUMER = new Node("Temperature", new Identifier(generateGlobalUniqueId(), ConsumerRoute.NODE_TYPE), TEMPERATURE_CONSUMER_SINK, TEMPERATURE_CONSUMER_SOURCE);

    static {
        Map<String, Object> properties = Properties.create();
        properties.put("clientAccess", "true");
        Map<String, Object> editor = Properties.create(properties, "editor");
        editor.put("x", 20);
        editor.put("y", 20);
        editor.put("color", NodeColor.VIRTUAL);
        editor.put("typeLabel", ConsumerRoute.NODE_TYPE_LABEL);
        TEMPERATURE_CONSUMER.setProperties(properties);
    }

    /* ###################################################################################### */

    public static Slot SETPOINT_CONSUMER_SOURCE = new Slot(new Identifier(generateGlobalUniqueId(), TYPE_SOURCE));
    public static Slot SETPOINT_CONSUMER_SINK = new Slot("Setpoint", new Identifier(generateGlobalUniqueId(), TYPE_SINK), false);
    public static Node SETPOINT_CONSUMER = new Node("Setpoint", new Identifier(generateGlobalUniqueId(), ConsumerRoute.NODE_TYPE), SETPOINT_CONSUMER_SINK, SETPOINT_CONSUMER_SOURCE);

    static {
        Map<String, Object> properties = Properties.create();
        properties.put("clientAccess", "true");
        Map<String, Object> editor = Properties.create(properties, "editor");
        editor.put("x", 20);
        editor.put("y", 200);
        editor.put("color", NodeColor.VIRTUAL);
        editor.put("typeLabel", ConsumerRoute.NODE_TYPE_LABEL);
        SETPOINT_CONSUMER.setProperties(properties);
    }

    /* ###################################################################################### */

    public static Slot TEMPERATURE_PROCESSOR_FLOW_FAHRENHEIT_SINK = new Slot(generateGlobalUniqueId(), SampleTemperatureProcessor.FAHRENHEIT_CONSUMER_SINK);
    public static Slot TEMPERATURE_PROCESSOR_FLOW_CELCIUS_SOURCE = new Slot(generateGlobalUniqueId(), SampleTemperatureProcessor.CELCIUS_PRODUCER_SOURCE);
    public static Slot TEMPERATURE_PROCESSOR_FLOW_LABEL_SOURCE = new Slot(generateGlobalUniqueId(), SampleTemperatureProcessor.LABEL_PRODUCER_SOURCE);
    public static Node TEMPERATURE_PROCESSOR_FLOW = new Node(SampleTemperatureProcessor.FLOW.getLabel(), new Identifier(generateGlobalUniqueId(), TYPE_SUBFLOW), TEMPERATURE_PROCESSOR_FLOW_FAHRENHEIT_SINK, TEMPERATURE_PROCESSOR_FLOW_CELCIUS_SOURCE, TEMPERATURE_PROCESSOR_FLOW_LABEL_SOURCE);

    static {
        Map<String, Object> properties = Properties.create();
        Map<String, Object> editor = Properties.create(properties, "editor");
        editor.put("x", 300);
        editor.put("y", 20);
        editor.put("color", NodeColor.VIRTUAL);
        editor.put("typeLabel", Node.TYPE_SUBFLOW_LABEL);
        TEMPERATURE_PROCESSOR_FLOW.setProperties(properties);
    }

    /* ###################################################################################### */

    public static Slot SETPOINT_PROCESSOR_FLOW_FAHRENHEIT_SINK = new Slot(generateGlobalUniqueId(), SampleTemperatureProcessor.FAHRENHEIT_CONSUMER_SINK);
    public static Slot SETPOINT_PROCESSOR_FLOW_CELCIUS_SOURCE = new Slot(generateGlobalUniqueId(), SampleTemperatureProcessor.CELCIUS_PRODUCER_SOURCE);
    public static Slot SETPOINT_PROCESSOR_FLOW_LABEL_SOURCE = new Slot(generateGlobalUniqueId(), SampleTemperatureProcessor.LABEL_PRODUCER_SOURCE);
    public static Node SETPOINT_PROCESSOR_FLOW = new Node(SampleTemperatureProcessor.FLOW.getLabel(), new Identifier(generateGlobalUniqueId(), TYPE_SUBFLOW), SETPOINT_PROCESSOR_FLOW_FAHRENHEIT_SINK, SETPOINT_PROCESSOR_FLOW_CELCIUS_SOURCE, SETPOINT_PROCESSOR_FLOW_LABEL_SOURCE);

    static {
        Map<String, Object> properties = Properties.create();
        Map<String, Object> editor = Properties.create(properties, "editor");
        editor.put("x", 300);
        editor.put("y", 150);
        editor.put("color", NodeColor.VIRTUAL);
        editor.put("typeLabel", Node.TYPE_SUBFLOW_LABEL);
        SETPOINT_PROCESSOR_FLOW.setProperties(properties);
    }

    /* ###################################################################################### */

    public static Slot TEMPERATURE_LABEL_SINK = new Slot(new Identifier(generateGlobalUniqueId(), TYPE_SINK));
    public static Node TEMPERATURE_LABEL = new Node("Temperature Label", new Identifier(generateGlobalUniqueId(), TextLabelNodeDescriptor.TYPE), TEMPERATURE_LABEL_SINK);

    static {
        Map<String, Object> properties = Properties.create();
        properties.put("clientAccess", "true");
        properties.put("postEndpoint", "mock:labelTemperature");
        Map<String, Object> editor = Properties.create(properties, "editor");
        editor.put("x", 750);
        editor.put("y", 50);
        editor.put("color", NodeColor.CLIENT);
        editor.put("typeLabel", TextLabelNodeDescriptor.TYPE_LABEL);
        TEMPERATURE_LABEL.setProperties(properties);

        Map<String, Object> widgetProperties =
            Widget.configureProperties(TEMPERATURE_LABEL, TextLabel.TYPE, TextLabel.COMPONENT);
        widgetProperties.put("positionX", 0);
        widgetProperties.put("positionY", 0);
        widgetProperties.put("color", "#ff0000");
    }

    /* ###################################################################################### */

    public static Slot SETPOINT_LABEL_SINK = new Slot(new Identifier(generateGlobalUniqueId(), TYPE_SINK));
    public static Node SETPOINT_LABEL = new Node("Setpoint Label", new Identifier(generateGlobalUniqueId(), TextLabelNodeDescriptor.TYPE), SETPOINT_LABEL_SINK);

    static {
        Map<String, Object> properties = Properties.create();
        properties.put("clientAccess", "true");
        properties.put("postEndpoint", "mock:labelSetpoint");
        Map<String, Object> editor = Properties.create(properties, "editor");
        editor.put("x", 750);
        editor.put("y", 150);
        editor.put("color", NodeColor.CLIENT);
        editor.put("typeLabel", TextLabelNodeDescriptor.TYPE_LABEL);
        SETPOINT_LABEL.setProperties(properties);

        Map<String, Object> widgetProperties =
            Widget.configureProperties(SETPOINT_LABEL, TextLabel.TYPE, TextLabel.COMPONENT);
        widgetProperties.put("positionX", 0);
        widgetProperties.put("positionY", 25);
        widgetProperties.put("color", "#0000ff");
    }

    /* ###################################################################################### */

    public static Slot SETPOINT_PLUS_BUTTON_SOURCE = new Slot(new Identifier(generateGlobalUniqueId(), TYPE_SOURCE));
    public static Slot SETPOINT_PLUS_BUTTON_SINK = new Slot(new Identifier(generateGlobalUniqueId(), TYPE_SINK), false);
    public static Node SETPOINT_PLUS_BUTTON = new Node("Increase Temperature Button", new Identifier(generateGlobalUniqueId(), PushButtonNodeDescriptor.TYPE), SETPOINT_PLUS_BUTTON_SOURCE, SETPOINT_PLUS_BUTTON_SINK);

    static {
        Map<String, Object> properties = Properties.create();
        properties.put("clientAccess", "true");
        Map<String, Object> editor = Properties.create(properties, "editor");
        editor.put("x", 50);
        editor.put("y", 400);
        editor.put("color", NodeColor.CLIENT);
        editor.put("typeLabel", PushButtonNodeDescriptor.TYPE_LABEL);
        SETPOINT_PLUS_BUTTON.setProperties(properties);

        Map<String, Object> widgetProperties =
            Widget.configureProperties(SETPOINT_PLUS_BUTTON, PushButton.TYPE, PushButton.COMPONENT);
        widgetProperties.put("positionX", 150);
        widgetProperties.put("positionY", 50);
    }

    /* ###################################################################################### */

    public static Slot SETPOINT_MINUS_BUTTON_SOURCE = new Slot(new Identifier(generateGlobalUniqueId(), TYPE_SOURCE));
    public static Slot SETPOINT_MINUS_BUTTON_SINK = new Slot(new Identifier(generateGlobalUniqueId(), TYPE_SINK), false);
    public static Node SETPOINT_MINUS_BUTTON = new Node("Decrease Temperature Button", new Identifier(generateGlobalUniqueId(), PushButtonNodeDescriptor.TYPE), SETPOINT_MINUS_BUTTON_SOURCE, SETPOINT_MINUS_BUTTON_SINK);

    static {
        Map<String, Object> properties = Properties.create();
        properties.put("clientAccess", "true");
        Map<String, Object> editor = Properties.create(properties, "editor");
        editor.put("x", 50);
        editor.put("y", 500);
        editor.put("color", NodeColor.CLIENT);
        editor.put("typeLabel", PushButtonNodeDescriptor.TYPE_LABEL);
        SETPOINT_MINUS_BUTTON.setProperties(properties);

        Map<String, Object> widgetProperties =
            Widget.configureProperties(SETPOINT_MINUS_BUTTON, PushButton.TYPE, PushButton.COMPONENT);
        widgetProperties.put("positionX", 0);
        widgetProperties.put("positionY", 50);
    }

    /* ###################################################################################### */

    public static Slot SETPOINT_PLUS_FILTER_SINK = new Slot(new Identifier(generateGlobalUniqueId(), TYPE_SINK));
    public static Slot SETPOINT_PLUS_FILTER_TRIGGER_SINK = new Slot("Trigger", new Identifier(generateGlobalUniqueId(), TYPE_SINK));
    public static Slot SETPOINT_PLUS_FILTER_SOURCE = new Slot(new Identifier(generateGlobalUniqueId(), TYPE_SOURCE));
    public static Node SETPOINT_PLUS_FILTER = new Node("Forward on trigger", new Identifier(generateGlobalUniqueId(), FilterNodeDescriptor.TYPE), SETPOINT_PLUS_FILTER_SINK, SETPOINT_PLUS_FILTER_TRIGGER_SINK, SETPOINT_PLUS_FILTER_SOURCE);

    static {
        Map<String, Object> properties = Properties.create();
        properties.put("onTrigger", "true");
        Map<String, Object> editor = Properties.create(properties, "editor");
        editor.put("x", 450);
        editor.put("y", 300);
        editor.put("color", NodeColor.DEFAULT);
        editor.put("typeLabel", FilterNodeDescriptor.TYPE_LABEL);
        SETPOINT_PLUS_FILTER.setProperties(properties);
    }

    /* ###################################################################################### */

    public static Slot SETPOINT_MINUS_FILTER_SINK = new Slot(new Identifier(generateGlobalUniqueId(), TYPE_SINK));
    public static Slot SETPOINT_MINUS_FILTER_TRIGGER_SINK = new Slot("Trigger", new Identifier(generateGlobalUniqueId(), TYPE_SINK));
    public static Slot SETPOINT_MINUS_FILTER_SOURCE = new Slot(new Identifier(generateGlobalUniqueId(), TYPE_SOURCE));
    public static Node SETPOINT_MINUS_FILTER = new Node("Forward on trigger", new Identifier(generateGlobalUniqueId(), FilterNodeDescriptor.TYPE), SETPOINT_MINUS_FILTER_SINK, SETPOINT_MINUS_FILTER_TRIGGER_SINK, SETPOINT_MINUS_FILTER_SOURCE);

    static {
        Map<String, Object> properties = Properties.create();
        properties.put("onTrigger", "true");
        Map<String, Object> editor = Properties.create(properties, "editor");
        editor.put("x", 450);
        editor.put("y", 450);
        editor.put("color", NodeColor.DEFAULT);
        editor.put("typeLabel", FilterNodeDescriptor.TYPE_LABEL);
        SETPOINT_MINUS_FILTER.setProperties(properties);
    }

    /* ###################################################################################### */

    public static Slot SETPOINT_INCREMENT_FUNCTION_SINK = new Slot(new Identifier(generateGlobalUniqueId(), TYPE_SINK));
    public static Slot SETPOINT_INCREMENT_FUNCTION_SOURCE = new Slot(new Identifier(generateGlobalUniqueId(), TYPE_SOURCE));
    public static Node SETPOINT_INCREMENT_FUNCTION = new Node("Increment by 1", new Identifier(generateGlobalUniqueId(), FunctionNodeDescriptor.TYPE), SETPOINT_INCREMENT_FUNCTION_SINK, SETPOINT_INCREMENT_FUNCTION_SOURCE);

    static {
        Map<String, Object> properties = Properties.create();
        properties.put("javascript", "output['value'] = input + 1");
        Map<String, Object> editor = Properties.create(properties, "editor");
        editor.put("x", 750);
        editor.put("y", 300);
        editor.put("color", NodeColor.DEFAULT);
        editor.put("typeLabel", FunctionNodeDescriptor.TYPE_LABEL);
        SETPOINT_INCREMENT_FUNCTION.setProperties(properties);
    }

    /* ###################################################################################### */

    public static Slot SETPOINT_DECREMENT_FUNCTION_SINK = new Slot(new Identifier(generateGlobalUniqueId(), TYPE_SINK));
    public static Slot SETPOINT_DECREMENT_FUNCTION_SOURCE = new Slot(new Identifier(generateGlobalUniqueId(), TYPE_SOURCE));
    public static Node SETPOINT_DECREMENT_FUNCTION = new Node("Decrement by 1", new Identifier(generateGlobalUniqueId(), FunctionNodeDescriptor.TYPE), SETPOINT_DECREMENT_FUNCTION_SINK, SETPOINT_DECREMENT_FUNCTION_SOURCE);

    static {
        Map<String, Object> properties = Properties.create();
        properties.put("javascript", "output['value'] = input - 1");
        Map<String, Object> editor = Properties.create(properties, "editor");
        editor.put("x", 750);
        editor.put("y", 450);
        editor.put("color", NodeColor.DEFAULT);
        editor.put("typeLabel", FunctionNodeDescriptor.TYPE_LABEL);
        SETPOINT_DECREMENT_FUNCTION.setProperties(properties);
    }

    /* ###################################################################################### */

    public static Slot SETPOINT_PRODUCER_SINK = new Slot(new Identifier(generateGlobalUniqueId(), TYPE_SINK));
    public static Slot SETPOINT_PRODUCER_SOURCE = new Slot("Setpoint", new Identifier(generateGlobalUniqueId(), TYPE_SOURCE), false);
    public static Node SETPOINT_PRODUCER = new Node("Setpoint", new Identifier(generateGlobalUniqueId(), ProducerRoute.NODE_TYPE), SETPOINT_PRODUCER_SOURCE, SETPOINT_PRODUCER_SINK);

    static {
        Map<String, Object> properties = Properties.create();
        properties.put("clientAccess", "true");
        properties.put("postEndpoint", "mock:producerSetpoint");
        Map<String, Object> editor = Properties.create(properties, "editor");
        editor.put("x", 1050);
        editor.put("y", 375);
        editor.put("color", NodeColor.VIRTUAL);
        editor.put("typeLabel", ProducerRoute.NODE_TYPE_LABEL);
        SETPOINT_PRODUCER.setProperties(properties);
    }

    /* ###################################################################################### */

    public static Node[] FLOW_NODES = new Node[]{
        TEMPERATURE_CONSUMER,
        SETPOINT_CONSUMER,
        TEMPERATURE_LABEL,
        SETPOINT_LABEL,
        SETPOINT_PLUS_BUTTON,
        SETPOINT_MINUS_BUTTON,
        SETPOINT_PLUS_FILTER,
        SETPOINT_MINUS_FILTER,
        SETPOINT_INCREMENT_FUNCTION,
        SETPOINT_DECREMENT_FUNCTION,
        SETPOINT_PRODUCER,
        TEMPERATURE_PROCESSOR_FLOW,
        SETPOINT_PROCESSOR_FLOW
    };

    /* ###################################################################################### */

    public static Flow FLOW = new Flow(
        "Thermostat Control",
        new Identifier(generateGlobalUniqueId(), Flow.TYPE),
        FLOW_NODES,
        new Wire[]{
            new Wire(TEMPERATURE_CONSUMER_SOURCE, TEMPERATURE_PROCESSOR_FLOW_FAHRENHEIT_SINK),
            new Wire(TEMPERATURE_PROCESSOR_FLOW_LABEL_SOURCE, TEMPERATURE_LABEL_SINK),
            new Wire(SETPOINT_CONSUMER_SOURCE, SETPOINT_PROCESSOR_FLOW_FAHRENHEIT_SINK),
            new Wire(SETPOINT_PROCESSOR_FLOW_LABEL_SOURCE, SETPOINT_LABEL_SINK),
            new Wire(SETPOINT_CONSUMER_SOURCE, SETPOINT_PLUS_FILTER_SINK),
            new Wire(SETPOINT_CONSUMER_SOURCE, SETPOINT_MINUS_FILTER_SINK),
            new Wire(SETPOINT_PLUS_BUTTON_SOURCE, SETPOINT_PLUS_FILTER_TRIGGER_SINK),
            new Wire(SETPOINT_MINUS_BUTTON_SOURCE, SETPOINT_MINUS_FILTER_TRIGGER_SINK),
            new Wire(SETPOINT_PLUS_FILTER_SOURCE, SETPOINT_INCREMENT_FUNCTION_SINK),
            new Wire(SETPOINT_MINUS_FILTER_SOURCE, SETPOINT_DECREMENT_FUNCTION_SINK),
            new Wire(SETPOINT_INCREMENT_FUNCTION_SOURCE, SETPOINT_PRODUCER_SINK),
            new Wire(SETPOINT_DECREMENT_FUNCTION_SOURCE, SETPOINT_PRODUCER_SINK)
        }
    );

    public static Flow getCopy() {
        try {
            return JSON.readValue(JSON.writeValueAsString(FLOW), Flow.class);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
