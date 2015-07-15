package com.hubspot.imap.imap.message;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface ImapMessage {
  Set<MessageFlag> getFlags() throws UnfetchedFieldException;
  long getMessageNumber() throws UnfetchedFieldException;
  long getUid() throws UnfetchedFieldException;

  class Builder implements ImapMessage {
    private Optional<Set<MessageFlag>> flags;
    private Optional<Long> messageNumber;
    private Optional<Long> uid;

    public ImapMessage build() {
      return this;
    }

    public Set<MessageFlag> getFlags() throws UnfetchedFieldException {
      return this.flags.orElseThrow(() -> new UnfetchedFieldException("flags"));
    }

    public Builder setFlagStrings(Collection<String> flags) {
      this.flags = Optional.of(flags.stream()
          .map(MessageFlag::getFlag)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .collect(Collectors.toSet()));
      return this;
    }

    public Builder setFlags(Set<MessageFlag> flags) {
      this.flags = Optional.of(flags);
      return this;
    }

    public long getMessageNumber() throws UnfetchedFieldException {
      return this.messageNumber.orElseThrow(() -> new UnfetchedFieldException("message number"));
    }

    public Builder setMessageNumber(long messageNumber) {
      this.messageNumber = Optional.of(messageNumber);
      return this;
    }

    public long getUid() throws UnfetchedFieldException {
      return this.uid.orElseThrow(() -> new UnfetchedFieldException("uid"));
    }

    public Builder setUid(long uid) {
      this.uid = Optional.of(uid);
      return this;
    }
  }
}
