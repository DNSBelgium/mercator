package be.dnsbelgium.mercator.feature.extraction.socialmedia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SocialMediaLinkAnalyser {

  private final Pattern facebookShallowPattern = Pattern.compile("^(https?://)?(www\\.)?facebook\\.com" +
      "(/[A-z]+\\.php|/marketplace|/gaming|/watch|/me|/messages|/help|/search|/groups|/policies" +
      "|/policies_center|/about|/legal|/communitystandards|/business|/payments_terms|/privacy|/dialog/feed(.*))?");

  // Matches the <profile> or <id> group with the profile id found in the URL
  // Regexes from https://github.com/lorey/social-media-profiles-regexs/#facebook
  private final Pattern facebookProfilePattern = Pattern.compile(
      "^(?:https?://)?(?:www\\.)?(?:facebook|fb)\\.com/(#!/)?(?<profile>" +
      "(?![A-z]+\\.php)(?!marketplace|gaming|watch|me/?$|messages|policies|policies_center|help|sharer|search" +
      "|groups/?$|hashtag|legal|about|communitystandards|business|payments_terms|privacy|dialog/feed)[A-z0-9_\\-.]+)(.*)");

  private final Pattern facebookIdPattern = Pattern.compile("^(?:https?://)?(?:www\\.)?facebook\\.com/(#!/)?" +
      "(?:profile\\.php\\?id=)?(?<id>[0-9]+)");

  // Regexes from https://github.com/lorey/social-media-profiles-regexs/#linkedin
  private final Pattern linkedinShallowPattern = Pattern.compile("^(?:https?://)?(?:[\\w]+\\.)?linkedin\\.com/?$");

  private final Pattern linkedinCompanyPattern = Pattern.compile("^(?:https?://)?(?:[\\w]+\\.)?linkedin\\.com/" +
      "(?<companyType>(company)|(school))/(?<companyPermalink>[A-z0-9-À-ÿ.]+)/?");

  private final Pattern linkedinPostPattern = Pattern.compile("^(?:https?://)?(?:[\\w]+\\.)?linkedin\\.com/feed/" +
      "update/urn:li:activity:(?<activityId>[0-9]+)/?");

  private final Pattern linkedinProfilePattern = Pattern.compile("^(?:https?://)?(?:[\\w]+\\.)?linkedin\\.com/" +
      "in/(?<permalink>[\\w\\-_À-ÿ%]+)/?");

  private final Pattern linkedinOldProfilePattern = Pattern.compile("^(?:https?://)?(?:[\\w]+\\.)?linkedin\\.com/" +
      "pub/(?<permalink>[A-z0-9_-]+)(?:/[A-z0-9]+){3}/?");

  // Regexes from https://github.com/lorey/social-media-profiles-regexs/#twitter
  private final Pattern twitterShallowPattern = Pattern.compile("^(?:https?://)?(?:www\\.)?twitter\\.com" +
      "($|/$|/intent/tweet)");

  private final Pattern twitterPostPattern = Pattern.compile("^(?:https?://)?(?:www\\.)?twitter\\.com/" +
      "@?(?<username>[A-z0-9_]+)/status/(?<tweetId>[0-9]+)/?");

  private final Pattern twitterUserPattern = Pattern.compile("^(?:https?://)?(?:www\\.)?twitter\\.com/" +
      "@?(?!home|share|privacy|intent)(?<username>[A-z0-9_]+)/?");

  // Regexes from https://github.com/lorey/social-media-profiles-regexs/#youtube
  private final Pattern youtubeDeepPattern = Pattern.compile("^(?:https?://)?(?:[A-z]+\\.)?youtube\\.com" +
      "(/channel|/c|/user|)/(?<id>[A-z0-9-_]+)/?");
  private final Pattern youtubeVideoPattern = Pattern.compile("(?:https?:)?//(?:(?:[A-z]+\\.)?youtube\\.com/" +
      "(?:watch\\?v=|embed/)|youtu\\.be/)(?<id>[A-z0-9\\-_]+)");
  private final Pattern youtubePlaylistPattern = Pattern.compile("^(?:https?://)?(?:[A-z]+\\.)?youtube\\.com" +
      "/playlist\\?list=(?<playlist>[A-z0-9\\-_]+)/?");

  private final Pattern youtubeShallowPattern = Pattern.compile("^(?:https?://)?(?:[A-z]+\\.)?(youtube\\.com|youtu\\.be(/|)$)" +
      "((/|)$|((/feed|/gaming|/explore|/results|/account|/premium|/reporthistory)(([#/?])(.*))?))");

  // Regexes from https://github.com/lorey/social-media-profiles-regexs/#vimeo
  private final Pattern vimeoDeepPattern = Pattern.compile("^(?:https?://)?(?:www\\.)?vimeo\\.com/" +
      "([A-z0-9\\-_]+|video/[0-9]+)");
  private final Pattern vimeoPlayerPattern = Pattern.compile("^(?:https?://)?(player\\.)vimeo\\.com(/video)?/([0-9]+)");
  private final Pattern vimeoChannelPattern = Pattern.compile("^(https?://)?(www\\.)?vimeo\\.com/" +
      "((channels|groups)/[A-z0-9\\-_]+)");

  private final Pattern vimeoShallowPattern = Pattern.compile("^(https?://)?(www\\.)?vimeo\\.com" +
      "((/|)$" +
      "|((/(channels|groups)(/|)$)" +
      "|((/features|/create|/ott|/stock|/for-hire|/blog|/students|/partners|/help|/upgrade|/upload|/about|/everyone" +
        "|/watch|/terms|/privacy|/cookie_policy|/dmca|/professionals|/business|/live|/ondemand|/analytics|/cameo" +
        "|/jobs|/join|/log_in|/solutions|/search)(([#/?])(.*))?)))");


  public SocialMediaLinkAnalyser() {
  }

  /**
   * Find and classifies social media links (Twitter, Facebook and LinkedIn) from the given URL.  The links
   * are classified for each social media between deep (pointing to a particular page, post, event, user...) and shallow.
   * As suggested in Senne Batsleer's thesis, the links allowing a website owner to create and instant share button are
   * counted as shallow links as they do not point to a specific page
   * (e.g. www.facebook.com/sharer/sharer.php?u=https://www.dnsbelgium.be)
   *
   * Regexes at the top of this class are used for deep/shallow link classification
   *
   * @param url The URL to classify
   */
  public SocialMediaLinkType getType(String url) {
    Matcher facebookShallowMatcher;
    Matcher facebookProfileMatcher;
    Matcher facebookIdMatcher;

    Matcher linkedinCompanyMatcher;
    Matcher linkedinPostMatcher;
    Matcher linkedinProfileMatcher;
    Matcher linkedinOldProfileMatcher;
    Matcher linkedinShallowMatcher;

    Matcher twitterPostMatcher;
    Matcher twitterUserMatcher;
    Matcher twitterShallowMatcher;

    Matcher youtubeShallowMatcher;
    Matcher youtubeChannelMatcher;
    Matcher youtubeVideoMatcher;
    Matcher youtubePlaylistMatcher;

    Matcher vimeoDeepMatcher;
    Matcher vimeoPlayerMatcher;
    Matcher vimeoChannelMatcher;
    Matcher vimeoShallowMatcher;

    // Facebook links
    facebookProfileMatcher = facebookProfilePattern.matcher(url);
    facebookIdMatcher = facebookIdPattern.matcher(url);

    if (facebookProfileMatcher.find() || facebookIdMatcher.find()) {
      return SocialMediaLinkType.FACEBOOK_DEEP;
    } else {
      facebookShallowMatcher = facebookShallowPattern.matcher(url);
      if (facebookShallowMatcher.find()) {
        return SocialMediaLinkType.FACEBOOK_SHALLOW;
      }
    }

    // Linkedin links
    linkedinShallowMatcher = linkedinShallowPattern.matcher(url);

    if (linkedinShallowMatcher.find()) {
      return SocialMediaLinkType.LINKEDIN_SHALLOW;
    } else {
      linkedinCompanyMatcher = linkedinCompanyPattern.matcher(url);
      linkedinPostMatcher = linkedinPostPattern.matcher(url);
      linkedinProfileMatcher = linkedinProfilePattern.matcher(url);
      linkedinOldProfileMatcher = linkedinOldProfilePattern.matcher(url);

      if (linkedinCompanyMatcher.find() || linkedinPostMatcher.find() || linkedinProfileMatcher.find()
          || linkedinOldProfileMatcher.find()) {
        return SocialMediaLinkType.LINKEDIN_DEEP;
      }
    }


    // Twitter links
    twitterShallowMatcher = twitterShallowPattern.matcher(url);
    if (twitterShallowMatcher.find()) {
      return SocialMediaLinkType.TWITTER_SHALLOW;
    } else {
      twitterPostMatcher = twitterPostPattern.matcher(url);
      twitterUserMatcher = twitterUserPattern.matcher(url);

      if (twitterPostMatcher.find() || twitterUserMatcher.find()) {
        return SocialMediaLinkType.TWITTER_DEEP;
      }
    }


    // YouTube links
    youtubeShallowMatcher = youtubeShallowPattern.matcher(url);
    if (youtubeShallowMatcher.find()) {
      return SocialMediaLinkType.YOUTUBE_SHALLOW;
    } else {
      youtubeChannelMatcher = youtubeDeepPattern.matcher(url);
      youtubeVideoMatcher = youtubeVideoPattern.matcher(url);
      youtubePlaylistMatcher = youtubePlaylistPattern.matcher(url);

      if (youtubeChannelMatcher.find() || youtubeVideoMatcher.find() || youtubePlaylistMatcher.find()) {
        return SocialMediaLinkType.YOUTUBE_DEEP;
      }

    }

    // Vimeo links
    vimeoShallowMatcher = vimeoShallowPattern.matcher(url);
    if (vimeoShallowMatcher.find()) {
      return SocialMediaLinkType.VIMEO_SHALLOW;
    } else {
      vimeoDeepMatcher = vimeoDeepPattern.matcher(url);
      vimeoChannelMatcher = vimeoChannelPattern.matcher(url);
      vimeoPlayerMatcher = vimeoPlayerPattern.matcher(url);
      if (vimeoDeepMatcher.find() || vimeoChannelMatcher.find() || vimeoPlayerMatcher.find()) {
        return SocialMediaLinkType.VIMEO_DEEP;
      }
    }



    return SocialMediaLinkType.UNKNOWN;
  }
}
