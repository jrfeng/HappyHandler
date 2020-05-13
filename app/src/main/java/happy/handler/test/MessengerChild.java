package happy.handler.test;

import happy.handler.Messenger;

@Messenger
public interface MessengerChild extends MessengerParent {
    void childA();

    void childB();
}
