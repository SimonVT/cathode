package net.simonvt.trakt.event;

public class SyncEvent {

  private boolean syncing;

  public SyncEvent(boolean syncing) {
    this.syncing = syncing;
  }

  public boolean isSyncing() {
    return syncing;
  }
}
