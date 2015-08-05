package org.openremote.beta.test;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.openremote.beta.server.testdata.SampleTemperatureProcessor;
import org.openremote.beta.server.testdata.SampleThermostatControl;
import org.openremote.beta.server.util.IdentifierUtil;
import org.openremote.beta.shared.event.FlowStartEvent;
import org.openremote.beta.shared.event.FlowStartedEvent;
import org.openremote.beta.shared.event.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class ThermostatControlTest extends IntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(ThermostatControlTest.class);

    @Produce
    ProducerTemplate producerTemplate;

    @EndpointInject(uri = "mock:flowEventReceiver")
    MockEndpoint flowEventReceiver;

    @EndpointInject(uri = "mock:messageEventReceiver")
    MockEndpoint messageEventReceiver;

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("direct:sendFlowEvent")
                    .to(createWebSocketUri("flow"));

                from("direct:sendMessageEvent")
                    .to(createWebSocketUri("message"));

                from(createWebSocketUri("flow"))
                    .to("log:FLOW_EVENT_RECEIVED: ${body}")
                    .to("mock:flowEventReceiver");

                from(createWebSocketUri("message"))
                    .to("log:MESSAGE_EVENT_RECEIVED: ${body}")
                    .to("mock:messageEventReceiver");
            }
        };
    }

    @Test
    public void execute() throws Exception {

        flowEventReceiver.reset();
        flowEventReceiver.expectedBodiesReceived(
            toJson(new FlowStartedEvent(SampleTemperatureProcessor.FLOW.getIdentifier().getId()))
        );
        FlowStartEvent flowStartEvent = new FlowStartEvent(SampleTemperatureProcessor.FLOW);
        producerTemplate.sendBody("direct:sendFlowEvent", flowStartEvent);
        flowEventReceiver.assertIsSatisfied();

        flowEventReceiver.reset();
        flowEventReceiver.expectedBodiesReceived(
            toJson(new FlowStartedEvent(SampleThermostatControl.FLOW.getIdentifier().getId()))
        );
        flowStartEvent = new FlowStartEvent(SampleThermostatControl.FLOW);
        producerTemplate.sendBody("direct:sendFlowEvent", flowStartEvent);
        flowEventReceiver.assertIsSatisfied();

        LOG.info("##########################################################################");

        MockEndpoint mockLabelTemperature = context().getEndpoint("mock:labelTemperature", MockEndpoint.class);
        MockEndpoint mockLabelSetpoint = context().getEndpoint("mock:labelSetpoint", MockEndpoint.class);

        mockLabelTemperature.expectedBodiesReceived("23 C");
        mockLabelSetpoint.expectedBodiesReceived("21 C");

        final String INSTANCE_ID = IdentifierUtil.generateGlobalUniqueId();

        messageEventReceiver.expectedBodiesReceivedInAnyOrder(
            toJson(new MessageEvent(
                SampleThermostatControl.FLOW,
                SampleThermostatControl.TEMPERATURE_CONSUMER,
                SampleThermostatControl.TEMPERATURE_CONSUMER_SINK,
                INSTANCE_ID,
                "75"
            )),
            toJson(new MessageEvent(
                SampleTemperatureProcessor.FLOW,
                SampleTemperatureProcessor.FAHRENHEIT_CONSUMER,
                SampleTemperatureProcessor.FAHRENHEIT_CONSUMER_SINK,
                INSTANCE_ID,
                "75"
            )),
            toJson(new MessageEvent(
                SampleTemperatureProcessor.FLOW,
                SampleTemperatureProcessor.CELCIUS_PRODUCER,
                SampleTemperatureProcessor.CELCIUS_PRODUCER_SINK,
                INSTANCE_ID,
                "23"
            )),
            toJson(new MessageEvent(
                SampleTemperatureProcessor.FLOW,
                SampleTemperatureProcessor.LABEL_PRODUCER,
                SampleTemperatureProcessor.LABEL_PRODUCER_SINK,
                INSTANCE_ID,
                "23 C"
            )),
            toJson(new MessageEvent(
                SampleThermostatControl.FLOW,
                SampleThermostatControl.TEMPERATURE_LABEL,
                SampleThermostatControl.TEMPERATURE_LABEL_SINK,
                INSTANCE_ID,
                "23 C"
            )),
            toJson(new MessageEvent(
                SampleThermostatControl.FLOW,
                SampleThermostatControl.SETPOINT_CONSUMER,
                SampleThermostatControl.SETPOINT_CONSUMER_SINK,
                INSTANCE_ID,
                "70"
            )),
            toJson(new MessageEvent(
                SampleTemperatureProcessor.FLOW,
                SampleTemperatureProcessor.FAHRENHEIT_CONSUMER,
                SampleTemperatureProcessor.FAHRENHEIT_CONSUMER_SINK,
                INSTANCE_ID,
                "70"
            )),
            toJson(new MessageEvent(
                SampleTemperatureProcessor.FLOW,
                SampleTemperatureProcessor.CELCIUS_PRODUCER,
                SampleTemperatureProcessor.CELCIUS_PRODUCER_SINK,
                INSTANCE_ID,
                "21"
            )),
            toJson(new MessageEvent(
                SampleTemperatureProcessor.FLOW,
                SampleTemperatureProcessor.LABEL_PRODUCER,
                SampleTemperatureProcessor.LABEL_PRODUCER_SINK,
                INSTANCE_ID,
                "21 C"
            )),
            toJson(new MessageEvent(
                SampleThermostatControl.FLOW,
                SampleThermostatControl.SETPOINT_LABEL,
                SampleThermostatControl.SETPOINT_LABEL_SINK,
                INSTANCE_ID,
                "21 C"
            ))
        );

        Exchange exchange = new DefaultExchange(context());
        exchange.getIn().setBody(new MessageEvent(
            SampleThermostatControl.FLOW,
            SampleThermostatControl.TEMPERATURE_CONSUMER,
            SampleThermostatControl.TEMPERATURE_CONSUMER_SINK,
            INSTANCE_ID,
            "75"
        ));
        producerTemplate.send("direct:sendMessageEvent", exchange);

        exchange = new DefaultExchange(context());
        exchange.getIn().setBody(new MessageEvent(
            SampleThermostatControl.FLOW,
            SampleThermostatControl.SETPOINT_CONSUMER,
            SampleThermostatControl.SETPOINT_CONSUMER_SINK,
            INSTANCE_ID,
            "70"
        ));
        producerTemplate.send("direct:sendMessageEvent", exchange);

        LOG.info("##########################################################################");

        mockLabelTemperature.assertIsSatisfied();
        mockLabelSetpoint.assertIsSatisfied();
        messageEventReceiver.assertIsSatisfied();

        LOG.info("##########################################################################");

        MockEndpoint mockProducerSetpoint = context().getEndpoint("mock:producerSetpoint", MockEndpoint.class);
        mockProducerSetpoint.expectedBodiesReceived("69", "69");

        messageEventReceiver.reset();
        messageEventReceiver.expectedBodiesReceivedInAnyOrder(
            toJson(new MessageEvent(
                SampleThermostatControl.FLOW,
                SampleThermostatControl.SETPOINT_MINUS_BUTTON,
                SampleThermostatControl.SETPOINT_MINUS_BUTTON_SINK,
                INSTANCE_ID,
                null
            )),
            toJson(new MessageEvent(
                SampleThermostatControl.FLOW,
                SampleThermostatControl.SETPOINT_PRODUCER,
                SampleThermostatControl.SETPOINT_PRODUCER_SINK,
                INSTANCE_ID,
                "69"
            )),
            toJson(new MessageEvent(
                SampleThermostatControl.FLOW,
                SampleThermostatControl.SETPOINT_MINUS_BUTTON,
                SampleThermostatControl.SETPOINT_MINUS_BUTTON_SINK,
                INSTANCE_ID,
                null
            )),
            toJson(new MessageEvent(
                SampleThermostatControl.FLOW,
                SampleThermostatControl.SETPOINT_PRODUCER,
                SampleThermostatControl.SETPOINT_PRODUCER_SINK,
                INSTANCE_ID,
                "69"
            ))
        );

        exchange = new DefaultExchange(context());
        exchange.getIn().setBody(new MessageEvent(
            SampleThermostatControl.FLOW,
            SampleThermostatControl.SETPOINT_MINUS_BUTTON,
            SampleThermostatControl.SETPOINT_MINUS_BUTTON_SINK,
            INSTANCE_ID,
            null
        ));
        producerTemplate.send("direct:sendMessageEvent", exchange);

        exchange = new DefaultExchange(context());
        exchange.getIn().setBody(new MessageEvent(
            SampleThermostatControl.FLOW,
            SampleThermostatControl.SETPOINT_MINUS_BUTTON,
            SampleThermostatControl.SETPOINT_MINUS_BUTTON_SINK,
            INSTANCE_ID,
            null
        ));
        producerTemplate.send("direct:sendMessageEvent", exchange);

        LOG.info("##########################################################################");

        mockProducerSetpoint.assertIsSatisfied();
        messageEventReceiver.assertIsSatisfied();
    }

}
