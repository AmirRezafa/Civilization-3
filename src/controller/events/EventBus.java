package controller.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventBus {
    private static final Map<Class<?>, List<Consumer<?>>> listeners = new HashMap<>();

    public static <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    @SuppressWarnings("unchecked")
    public static void publish(Object event) {
        List<Consumer<?>> subscribers = listeners.get(event.getClass());
        if (subscribers == null) return;

        for (Consumer<?> listener : new ArrayList<>(subscribers)) {
            ((Consumer<Object>) listener).accept(event);
        }
    }
}
