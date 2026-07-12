package be.dnsbelgium.mercator.geoip;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.nio.file.FileSystems;

public class FileUtil {

  public static final String FILE_SEP = FileSystems.getDefault().getSeparator();

  private FileUtil() {
  }

  public static String appendPath(String parent, String path, String... toAppend) {
    StringBuilder str = new StringBuilder(
        !StringUtils.isBlank(path) ? Strings.CS.appendIfMissing(parent, FILE_SEP, FILE_SEP) + path : parent);

    for (String s : toAppend) {
      if (!StringUtils.isBlank(s)) {
        str.append(FILE_SEP).append(s);
      }
    }
    return str.toString();
  }

}
