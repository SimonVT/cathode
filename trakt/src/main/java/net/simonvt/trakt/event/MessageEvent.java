package net.simonvt.trakt.event;

public class MessageEvent {

    private int mMessageRes;

    public MessageEvent(int messageRes) {
        mMessageRes = messageRes;
    }

    public int getMessageRes() {
        return mMessageRes;
    }
}
