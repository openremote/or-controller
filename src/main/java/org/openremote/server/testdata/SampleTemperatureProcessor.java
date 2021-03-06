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

package org.openremote.server.testdata;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openremote.server.catalog.change.ChangeNodeDescriptor;
import org.openremote.server.catalog.function.FunctionNodeDescriptor;
import org.openremote.server.catalog.storage.StorageNodeDescriptor;
import org.openremote.server.route.ConsumerRoute;
import org.openremote.server.route.ProducerRoute;
import org.openremote.shared.flow.Flow;
import org.openremote.shared.flow.Node;
import org.openremote.shared.flow.Slot;
import org.openremote.shared.flow.Wire;

import java.io.IOException;
import java.util.List;

import static org.openremote.server.util.IdentifierUtil.generateGlobalUniqueId;
import static org.openremote.server.util.JsonUtil.JSON;

public class SampleTemperatureProcessor {

    /* ###################################################################################### */

    public static Slot FAHRENHEIT_CONSUMER_SOURCE = new Slot(generateGlobalUniqueId(), Slot.TYPE_SOURCE);
    public static Slot FAHRENHEIT_CONSUMER_SINK = new Slot(generateGlobalUniqueId(), Slot.TYPE_SINK, false);
    public static Node FAHRENHEIT_CONSUMER = new Node("Fahrenheit", generateGlobalUniqueId(), Node.TYPE_CONSUMER);

    static {
        new ConsumerRoute.Descriptor() {
            @Override
            public void addSlots(List<Slot> slots) {
                slots.add(FAHRENHEIT_CONSUMER_SOURCE);
                slots.add(FAHRENHEIT_CONSUMER_SINK);
            }
        }.initialize(FAHRENHEIT_CONSUMER);
        FAHRENHEIT_CONSUMER.getEditorSettings().setPositionX((double) 10);
        FAHRENHEIT_CONSUMER.getEditorSettings().setPositionY((double) 250);
    }

    /* ###################################################################################### */

    public static Slot FAHRENHEIT_CONVERTER_SINK = new Slot(generateGlobalUniqueId(), Slot.TYPE_SINK);
    public static Slot FAHRENHEIT_CONVERTER_SOURCE = new Slot(generateGlobalUniqueId(), Slot.TYPE_SOURCE);
    public static Node FAHRENHEIT_CONVERTER = new Node("Fahrenheit to Celcius", generateGlobalUniqueId(), FunctionNodeDescriptor.TYPE);

    static {
        new FunctionNodeDescriptor() {
            @Override
            public void addSlots(List<Slot> slots) {
                slots.add(FAHRENHEIT_CONVERTER_SINK);
                slots.add(FAHRENHEIT_CONVERTER_SOURCE);
            }

            @Override
            protected ObjectNode getInitialProperties() {
                return FunctionNodeDescriptor.FUNCTION_INITIAL_PROPERTIES.deepCopy()
                    .put("javascript", "result = input ? (((parseInt(input) - 32)*5)/9).toFixed(0) : null");
            }
        }.initialize(FAHRENHEIT_CONVERTER);
        FAHRENHEIT_CONVERTER.getEditorSettings().setPositionX((double) 300);
        FAHRENHEIT_CONVERTER.getEditorSettings().setPositionY((double) 250);
    }

    /* ###################################################################################### */

    public static Slot TEMPERATURE_DATABASE_SINK = new Slot(generateGlobalUniqueId(), Slot.TYPE_SINK);
    public static Node TEMPERATURE_DATABASE = new Node("Temperature Database", generateGlobalUniqueId(), StorageNodeDescriptor.TYPE);

    static {
        new StorageNodeDescriptor() {
            @Override
            public void addSlots(List<Slot> slots) {
                slots.add(TEMPERATURE_DATABASE_SINK);
            }
        }.initialize(TEMPERATURE_DATABASE);
        TEMPERATURE_DATABASE.setPostEndpoint("mock:temperatureDatabase");
        TEMPERATURE_DATABASE.getEditorSettings().setPositionX((double) 350);
        TEMPERATURE_DATABASE.getEditorSettings().setPositionY((double) 400);
    }

    /* ###################################################################################### */

