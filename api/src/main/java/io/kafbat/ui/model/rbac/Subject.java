package io.kafbat.ui.model.rbac;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.kafbat.ui.model.rbac.provider.Provider;
import java.util.Objects;
import lombok.Getter;

@Getter
public class Subject {

  Provider provider;
  String type;
  String value;
  boolean isRegex;

  public void setProvider(String provider) {
    this.provider = Provider.fromString(provider.toUpperCase());
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public void setIsRegex(boolean isRegex) {
    this.isRegex = isRegex;
  }

  public void validate() {
    checkNotNull(type, "Subject type cannot be null");
    checkNotNull(value, "Subject value cannot be null");

    checkArgument(!type.isEmpty(), "Subject type cannot be empty");
    checkArgument(!value.isEmpty(), "Subject value cannot be empty");
  }

  public boolean matches(final String attribute) {
    if (isRegex) {
      return Objects.nonNull(attribute) && attribute.matches(this.value);
    }
    return this.value.equalsIgnoreCase(attribute);
  }
}
