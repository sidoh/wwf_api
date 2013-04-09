package org.sidoh.wwf_api;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import sun.misc.BASE64Decoder;

import java.io.Console;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convenience class for logging into Facebook and retrieving the oauth token that WWF uses to
 * authenticate with Facebook. This is the only session paramater necessary to interact with
 * the WWF API.
 */
public class AccessTokenRetriever {
  private static final BASE64Decoder BASE_64_DECODER = new BASE64Decoder();

  private static final String LOGIN_FORM_ID = "login_form";
  private static final String LOGIN_FORM_SUBMIT_BUTTON_VALUE = "Log In";

  private static final Pattern SIGNED_REQUEST_REGEX = Pattern.compile("name=\"signed_request\" value=\"[^.]+\\.([^\"]+)\"");

  private static final class Context {
    private final String username;
    private final String password;
    private final WebClient client;

    private Context(String username, String password) {
      Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);

      this.username = username;
      this.password = password;
      this.client = new WebClient(BrowserVersion.FIREFOX_2);
    }
  }

  /**
   * Retrieves access token by prompting user for facebook login credentials and calling
   * getAccessToken(String, String).
   *
   * @return access token for user
   */
  public String promptForAccessToken() throws IOException {
    Console console = System.console();

    return getAccessToken(console.readLine("Facebook username: "),
      String.valueOf(console.readPassword("Facebook password: ")));
  }

  /**
   * Retrieves the access token for a Facebook user having the provided username and password.
   * This only works for users who have already authorized the WWF app within Facebook.
   *
   * @param username
   * @param password
   * @return access token for the user. if user hasn't authorized WWF, this will return null.
   */
  public String getAccessToken(String username, String password) throws IOException {
    Context callContext = new Context(username, password);
    WebClient client = callContext.client;

    if ( login(callContext) ) {
      HtmlPage result = (HtmlPage) client.getPage("https://apps.facebook.com/wordswithfriends/");
      Matcher matcher = SIGNED_REQUEST_REGEX.matcher(result.asXml());
      if ( matcher.find() ) {
        JSONObject signedObject = (JSONObject) JSONValue.parse(new String(BASE_64_DECODER.decodeBuffer(matcher.group(1))));
        String accessToken = (String)signedObject.get("oauth_token");

        return accessToken;
      }
      else {
        throw new RuntimeException("Couldn't find signed request in source");
      }
    }
    else {
      throw new RuntimeException("Login failed");
    }
  }

  /**
   * Login to Facebook by finding the login form on the front page, filling it out, and
   * submitting it.
   *
   * @param callContext
   * @return true if login was successful
   * @throws IOException
   */
  protected boolean login(Context callContext) throws IOException {
    WebClient client = callContext.client;
    HtmlPage loginPage = (HtmlPage)client.getPage("https://www.facebook.com/");

    // Find the login form and submit it
    HtmlForm loginForm = findFormById(loginPage, LOGIN_FORM_ID);

    if ( loginForm == null ) {
      throw new RuntimeException("Couldn't find login form on homepage");
    }

    HtmlPage postLoginPage = submitLoginForm(callContext, loginForm);

    return findFormById(postLoginPage, LOGIN_FORM_ID) == null;
  }

  /**
   * Fill out and submit a login form
   *
   * @param callContext
   * @param form
   * @return the result of submitting the login form
   * @throws IOException
   */
  protected HtmlPage submitLoginForm(Context callContext, HtmlForm form) throws IOException {
    form.getInputByName("email").setValueAttribute(callContext.username);
    form.getInputByName("pass").setValueAttribute(callContext.password);
    return (HtmlPage) form.getInputByValue(LOGIN_FORM_SUBMIT_BUTTON_VALUE).click();
  }

  /**
   * Find a form with a particular DOM ID.
   *
   * @param page
   * @param formId
   * @return
   */
  protected static HtmlForm findFormById(Page page, String formId) {
    for (Object o : ((HtmlPage) page).getForms()) {
      HtmlForm form = (HtmlForm)o;

      if ( formId.equals(form.getId()) ) {
        return form;
      }
    }

    return null;
  }
}
