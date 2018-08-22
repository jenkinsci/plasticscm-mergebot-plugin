package com.codicesoftware.plasticscm.plugins.mergebot.jenkins;

import hudson.model.Run;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import hudson.util.Digester2;
import org.apache.commons.digester.Digester;
import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

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
        Digester digester = new Digester2();
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
