package net.simonvt.cathode.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ApiUtils {

  private static final char[] DIGITS = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f',
  };

  private ApiUtils() {
  }

  public static String getSha(String string) {
    return getSha(string, "UTF-8");
  }

  public static String getSha(String string, String encoding) {
    try {
      byte[] sha = MessageDigest.getInstance("SHA").digest(string.getBytes(encoding));
      return new String(encodeHex(sha));
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    return string;
  }

  private static char[] encodeHex(byte[] data) {
    int l = data.length;
    char[] out = new char[l << 1];

    for (int i = 0, j = 0; i < l; i++) {
      out[j++] = DIGITS[(0xF0 & data[i]) >>> 4];
      out[j++] = DIGITS[0x0F & data[i]];
    }
    return out;
  }

  public static boolean isPlaceholder(String url) {
    boolean isPlaceholder = false;

    isPlaceholder |= "http://slurm.trakt.us/images/avatar-large.jpg".equals(url);
    isPlaceholder |= "http://slurm.trakt.us/images/fanart-dark.jpg".equals(url);
    isPlaceholder |= "http://slurm.trakt.us/images/episode-dark.jpg".equals(url);
    isPlaceholder |= "http://slurm.trakt.us/images/poster-dark.jpg".equals(url);

    return isPlaceholder;
  }
}