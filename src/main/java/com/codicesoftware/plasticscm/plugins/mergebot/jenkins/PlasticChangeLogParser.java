package com.codicesoftware.plasticscm.plugins.mergebot.jenkins;

import hudson.model.Run;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import org.apache.commons.digester3.Digester;
import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class PlasticChangeLogParser extends ChangeLogParser {
    @Override
    public ChangeLogSet parse(
            Run run, RepositoryBrowser<?> browser, File changelogFile)
            throws IOException, SAXException {
        Reader reader = new InputStreamReader(
            new FileInputStream(changelogFile), StandardCharsets.UTF_8);
        try {
            return parse(run, browser, reader);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public ChangeLogSet parse(
            Run<?,?> run, RepositoryBrowser<?> browser, Reader reader)
            throws IOException, SAXException {
        List<ChangeSet> changesetList = new ArrayList<ChangeSet>();
        Digester digester = new Digester();

        digester.setXIncludeAware(false);

        if (!Boolean.getBoolean(PlasticChangeLogParser.class.getName() + ".UNSAFE")) {
            try {
                digester.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                digester.setFeature("http://xml.org/sax/features/external-general-entities", false);
                digester.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                digester.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            }
            catch ( ParserConfigurationException ex) {
                throw new SAXException("Failed to securely configure CVS changelog parser", ex);
            }
        }

        digester.push(changesetList);

        digester.addObjectCreate("*/changeset", ChangeSet.class);
        digester.addSetProperties("*/changeset");
        digester.addBeanPropertySetter("*/changeset/date", "dateStr");
        digester.addBeanPropertySetter("*/changeset/user");
        digester.addBeanPropertySetter("*/changeset/comment");
        digester.addBeanPropertySetter("*/changeset/repname", "repoName");
        digester.addBeanPropertySetter("*/changeset/repserver", "repoServer");
        digester.addSetNext("*/changeset", "add");

        digester.addObjectCreate("*/changeset/items/item", ChangeSet.Item.class);
        digester.addSetProperties("*/changeset/items/item");
        digester.addBeanPropertySetter("*/changeset/items/item/action", "action");
        digester.addBeanPropertySetter("*/changeset/items/item/path", "path");
        digester.addSetNext("*/changeset/items/item", "add");

        digester.parse(reader);

        return new PlasticChangeLogSet(run, browser, changesetList);
    }
}
