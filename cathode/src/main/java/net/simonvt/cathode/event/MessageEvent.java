package net.simonvt.cathode.event;

public class MessageEvent {

  private int messageRes;

  public MessageEvent(int messageRes) {
    this.messageRes = messageRes;
  }

  public int getMessageRes() {
    return messageRes;
  }
}
