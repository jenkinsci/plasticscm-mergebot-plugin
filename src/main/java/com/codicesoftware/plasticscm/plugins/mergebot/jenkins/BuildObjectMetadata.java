package com.codicesoftware.plasticscm.plugins.mergebot.jenkins;

import hudson.AbortException;
import hudson.model.TaskListener;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static hudson.Util.singleQuote;

@ExportedBean(defaultVisibility=999)
public class BuildObjectMetadata {

    public static final String DATE_SORTABLE_FORMAT = "yyyy'-'MM'-'dd'T'HH':'mm':'ss";

    public static ChangeSet retrieveFromServer(
        CmExeWrapper cmExeWrapper, UpdateToSpec updateToSpec, TaskListener listener)
        throws AbortException {

        ChangeSet buildObject = buildObjectMetadata(cmExeWrapper, updateToSpec, listener);

        List<ChangeSet.Item> buildObjectItems = buildChangedPaths(cmExeWrapper, updateToSpec, listener);

        buildObject.setItems(buildObjectItems);

        return buildObject;
    }

    private static ChangeSet buildObjectMetadata(
        CmExeWrapper cmExeWrapper, UpdateToSpec updateToSpec, TaskListener listener)
        throws AbortException {

        ChangeSet buildObject = new ChangeSet();
        buildObject.setRepoName(updateToSpec.getRepName());
        buildObject.setRepoServer(updateToSpec.getRepServer());
        buildObject.setVersion(updateToSpec.getFullObjectSpec());

        String queryObjectType = updateToSpec.getObjectType().toString().toLowerCase();
        String whereClause = getWhereClause(updateToSpec);

        String findClause = String.format("%s %s", queryObjectType, whereClause);

        String formatClause =
            "--format=" + fieldSeparator +
                "COMMENT={comment}" + fieldSeparator +
                "DATE={date}" + fieldSeparator +
                "OWNER={owner}" + fieldSeparator;

        String dateFormatClause = "--dateformat=" + DATE_SORTABLE_FORMAT;
        String repositoryClause = "on repository " +
            singleQuote(updateToSpec.getRepName() + "@" + updateToSpec.getRepServer());

        Reader reader = null;
        try {
            reader = cmExeWrapper.execute(
                new String[]{ "find", findClause, formatClause, dateFormatClause, repositoryClause, "--nototal"});

            fillObjectMetadata(reader, buildObject);
        } catch (Exception e) {
            listener.fatalError(e.getMessage());
            throw new AbortException(e.getMessage());
        } finally {
            IOUtils.closeQuietly(reader);
        }

        return buildObject;
    }

    private static String getWhereClause(UpdateToSpec updateToSpec) {
        if (updateToSpec.getObjectType() == SpecObjectType.Changeset)
            return "where changesetid=" + updateToSpec.getObjectName();

        if (updateToSpec.getObjectType() == SpecObjectType.Shelve)
            return "where shelveid=" + updateToSpec.getObjectName();

        if (updateToSpec.getObjectType() == SpecObjectType.Label)
            return "where name=" +  singleQuote(updateToSpec.getObjectName());

        if (updateToSpec.getObjectType() == SpecObjectType.Branch)
            return "where " + getClauseForSingleTrackedBranch(updateToSpec.getObjectName());

        return null;
    }

    private static String getClauseForSingleTrackedBranch(final String branch) {
        int nameIndex = branch.lastIndexOf("/");
        if(nameIndex == -1)
            return String.format("name='%s'", branch);

        if(nameIndex == 0)
            return String.format("parent=-1 and name='%s'", branch.substring(1));

        return String.format("parent='%s' and name='%s'",
            branch.substring(0, nameIndex), branch.substring(nameIndex + 1));
    }

    private static void fillObjectMetadata(Reader reader, ChangeSet buildObject) throws IOException, ParseException {
        BufferedReader bufferedReader = null;

        String compilePatternStr =
            fieldSeparator + "COMMENT=(.*)" +
            fieldSeparator  + "DATE=(.+)" +
            fieldSeparator + "OWNER=(.+)" +
            fieldSeparator + "\\s*$";

        Pattern detailsRegex = Pattern.compile(compilePatternStr);

        try {
            bufferedReader = new BufferedReader(reader);
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                Matcher matcher = detailsRegex.matcher(line);
                if (!matcher.find())
                    continue;

                buildObject.setComment(matcher.group(1));
                buildObject.setDateStr(matcher.group(2));
                buildObject.setUser(matcher.group(3));
            }
        }
        finally
        {
            if (bufferedReader != null)
                bufferedReader.close();
        }
    }

    private static List<ChangeSet.Item> buildChangedPaths(
        CmExeWrapper cmExeWrapper, UpdateToSpec updateToSpec, TaskListener listener)
        throws AbortException {

        String formatClause =
            "--format=" + fieldSeparator +
                "{status}" + fieldSeparator +
                "{path}" + fieldSeparator +
                "{srccmpath}" + fieldSeparator;

        Reader reader = null;
        try {
            reader = cmExeWrapper.execute(
                new String[]{ "diff", updateToSpec.getFullObjectSpec(), "--repositorypaths", formatClause});

            return parseChangedPaths(reader);
        } catch (Exception e) {
            listener.fatalError(e.getMessage());
            throw new AbortException(e.getMessage());
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    private static List<ChangeSet.Item> parseChangedPaths(Reader reader)
        throws IOException {

        List<ChangeSet.Item> items = new ArrayList<ChangeSet.Item>();
        BufferedReader bufferedReader = null;
        String compilePatternStr =
            fieldSeparator + "(.*)" + fieldSeparator + "(.+)" + fieldSeparator + "(.*)" + fieldSeparator;

        Pattern detailsRegex = Pattern.compile(compilePatternStr);

        try {
            bufferedReader = new BufferedReader(reader);
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                Matcher matcher = detailsRegex.matcher(line);
                if (!matcher.find())
                    continue;

                String changeType = matcher.group(1);

                if (changeType.equals("M")) {
                    ChangeSet.Item dstMove = new ChangeSet.Item(
                        trimQuotes(matcher.group(2)),
                        ChangeSet.Item.KIND_ADDED);

                    ChangeSet.Item srcMove = new ChangeSet.Item(
                        trimQuotes(matcher.group(3)),
                        ChangeSet.Item.KIND_DELETED);

                    items.add(dstMove);
                    items.add(srcMove);
                    continue;
                }

                ChangeSet.Item item = new ChangeSet.Item(
                    trimQuotes(matcher.group(2)),
                    getAction(changeType));

                items.add(item);
            }
        } finally {
            if (bufferedReader != null)
                bufferedReader.close();
        }

        return items;
    }

    private static String getAction(String changeType) {
        if (changeType.equals("A"))
            return ChangeSet.Item.KIND_ADDED;

        if (changeType.equals("D"))
            return ChangeSet.Item.KIND_DELETED;

        return ChangeSet.Item.KIND_CHANGED;
    }

    private static String trimQuotes(String value) {
        return value.replaceFirst("^\\\"", "").replaceFirst("\\\"$", "");
    }

    private static final String fieldSeparator = "##DEF_SEP##";
}
