package mse.helpers;

import java.io.PrintWriter;

/**
 * Created by Michael on 02/12/2015.
 */
public class HtmlHelper {

    public static void writeHtmlHeader(PrintWriter pw, String title, String mseStyleLocation) {

        String bootstrapLocation = "../../bootstrap/css/bootstrap.css";

        pw.println("<!DOCTYPE html>\n" +
                "\n" +
                "<html>" +
                "\n" +
                "<head>\n" +
                "\t<title>" + title + "</title>\n" +
                "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + mseStyleLocation + "\">\n" +
                "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + bootstrapLocation + "\">\n" +
                "</head>");
    }
}
