package Project.strategies;

import Project.MyTextEvent;

/**
 * Created by Simon Purup Eskildsen on 4/6/17.
 */
public interface EventHandlerStrategy {
    void handleEvent(MyTextEvent event);
    default void close(){}
}
