package org.avaje.docker.commands.process;

import java.util.List;
import java.util.Map;

public class OutputMeta {

  final List<Map<String, String>> rows;

  public OutputMeta(List<Map<String, String>> rows) {
    this.rows = rows;
  }

  public boolean exists(String key, String match) {
    return find(key, match) != null;
  }

  public Map<String, String> find(String key, String match) {

    for (Map<String, String> row : rows) {
      String val = row.get(key);
      if (match.equals(val)) {
        return row;
      }
    }
    return null;
  }
}
