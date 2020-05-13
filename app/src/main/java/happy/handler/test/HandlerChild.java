package happy.handler.test;

import happy.handler.Handler;

@Handler
public interface HandlerChild extends HandlerParent {
    void childA();

    void childB();
}
