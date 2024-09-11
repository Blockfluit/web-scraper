package nl.nielsvanbruggen.webScraper.imdb.utils;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImdbUtils {
    private static final Pattern nameIdPattern = Pattern.compile("/name/(\\p{Alnum}+)/");
    private static final Pattern titleIdPattern = Pattern.compile("/title/(\\p{Alnum}+)/");

    private ImdbUtils() {}

    public static String[] parseName(String fullName) {
        String[] parts = fullName.split(" ");
        String[] name = new String[2];

        name[0] = parts.length == 1 ?
                parts[0] :
                String.join(" ", Arrays.copyOfRange(parts, 0, parts.length - 1));
        name[1] = parts.length == 1 ?
                "" :
                parts[parts.length - 1];

        return name;
    }

    public static String getNameIdFromUrl(String url) {
        Matcher matcher = nameIdPattern.matcher(url);

        if(matcher.find()) {
            return matcher.group(1);
        } else return null;
    }

    public static String getTitleIdFromUrl(String url) {
        Matcher matcher = titleIdPattern.matcher(url);

        if(matcher.find()) {
            return matcher.group(1);
        } else return null;
    }
}
