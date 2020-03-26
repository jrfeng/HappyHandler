package happy.handler.test;

import happy.handler.Messenger;

@Messenger("MyMessenger")
public interface MessengerTest {
    void say(String words);

    void sayHello();
}
