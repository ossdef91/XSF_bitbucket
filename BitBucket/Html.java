/*
 *
 * Copyright (c) 2001-2017 Dassault Systemes.
 * All Rights Reserved
 *
 * Code to generate html
 * This is old-fashioned HTML 1.0 type generations. Obviously, a more
 * modern approach would be better. This is just for illustration.
 *
 */

package com.dassaultsystemes.xsoftware.scmdaemon.filesys;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;


public class Html {
    private String contextPath;
    private String webappPath;
    private String webapp;
    
    // Constructor: Store the page context path and sub-path
    public Html(String contextPath, String path) {
        this.contextPath = contextPath;
        webappPath = this.contextPath + path;
        webapp = Paths.get(webappPath).getFileName().toString();
    }
    
    public String resourcePath(String relpath) {
        return String.format("%s/%s",  webappPath, relpath);
    }
    
    public String includeScript(String path) {
        return String.format("<script src=\"%s\"></script>\n", contextPath + path);
    }
  
    public String loadScriptModule(String module) {
        String html = String.format("<script>  requirejs.config({baseUrl: \"%s/webapps/%s\", paths: {\"DS/%s\": \"%s/webapps/%s\"}, waitSeconds: 15});\nrequire(['%s']);</script>\n",
                contextPath, webapp, webapp, contextPath, webapp, module);
        return html;
    }
    
    public String linkCSS(String path) {
        return String.format("<link REL=\"stylesheet\" TYPE=\"text/css\" HREF=\"%s\">\n", resourcePath(path));
    }
  
    public String linkCSS() {
        return String.format("<link REL=\"stylesheet\" TYPE=\"text/css\" HREF=\"%s\">\n", resourcePath(webapp + ".css"));
    }
    
    public String tableStartLeft (boolean paragraph, boolean stretch, String id) {
        String p = paragraph ? "<p>" : "";
        String width = stretch ? "width='100%'" : "";
        String idTxt = id.compareTo("") == 0 ? "" : String.format("id=%s", id);

        String html = String.format("%s<TABLE class=LEFTHEADERTBL %s %s bgcolor=#B0C8D6 cellspacing=1 cellpadding=3 border=0 frame=border>\n",
                p, idTxt, width);

        return html;
    }
  
    public String tableEnd() {
        return "</TABLE>\n";
    }
  
    public String formStart(String name, String method, String action) {
        return String.format("<form name=\"%s\" id=\"%s\" method=\"%s\" action=\"%s\">\n", name, name, method, action);
    }
  
    public String formStart(String name, String method, String action, String onSubmit) {
        return String.format("<form name=\"%s\" id=\"%s\" method=\"%s\" action=\"%s\" onSubmit=\"%s\">\n", name, name, method, action, onSubmit);
    }
    
    public String formEnd() {
        return "</FORM>\n";
    }
  
    public String pageEnd() {
        return "</BODY>\n</HTML>\n";
    }
    
    public String inputField(String type, String title, String name, String value, int length) {
         return String.format("<TR><th>%s</th><td><input type=\"%s\" name=\"%s\" id=\"%s\" value=\"%s\" size=%d></td></TR>\n", 
                 title, type, name, name, value, length);
    }
  
    public String hiddenInput(String name, String value) {
        return String.format("<input type=\"hidden\" name=\"%s\" id=\"%s\" value=\"%s\">\n", name, name, value);
    }
  
    public String myButton (String type, String onclick, String text, String status) {
        String typeTxt = String.format(" type='%s'", type);
        String mapped = text.replaceAll("'", "&#039;");
        mapped = mapped.replaceAll("\"", "&quot;");
        String value = String.format(" value='%s'", mapped);

        String onClickTxt = "";
        if (onclick.compareTo("") != 0) {
            onClickTxt = String.format(" onClick='%s'", onclick);
        }

        return String.format("<INPUT %s %s%s %s>\n", typeTxt, value, onClickTxt, status);
    }
    
    public String myButton (String type, String onclick, String text) {
        String typeTxt = String.format(" type='%s'", type);
        String mapped = text.replaceAll("'", "&#039;");
        mapped = mapped.replaceAll("\"", "&quot;");
        String value = String.format(" value='%s'", mapped);

        String onClickTxt = "";
        if (onclick.compareTo("") != 0) {
            onClickTxt = String.format(" onClick='%s'", onclick);
        }

        return String.format("<INPUT %s %s%s>\n", typeTxt, value, onClickTxt);
    }
    
    public String controlBar (int colspan, List<String> extra) {

        String html = String.format("<TR><th class=CONTROLS colspan=%-3d>\n", colspan);

        if ( !extra.isEmpty() ) {
            for (String button : extra)
                html += button;
        }

        html += "</th></TR>\n";
        return html;
    }
    
    public String pageStart (String subtitle) {
        
        return "<html>\n";
    }
  
    public String hLink (String href, String text) {
        String alink = "<a";
        alink += String.format(" href=%s", href);
        alink += String.format(">%s</a>", text);
        return alink;
    }
    
    public String hLink (String title, String href, String text, String[] args) {
        String alink = "<a";

        alink += String.format(" title='%s'", title);
        alink += String.format(" href=%s", href);

        String join;
        for (int i = 0; i < args.length; i += 2) {
            String name = args[i];
            String val = (args.length - i) == 1 ? "" : args[i + 1];
            join = i == 0 ? "?" : "&";
            alink += String.format("%s%s=%s", join, name, val);
        }

        alink += String.format(">%s</a>", text);
        return alink;
    }
    
    public String dialogSeparator(String separator) {
        return String.format("<br>%s",  separator);
    }
  
    public String setClass(String theClass, String text) {
        return String.format("<SPAN CLASS=%s>%s</SPAN>", theClass, text);
    }
    
    private String resultControlBar (String destination) {

        List<String> myButtons = Arrays.asList(
           myButton("button", destination, Utils.getStr("OK")));

        return controlBar(2, myButtons);
    }
    
    public String resultHeader(boolean success) {
          String status;
          
          if ( success ) 
              status = Utils.getStr("OpSuccessful");
          else
              status = Utils.getStr("OpFailed");
          String result = "<HTML><HEAD>\n";
          result += linkCSS();
          result += "<TITLE>" + status + "</TITLE>\n";
          result += "</HEAD>\n";
          result += "<BODY>\n";
          result += "<div class='TITLEBAR'>" + status + "</div>\n";
          result += tableStartLeft(true,  false, "");
          return result;
      }
  
      public String resultFooter(String destination) {
             String result = resultControlBar(destination);
          result += tableEnd();
          result += "</BODY>\n";
          result += "</HTML>\n";
          return result;
      }
}
