package io.ebean.test.containers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class TrimSplit {

  static List<String> split(String content) {
    if (content == null) {
      return Collections.emptyList();
    }
    List<String> names = new ArrayList<>();
    for (String serviceName : content.split(",")) {
      serviceName = serviceName.trim();
      if (!serviceName.isEmpty()) {
        names.add(serviceName);
      }
    }
    return names;
  }
}