    public static Slot CELCIUS_PRODUCER_SINK = new Slot(generateGlobalUniqueId(), Slot.TYPE_SINK);
    public static Slot CELCIUS_PRODUCER_SOURCE = new Slot(generateGlobalUniqueId(), Slot.TYPE_SOURCE, false);
    public static Node CELCIUS_PRODUCER = new Node("Celcius", generateGlobalUniqueId(), Node.TYPE_PRODUCER);

    static {
        new ProducerRoute.Descriptor() {
            @Override
            public void addSlots(List<Slot> slots) {
                slots.add(CELCIUS_PRODUCER_SINK);
                slots.add(CELCIUS_PRODUCER_SOURCE);
            }
        }.initialize(CELCIUS_PRODUCER);
        CELCIUS_PRODUCER.setPostEndpoint("mock:producerCelcius");
        CELCIUS_PRODUCER.getEditorSettings().setPositionX((double) 750);
        CELCIUS_PRODUCER.getEditorSettings().setPositionY((double) 250);
    }

    /* ###################################################################################### */

    public static Slot CELCIUS_APPENDER_SINK = new Slot(generateGlobalUniqueId(), Slot.TYPE_SINK);
    public static Slot CELCIUS_APPENDER_SOURCE = new Slot(generateGlobalUniqueId(), Slot.TYPE_SOURCE);
    public static Node CELCIUS_APPENDER = new Node("Append Celcius Symbol", generateGlobalUniqueId(), ChangeNodeDescriptor.TYPE);

    static {
        new ChangeNodeDescriptor() {
            @Override
            public void addSlots(List<Slot> slots) {
                slots.add(CELCIUS_APPENDER_SINK);
                slots.add(CELCIUS_APPENDER_SOURCE);
            }

            @Override
            protected ObjectNode getInitialProperties() {
                return JSON.createObjectNode().put("append", " \u00B0C");
            }
        }.initialize(CELCIUS_APPENDER);
        CELCIUS_APPENDER.getEditorSettings().setPositionX((double) 650);
        CELCIUS_APPENDER.getEditorSettings().setPositionY((double) 400);
    }

    /* ###################################################################################### */

    public static Slot LABEL_PRODUCER_SINK = new Slot(generateGlobalUniqueId(), Slot.TYPE_SINK);
    public static Slot LABEL_PRODUCER_SOURCE = new Slot(generateGlobalUniqueId(), Slot.TYPE_SOURCE, false);
    public static Node LABEL_PRODUCER = new Node("Label", generateGlobalUniqueId(), Node.TYPE_PRODUCER);

    static {
        new ProducerRoute.Descriptor() {
            @Override
            public void addSlots(List<Slot> slots) {
                slots.add(LABEL_PRODUCER_SINK);
                slots.add(LABEL_PRODUCER_SOURCE);
            }
        }.initialize(LABEL_PRODUCER);
        LABEL_PRODUCER.setPostEndpoint("mock:producerLabel");
        LABEL_PRODUCER.getEditorSettings().setPositionX((double) 1000);
        LABEL_PRODUCER.getEditorSettings().setPositionY((double) 450);
    }

    /* ###################################################################################### */

    public static Node[] FLOW_NODES = new Node[]{
        FAHRENHEIT_CONSUMER,
        FAHRENHEIT_CONVERTER,
        TEMPERATURE_DATABASE,
        CELCIUS_PRODUCER,
        CELCIUS_APPENDER,
        LABEL_PRODUCER
    };

    /* ###################################################################################### */

    public static Flow FLOW = new Flow(
        "Temperature Processor",
        generateGlobalUniqueId(),
        FLOW_NODES,
        new Wire[]{
            new Wire(FAHRENHEIT_CONSUMER_SOURCE, FAHRENHEIT_CONVERTER_SINK),
            new Wire(FAHRENHEIT_CONSUMER_SOURCE, TEMPERATURE_DATABASE_SINK),
            new Wire(FAHRENHEIT_CONVERTER_SOURCE, CELCIUS_PRODUCER_SINK),
            new Wire(FAHRENHEIT_CONVERTER_SOURCE, CELCIUS_APPENDER_SINK),
            new Wire(CELCIUS_APPENDER_SOURCE, LABEL_PRODUCER_SINK)
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
