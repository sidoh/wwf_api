package org.sidoh.wwf_api;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import sun.misc.BASE64Decoder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccessTokenRetriever {
  private final WebClient client = new WebClient(BrowserVersion.FIREFOX_2);

  private static final BASE64Decoder BASE_64_DECODER = new BASE64Decoder();

  private static final String LOGIN_FORM_ID = "login_form";
  private static final String LOGIN_FORM_SUBMIT_BUTTON_VALUE = "Log In";
  private static final String POST_LOGIN_PAGE_TITLE = "Facebook";

  private static final Pattern SIGNED_REQUEST_REGEX = Pattern.compile("name=\"signed_request\" value=\"[^.]+\\.([^\"]+)\"");

  /**
   * Retrieves the access token for a Facebook user having the provided username and password
   *
   * @param username
   * @param password
   * @return
   */
  public synchronized String getAccessToken(String username, String password) throws IOException {
    if ( login(username, password) ) {
      HtmlPage result = (HtmlPage) client.getPage("https://apps.facebook.com/wordswithfriends/");
      Matcher matcher = SIGNED_REQUEST_REGEX.matcher(result.asXml());
      if ( matcher.find() ) {
        JSONObject signedObject = (JSONObject) JSONValue.parse(new String(BASE_64_DECODER.decodeBuffer(matcher.group(1))));
        return (String)signedObject.get("oauth_token");
      }
      else {
        System.out.println(result.asXml());
        throw new RuntimeException("Couldn't find signed request in source");
      }
    }
    else {
      throw new RuntimeException("Login failed");
    }
  }

  protected boolean login(String username, String password) throws IOException {
    HtmlPage loginPage = (HtmlPage)client.getPage("https://www.facebook.com/");

    // Find the login form and submit it
    HtmlForm loginForm = findFormById(loginPage, LOGIN_FORM_ID);

    if ( loginForm == null ) {
      throw new RuntimeException("Couldn't find login form on homepage");
    }

    HtmlPage postLoginPage = submitLoginForm(username, password, loginForm);

    return postLoginPage.getTitleText().equals(POST_LOGIN_PAGE_TITLE);
  }

  protected HtmlPage submitLoginForm(String username, String password, HtmlForm form) throws IOException {
    form.getInputByName("email").setValueAttribute(username);
    form.getInputByName("pass").setValueAttribute(password);
    return (HtmlPage) form.getInputByValue(LOGIN_FORM_SUBMIT_BUTTON_VALUE).click();
  }

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
