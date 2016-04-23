

package no.java.schedule.io.model;

import java.util.Arrays;

public class EMSData {


  public String name;
  public String prompt;
  public String value;
  public String[] array;

  @Override
  public String toString() {
    return "EMSData{" +
        "name='" + name + '\'' +
        ", prompt='" + prompt + '\'' +
        ", value='" + value + '\'' +
        ", array=" + Arrays.toString(array) +
        '}';
  }
}
