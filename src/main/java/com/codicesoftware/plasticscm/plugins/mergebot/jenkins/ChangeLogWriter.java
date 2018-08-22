package com.codicesoftware.plasticscm.plugins.mergebot.jenkins;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChangeLogWriter {

    public static void writeLog(ChangeSet buildObject, File changelogFile) throws IOException {
        try (Writer outputWriter = new OutputStreamWriter(new FileOutputStream(changelogFile), StandardCharsets.UTF_8);
             PrintWriter writer = new PrintWriter(outputWriter)
        ) {
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<changelog>");

            writer.println(String.format("\t<changeset version=\"%s\">", escapeForXml(buildObject.getVersion())));
            writer.println(String.format("\t\t<date>%s</date>", getDateStr(buildObject.getDate())));
            writer.println(String.format("\t\t<user>%s</user>", escapeForXml(buildObject.getUser())));
            writer.println(String.format("\t\t<comment>%s</comment>", escapeForXml(buildObject.getComment())));
            writer.println("\t\t<items>");
            for (ChangeSet.Item item : buildObject.getItems()) {
                writer.println("\t\t\t<item>");
                writer.println(String.format("\t\t\t\t<action>%s</action>", item.getAction()));
                writer.println(String.format("\t\t\t\t<path>%s</path>", escapeForXml(item.getPath())));
                writer.println("\t\t\t</item>");
            }
            writer.println("\t\t</items>");
            writer.println("\t</changeset>");

            writer.println("</changelog>");
        }
    }

    private static String getDateStr(Date date) {
        return ChangeSet.getDateFormat().format(date);
    }

    private static String escapeForXml(String value) {
        if(value == null) {
            return null;
        }

        int size = value.length();
        char ch;
        StringBuilder escapedString = new StringBuilder(size);
        for(int i = 0; i < size; i ++) {
            ch = value.charAt(i);
            switch(ch)
            {
                case '&'  : escapedString.append("&amp;");  break;
                case '<'  : escapedString.append("&lt;");   break;
                case '>'  : escapedString.append("&gt;");   break;
                case '\'' : escapedString.append("&apos;"); break;
                case '\"' : escapedString.append("&quot;");break;
                default:    escapedString.append(ch);
            }
        }

        return escapedString.toString();
    }
}
