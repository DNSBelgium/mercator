package be.dnsbelgium.mercator.wappalyzer.jappalyzerTests.utils;

import be.dnsbelgium.mercator.wappalyzer.jappalyzer.Jappalyzer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TestUtils {

    public static String readContentFromResource(String techFilename) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = Jappalyzer.class.getClassLoader().getResourceAsStream(techFilename)) {
            if (is != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String str;
                while ((str = br.readLine()) != null) {
                    sb.append(str);
                }
            }
        }
        return sb.toString();
    }

}
