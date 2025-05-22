package be.dnsbelgium.mercator.common;

public class SurrogateCodePoints {


  public static String removeIncompleteSurrogates(String input) {
    return replaceIncompleteSurrogates(input, "");
  }

  /**
   * Removes char values that are either a Unicode high-surrogate code unit or a Unicode low-surrogate code unit
   * but are not part of a (high, low) pair.
   * @param input the input string
   * @return the input string with all incomplete surrogates removed
   */
  public static String replaceIncompleteSurrogates(String input, String replacement) {
    if (input == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < input.length(); i++) {
      char currentChar = input.charAt(i);
      boolean isHighSurrogate = Character.isHighSurrogate(currentChar);
      boolean isLowSurrogate = Character.isLowSurrogate(currentChar);
      if (!isHighSurrogate && !isLowSurrogate) {
        sb.append(currentChar);
      } else {
        boolean isLast = (i == input.length() - 1);
        if (isLast) {
          if (isHighSurrogate) //noinspection SingleStatementInBlock
          {
            // high surrogate as last char => pair is incomplete
            sb.append(replacement);
          } else {
            // current is low surrogate
            // but previous was not a high surrogate, else we would have processed it already
            sb.append(replacement);
          }
        } else {
          char nextChar = input.charAt(i + 1);
          boolean nextIsLowSurrogate = Character.isLowSurrogate(nextChar);
          if (isHighSurrogate && nextIsLowSurrogate) {
            sb.append(currentChar).append(nextChar);
            i++;
          } else {
            // either we have a low surrogate and previous was not a high surrogate
            // or we have a high surrogate not followed by a low surrogate
            sb.append(replacement);
          }
        }
      }
    }
    return sb.toString();
  }

}
