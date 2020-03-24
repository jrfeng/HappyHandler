package happy.handler.test;

import happy.handler.Handler;

@Handler
public interface Hello {
    void say(String words);

    void sayHello();
}