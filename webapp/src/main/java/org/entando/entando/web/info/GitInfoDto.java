package org.entando.entando.web.info;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.Properties;

@JsonInclude(Include.NON_EMPTY)
public class GitInfoDto extends AbstractInfoDto {

    private static final String COMMIT_TIME_KEY = "commit.time";
    private static final String COMMIT_ID_FULL_KEY = "commit.id.full";

    private final Commit commit;
    public GitInfoDto(Properties entries) {
        super(processEntries(entries));
        commit = new Commit(entries);

    }

    public String getBranch() {
        return get("branch");
    }

    public Commit getCommit() {
        return commit;
    }


    private static Properties processEntries(Properties properties) {
        coercePropertyToEpoch(properties, COMMIT_TIME_KEY);
        coercePropertyToEpoch(properties, "build.time");
        Object commitId = properties.get("commit.id");
        if (commitId != null) {
            // Can get converted into a map, so we copy the entry as a nested key
            properties.put(COMMIT_ID_FULL_KEY, commitId);
        }
        return properties;
    }


    public static class Commit extends AbstractInfoDto {

        public Commit(Properties entries) {
            super(GitInfoDto.processEntries(entries));
        }

        public String getId() {
            String shortId = get("commit.id.abbrev");
            if (shortId != null) {
                return shortId;
            }
            String id = get(COMMIT_ID_FULL_KEY);
            if (id == null) {
                return null;
            }
            return (id.length() > 7) ? id.substring(0, 7) : id;
        }

        public String getTime() {
            return get(COMMIT_TIME_KEY);
        }

    }


}
