package org.openremote.beta.client.shared;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.dom.TimeoutHandler;
import elemental.events.CustomEvent;
import elemental.events.EventRemover;
import org.openremote.beta.shared.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractPresenter.class);

    final protected Element view;

    public AbstractPresenter(Element view) {
        if (view == null)
            throw new IllegalArgumentException("Can't instantiate presenter without a view element: " + getClass().getName());
        LOG.debug("Creating presenter for view '" + view.getLocalName() + "': " + getClass().getName());
        this.view = view;
    }

    public Element getView() {
        return view;
    }

    public <E extends Event> EventRemover addEventListener(Class<E> eventClass,
                                                           EventListener<E> listener) {
        return addEventListener(eventClass, false, false, listener);
    }

    public <E extends Event> EventRemover addEventListener(Class<E> eventClass,
                                                           boolean stopPropagation,
                                                           boolean stopImmediatePropagation,
                                                           EventListener<E> listener) {
        String eventType = Event.getType(eventClass);
        LOG.debug("Adding event listener to view '" + getView().getLocalName() + "': " + eventType);
        return getView().addEventListener(eventType, evt -> {
            CustomEvent customEvent = (CustomEvent) evt;
            if (stopPropagation)
                customEvent.stopPropagation();
            if (stopImmediatePropagation)
                customEvent.stopImmediatePropagation();
            @SuppressWarnings("unchecked")
            E event = (E) customEvent.getDetail();
            listener.on(event);
        }, false); // Disable the capture phase, optional bubbling is easier to understand
    }

    public int dispatchEvent(Event event) {
        return dispatchEvent(event, 0);
    }

    public int dispatchEvent(Event event, int delayMillis) {
        return dispatchEvent(event, delayMillis, -1);
    }

    public int dispatchEvent(Event event, int delayMillis, int timeoutId) {
        if (timeoutId != -1) {
            Browser.getWindow().clearTimeout(timeoutId);
        }
        TimeoutHandler dispatchHandler = () -> {
            LOG.debug("Dispatching custom event on view '" + getView().getLocalName() + "': " + event.getType());
            CustomEvent customEvent = (CustomEvent) view.getOwnerDocument().createEvent("CustomEvent");

            boolean bubbling = true;
            boolean cancelable = true;
            if (event instanceof PropagationOptions) {
                PropagationOptions propagationOptions = (PropagationOptions) event;
                bubbling = propagationOptions.isBubbling();
                cancelable = propagationOptions.isCancelable();
            }

            customEvent.initCustomEvent(event.getType(), bubbling, cancelable, event);
            getView().dispatchEvent(customEvent);
        };
        if (delayMillis > 0) {
            LOG.debug("Scheduling after " + delayMillis + " milliseconds, custom event on view '" + getView().getLocalName() + "': " + event.getType());
            return Browser.getWindow().setTimeout(dispatchHandler, delayMillis);
        } else {
            dispatchHandler.onTimeoutHandler();
            return -1;
        }
    }
}

