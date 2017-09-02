package org.avaje.docker.commands.process;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OutputParser {

  private final String[] keys;
  private final int[] pos;

  private final List<Map<String,String>> rows = new ArrayList<>();

  public static OutputMeta parse(List<String> lines, String[] headers) {
    OutputParser parser = new OutputParser(headers);
    parser.readHeaderPositions(lines.get(0));
    for (int i = 1; i < lines.size(); i++) {
      parser.readRow(lines.get(i));
    }
    return new OutputMeta(parser.getRows());
  }


  OutputParser(String[] keys) {
    this.keys = keys;
    this.pos = new int[keys.length];
  }

  void readHeaderPositions(String line) {
    for (int i = 0; i <keys.length; i++) {
      pos[i] = line.indexOf(keys[i]);
    }
  }

  void readRow(String line) {

    Map<String,String> row = new LinkedHashMap<>();

    for (int i = 1; i < pos.length; i++) {
      String sub = line.substring(pos[i-1], pos[i]);
      sub = sub.trim();
      row.put(keys[i-1], sub);
    }
    int last = pos.length-1;
    String sub = line.substring(pos[last]);
    sub = sub.trim();
    row.put(keys[last], sub);

    rows.add(row);
  }

  List<Map<String, String>> getRows() {
    return rows;
  }

}
