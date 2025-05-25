package be.dnsbelgium.mercator.vat.wappalyzer.jappalyzer;

import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

public class CappedEvaluator extends Evaluator {

  private final Evaluator delegate;
  private final int maximumMatches;
  private int matchCount = 0;

  public CappedEvaluator(Evaluator delegate, int maximumMatches) {
    this.delegate = delegate;
    this.maximumMatches = maximumMatches;
  }

  @Override
  public boolean matches(Element root, Element element) {
    if (matchCount >= maximumMatches) {
      return false;
    }
    boolean matches = delegate.matches(root, element);
    if (matches) {
      matchCount++;
      return true;
    }
    return false;
  }
}
