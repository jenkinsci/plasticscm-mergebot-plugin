package com.codicesoftware.plasticscm.plugins.mergebot.jenkins;

import hudson.model.Run;
import hudson.scm.RepositoryBrowser;

import java.util.Iterator;
import java.util.List;


public class PlasticChangeLogSet extends hudson.scm.ChangeLogSet<ChangeSet> {
    private final List<ChangeSet> buildObjectList;

    public PlasticChangeLogSet(
        Run<?, ?> run, RepositoryBrowser<?> browser, List<ChangeSet> buildObjectList) {
        super(run, browser);
        this.buildObjectList = buildObjectList;

        for (ChangeSet changeset : buildObjectList) {
            changeset.setParent(this);
        }
    }

    @Override
    public boolean isEmptySet() {
        return buildObjectList.isEmpty();
    }

    public Iterator<ChangeSet> iterator() {
        return buildObjectList.iterator();
    }
}