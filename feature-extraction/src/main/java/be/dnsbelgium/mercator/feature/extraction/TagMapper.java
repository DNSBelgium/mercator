package be.dnsbelgium.mercator.feature.extraction;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;

import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class TagMapper {

  private static final Logger logger = getLogger(TagMapper.class);

  @SuppressWarnings("RedundantTypeArguments")
  private final Map<String, String> tagMap = Map.<String, String>ofEntries(
      Map.entry("#document", ""),  // every JSoup document starts with this element, no need to map it
      Map.entry("a", "a"),
      Map.entry("abbr", "b"),
      Map.entry("address", "c"),
      Map.entry("applet", "d"),
      Map.entry("article", "e"),
      Map.entry("aside", "f"),
      Map.entry("audio", "g"),
      Map.entry("b", "h"),
      Map.entry("base", "i"),
      Map.entry("bdi", "j"),
      Map.entry("bdo", "k"),
      Map.entry("blockquote", "l"),
      Map.entry("body", "m"),
      Map.entry("br", "n"),
      Map.entry("button", "o"),
      Map.entry("canvas", "p"),
      Map.entry("caption", "q"),
      Map.entry("center", "r"),
      Map.entry("cite", "s"),
      Map.entry("code", "t"),
      Map.entry("colgroup", "u"),
      Map.entry("data", "v"),
      Map.entry("datalist", "w"),
      Map.entry("dd", "x"),
      Map.entry("del", "y"),
      Map.entry("details", "z"),
      Map.entry("dfn", "A"),
      Map.entry("dialog", "B"),
      Map.entry("div", "C"),
      Map.entry("dl", "D"),
      Map.entry("dt", "E"),
      Map.entry("em", "F"),
      Map.entry("embed", "G"),
      Map.entry("fieldset", "H"),
      Map.entry("figcaption", "I"),
      Map.entry("figure", "J"),
      Map.entry("footer", "K"),
      Map.entry("form", "L"),
      Map.entry("h1", "M"),
      Map.entry("h2", "N"),
      Map.entry("h3", "O"),
      Map.entry("h4", "P"),
      Map.entry("h5", "Q"),
      Map.entry("h6", "R"),
      Map.entry("head", "S"),
      Map.entry("header", "T"),
      Map.entry("hr", "U"),
      Map.entry("html", "V"),
      Map.entry("i", "W"),
      Map.entry("iframe", "X"),
      Map.entry("img", "Y"),
      Map.entry("input", "Z"),
      Map.entry("ins", "0"),
      Map.entry("kbd", "1"),
      Map.entry("label", "2"),
      Map.entry("legend", "3"),
      Map.entry("li", "4"),
      Map.entry("link", "5"),
      Map.entry("main", "6"),
      Map.entry("map", "7"),
      Map.entry("mark", "8"),
      Map.entry("meta", "9"),
      Map.entry("meter", "!"),
      Map.entry("nav", "#"),
      Map.entry("noframes", "$"),
      Map.entry("noscript", "%"),
      Map.entry("object", "&"),
      Map.entry("ol", "'"),
      Map.entry("optgroup", "("),
      Map.entry("option", ")"),
      Map.entry("output", "*"),
      Map.entry("p", "+"),
      Map.entry("param", ","),
      Map.entry("path", "`"),
      Map.entry("picture", "-"),
      Map.entry("pre", "."),
      Map.entry("progress", "/"),
      Map.entry("q", ":"),
      Map.entry("rp", ";"),
      Map.entry("rt", "<"),
      Map.entry("ruby", "="),
      Map.entry("s", ">"),
      Map.entry("samp", "?"),
      Map.entry("script", "@"),
      Map.entry("section", "["),
      Map.entry("select", "]"),
      Map.entry("small", "ÿ"),
      Map.entry("source", "^"),
      Map.entry("span", "_"),
      Map.entry("strike", "{"),
      Map.entry("strong", "}"),
      Map.entry("style", "|"),
      Map.entry("sub", "~"),
      Map.entry("summary", "é"),
      Map.entry("sup", "è"),
      Map.entry("svg", "ë"),
      Map.entry("table", "ê"),
      Map.entry("tbody", "á"),
      Map.entry("td", "à"),
      Map.entry("template", "ä"),
      Map.entry("textarea", "â"),
      Map.entry("tfoot", "í"),
      Map.entry("th", "ì"),
      Map.entry("thead", "ï"),
      Map.entry("time", "î"),
      Map.entry("title", "ó"),
      Map.entry("tr", "ò"),
      Map.entry("track", "ö"),
      Map.entry("tt", "ô"),
      Map.entry("u", "ú"),
      Map.entry("ul", "ù"),
      Map.entry("var", "ü"),
      Map.entry("video", "û"),
      Map.entry("wbr", "ç")
  );

  /*
    Returns a string representation of all tags in given document
   */

  /**
   * Returns a string representation of all tags in given document
   * @param document the document to compress
   * @return a string representation of the HTML structure of given document
   */
  public String compress(Document document) {
    StringBuilder result = new StringBuilder();
    for (Element element : document.getAllElements()) {
      String tag = element.nodeName().toLowerCase();
      String abbrev = tagMap.get(tag);
      if (abbrev == null) {
        logger.debug("No mapping defined for tag {}", tag);
        result.append("¿");
      } else {
        result.append(abbrev);
      }
    }
    return result.toString();
  }

}
