package org.openremote.beta.client.shared;

import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsType;
import elemental.client.Browser;
import elemental.dom.Element;
import elemental.events.CloseEvent;
import elemental.events.MessageEvent;
import elemental.html.WebSocket;
import org.fusesource.restygwt.client.JsonEncoderDecoder;
import org.openremote.beta.client.shared.SessionClosedErrorEvent.Error;
import org.openremote.beta.shared.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsType
@JsExport
public abstract class SessionPresenter extends RequestPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(SessionPresenter.class);

    protected WebSocket webSocket;
    protected int failureCount;
    protected int currentReconnectAttempt = -1;

    public SessionPresenter(Element view) {
        super(view);

        addEventListener(SessionConnectEvent.class, event -> {

            if (webSocket != null) {
                if (webSocket.getReadyState() != WebSocket.CLOSED) {
                    // Close silently
                    LOG.debug(
                        "New connection attempt to '" + event.getServiceUrl() + "', closing " +
                            "stale existing connection silently: " + webSocket.getUrl()
                    );
                    webSocket.setOnclose(null);
                    webSocket.close();
                }
                webSocket = null;
            }

            webSocket = Browser.getWindow().newWebSocket(event.getServiceUrl());
            webSocket.setOnopen(evt -> {
                if (webSocket.getReadyState() == WebSocket.OPEN) {
                    LOG.debug("WebSocket open: " + webSocket.getUrl());
                    dispatchEvent(new SessionOpenedEvent());
                }
            });
            webSocket.setOnclose(evt -> {
                CloseEvent closeEvent = (CloseEvent) evt;
                if (closeEvent.isWasClean() && closeEvent.getCode() == 1000) {
                    LOG.debug("WebSocket closed: " + webSocket.getUrl());
                    dispatchEvent(new SessionClosedCleanEvent());
                } else {
                    LOG.debug("WebSocket '" + webSocket.getUrl() + "' closed with error: " + closeEvent.getCode());
                    dispatchEvent(new SessionClosedErrorEvent(
                        new Error(closeEvent.getCode(), closeEvent.getReason())
                    ));
                }
            });
            webSocket.setOnmessage(evt -> {
                MessageEvent messageEvent = (MessageEvent) evt;
                String data = messageEvent.getData().toString();
                LOG.debug("Received data on WebSocket '" + webSocket.getUrl() + "': " + data);
                onMessageReceived(data);
            });
        });

        addEventListener(SessionCloseEvent.class, event -> {
            if (webSocket != null) {
                webSocket.close(1000, "SessionCloseEvent");
            }
        });
    }

    public void connectAndRetryOnFailure(String serviceUrl, int maxAttempts, int delayMillis) {

        addEventListener(SessionOpenedEvent.class, event -> {
            if (currentReconnectAttempt != -1) {
                LOG.debug("Session opened successfully, resetting failure count...");
                failureCount = 0;
                currentReconnectAttempt = -1;
            }
        });

        addEventListener(SessionClosedErrorEvent.class, event -> {
            LOG.debug("Session closed with error, incrementing failure count: " + failureCount);
            failureCount++;
            if (failureCount < maxAttempts) {
                LOG.debug("Session reconnection attempt '" + serviceUrl + "' with delay milliseconds: " + delayMillis);
                currentReconnectAttempt = dispatchEvent(
                    new SessionConnectEvent(serviceUrl),
                    delayMillis,
                    currentReconnectAttempt
                );
            } else {
                LOG.error("Giving up connecting to service after " + failureCount + " failures: " + serviceUrl);
            }
        });

        currentReconnectAttempt = dispatchEvent(new SessionConnectEvent(serviceUrl));
    }

    protected void sendMessage(String data) {
        if (webSocket != null && webSocket.getReadyState() == WebSocket.OPEN) {
            LOG.debug("Sending data on WebSocket '" + webSocket.getUrl() + "': " + data);
            webSocket.send(data);
        } else {
            LOG.debug("WebSocket not connected, discarding: " + data);
        }
    }

    protected <E extends Event> void sendMessage(JsonEncoderDecoder<E> encoderDecoder, E event) {
        sendMessage(encoderDecoder.encode(event).toString());
    }

    protected abstract void onMessageReceived(String data);
}
