<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.*" %>
<%@ page import="javax.mail.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.user.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page import="com.ecyrd.jspwiki.util.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>

<%! 
    Logger log = Logger.getLogger("JSPWiki");
    
    String message = null;
    public boolean resetPassword(WikiEngine wiki, HttpServletRequest request) 
    {
        // Reset pw for account name
        String name = request.getParameter("name");
        UserDatabase userDatabase = wiki.getUserManager().getUserDatabase();
        boolean success = false;
        try 
        {
            UserProfile profile = null;
            try 
            {
                profile = userDatabase.find(name);
            }
            catch (NoSuchPrincipalException e) 
            {
                // Try email as well
            }
            if (profile == null) 
            {
                profile = userDatabase.findByEmail(name);
            }
			
			String email = profile.getEmail();

			String randomPassword = TextUtil.generateRandomPassword();

			// Try sending email first, as that is more likely to fail.
            
             String mailMessage = "As requested, your new password for login '"
                    + profile.getLoginName() + "' is '" + randomPassword + "'.\n\n" +
                    "You may log in at "
                    + wiki.getURLConstructor().makeURL(WikiContext.NONE, "Login.jsp", true, "") + ".\n\n"
                    + "--" + wiki.getApplicationName();
 			 
 			MailUtil.sendMessage( wiki,
                                    email,
 			                      "New password for " + wiki.getApplicationName(),
 			                      mailMessage );
		
            log.info("User "+email+" requested and received a new password.");
            
			// Mail succeeded.  Now reset the password.
			// If this fails, we're kind of screwed, because we already emailed.
			profile.setPassword(randomPassword);
			userDatabase.save(profile);
			userDatabase.commit();
			success = true;
        }
        catch (NoSuchPrincipalException e) 
        {
            message = "No user or email '" + name + "' was found.";
            log.info("Tried to reset password for non-existent user '" + name + "'");
        }
        catch (SendFailedException e) 
        {
            message = "Internal error: couldn't send the email!  Contact the site administrator, please.";
            log.error("Tried to reset password and got SendFailedException: " + e);
        }
        catch (AuthenticationFailedException e) 
        {
            message = "Internal error: couldn't send the email!  Contact the site administrator, please.";
            log.error("Tried to reset password and got AuthenticationFailedException: " + e);            
        }
        catch (Exception e) 
        {
            message = "Internal error. Contact the site administrator, please.";
            log.error("Tried to reset password and got another exception: " + e);
        }
        return success;
    }
%>

<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );

	//Create wiki context like in Login.jsp: 
    //don't check for access permissions: if you have lost your password you cannot login!
	WikiContext wikiContext = (WikiContext) pageContext.getAttribute( WikiTagBase.ATTR_CONTEXT, PageContext.REQUEST_SCOPE );
	
	// If no context, it means we're using container auth.  So, create one anyway
	if( wikiContext == null )
	{
	    wikiContext = wiki.createContext( request, WikiContext.LOGIN );
	    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
	                              wikiContext,
	                              PageContext.REQUEST_SCOPE );
	}

    WikiSession wikiSession = wikiContext.getWikiSession(); 
    String action  = request.getParameter("action");

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    response.setHeader( "Cache-control", "max-age=0" );
    response.setDateHeader( "Expires", new Date().getTime() );
    response.setDateHeader( "Last-Modified", new Date().getTime() );

%>


<!DOCTYPE html 
     PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
     "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>

<head>
  <title><wiki:Variable var="applicationname" />: <wiki:PageName /></title>
  <wiki:Include page="commonheader.jsp"/>
</head>

<body class="view" bgcolor="#FFFFFF">
<a name="Top"></a>

<div id="wikibody" >
  <wiki:Include page="Header.jsp" />

  <!-- Removed application logo here since it may conflict with other templates.
       TODO: LostPassword.jsp needs to be properly integrated into the templating system 
       in order to be translatable -->

  <div id="page">
  <%
      boolean done = false;
  
      if ((action != null) && (action.equals("resetPassword"))) {
	      if (resetPassword(wiki, request)) {
	          done = true;
	          wikiSession.addMessage("A new password has been emailed to the requested account.");
	          %>

            <h3>Password reset</h3>

            <wiki:Messages div="information" />

            <p><a href=Login.jsp>Click here</a> to log in
            once you retrieve your new password.</p>
            <%
	      }
	      else
	      {
	          // Error
              wikiSession.addMessage(message);
	          %>

              <h3>Unable to reset password.  Please try again.</h3>

              <wiki:Messages div="error" />

              <%
	      }          
      }

      // Display something to ask for a username

      if (!done) {
      %>
      <div>Lost or forgot your password?  Enter your account name or email here:
      <form method="POST" accept-charset="UTF-8">
        <input type="hidden" name="action" value="resetPassword"/>
        <input type="text" name="name"/>
        <input type="submit" name="Submit" value="Reset password!"/>
      </form>
      </div>
      
    <%} %>
  </div>

  <div id="favorites"><wiki:Include page="Favorites.jsp"/></div>

  <wiki:Include page="Footer.jsp" />

  <div style="clear:both; height:0px;" > </div>

</div>
<a name="Bottom"></a>

</body>
</html>