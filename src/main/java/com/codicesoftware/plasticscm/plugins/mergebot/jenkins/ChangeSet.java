package com.codicesoftware.plasticscm.plugins.mergebot.jenkins;

import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@ExportedBean(defaultVisibility = 999)
public class ChangeSet extends ChangeLogSet.Entry {

    public ChangeSet() {
        this("", "", "", "", "", null);
    }

    public ChangeSet(
        String version,
        String repoName,
        String repoServer,
        String user,
        String comment,
        Date date) {
        this.version = version;
        this.repoName = repoName;
        this.repoServer = repoServer;
        this.date = date != null ? (Date)date.clone() : null;
        this.comment = comment;
        this.user = user;
        items = new ArrayList<ChangeSet.Item>();
    }

    static DateFormat getDateFormat() {
        return new SimpleDateFormat(BuildObjectMetadata.DATE_SORTABLE_FORMAT);
    }

    @Override
    public Collection<String> getAffectedPaths() {
        Collection<String> paths = new ArrayList<String>(items.size());
        for (Item item : items) {
            paths.add(item.getPath());
        }
        return paths;
    }

    @Override
    public Collection<? extends ChangeLogSet.AffectedFile> getAffectedFiles() {
        return Collections.unmodifiableCollection(items);
    }

    @Override
    public ChangeLogSet getParent() {
        return (ChangeLogSet) super.getParent();
    }

    @Override
    protected void setParent(hudson.scm.ChangeLogSet parent) {
        super.setParent(parent);
    }

    @Override
    public User getAuthor() {
        return User.get(user);
    }

    @Override
    public String getMsg() {
        return comment;
    }

    @Override
    public String getCommitId() {
        return version;
    }

    @Override
    public long getTimestamp() {
        return date.getTime();
    }

    @Exported
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    @Exported
    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    @Exported
    public String getRepoServer() {
        return repoServer;
    }

    public void setRepoServer(String repoServer) {
        this.repoServer = repoServer;
    }

    @Exported
    public String getRepository() {
        return repoName + "@" + repoServer;
    }

    @Exported
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Exported
    public Date getDate() {
        return (Date)date.clone();
    }

    public void setDateStr(String dateStr) throws ParseException {
        this.date = getDateFormat().parse(dateStr);
    }

    @Exported
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Exported
    public List<Item> getItems() {
        return items;
    }

    public void add(ChangeSet.Item item) {
        items.add(item);
        item.setParent(this);
    }

    private String version;
    private String repoName;
    private String repoServer;
    private String user;
    private Date date;
    private String comment;
    private List<ChangeSet.Item> items;

    @ExportedBean(defaultVisibility = 999)
    public static class Item implements ChangeLogSet.AffectedFile {
        private String path;
        private String action;
        private ChangeSet parent;

        public Item() {
            this("", "");
        }

        public Item(String path, String action) {
            setPath(path);
            this.action = action;
        }

        @Exported
        public String getPath() {
            return this.path;
        }

        public void setPath(String path) {
            this.path = formatPath(path);
        }

        public ChangeSet getParent() {
            return this.parent;
        }

        public void setParent(ChangeSet parent) {
            this.parent = parent;
        }

        @Exported
        public String getAction() {
            return this.action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        @Exported
        @Override
        public EditType getEditType() {
            if (action.equals(KIND_ADDED))
                return EditType.ADD;

            if (action.equals(KIND_DELETED))
                return EditType.DELETE;

            return EditType.EDIT;
        }

        static String formatPath(String path) {
            path = path.replace('\\', '/');

            return path.startsWith("/")
                ? path.replaceFirst("^/*", "")
                : path;
        }

        static String KIND_ADDED = "added";
        static String KIND_DELETED = "deleted";
        static String KIND_CHANGED = "changed";
    }
}
