package com.gh4a.loader;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.util.EncodingUtils;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.gh4a.Gh4Application;
import com.gh4a.utils.StringUtils;

public class GitModuleParserLoader extends BaseLoader<Map<String, String>> {

    private String mRepoOwner;
    private String mRepoName;
    private String mPath;
    private String mRef;

    public GitModuleParserLoader(Context context, String repoOwner, String repoName, String path, String ref) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPath = path;
        mRef = ref;
    }

    @Override
    public Map<String, String> doLoadInBackground() throws IOException {
        ContentsService contentService = (ContentsService)
                Gh4Application.get().getService(Gh4Application.CONTENTS_SERVICE);
        List<RepositoryContents> contents =
                contentService.getContents(new RepositoryId(mRepoOwner, mRepoName), mPath, mRef);

        if (contents == null || contents.isEmpty()) {
            return null;
        }
        String data = new String(EncodingUtils.fromBase64(contents.get(0).getContent()));
        if (StringUtils.isBlank(data)) {
            return null;
        }
        Map<String, String> gitModuleMap = new HashMap<>();
        String[] lines = data.split("\n");
        String path = null;
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("path = ")) {
                String[] pathPart = line.split("=");
                path = pathPart[1].trim();
            } else if (line.startsWith("url = ")) {
                String[] urlPart = line.split("=");

                String url = urlPart[1].trim().replace("github.com:", "github.com/");
                int pos = url.indexOf("git@");
                if (pos == 0) {
                    url = "ssh://" + url.substring(4);
                }

                Uri uri = Uri.parse(url);
                if (!TextUtils.equals(uri.getHost(), "github.com")) {
                    continue;
                }
                List<String> pathSegments = uri.getPathSegments();
                if (pathSegments == null || pathSegments.size() < 2) {
                    continue;
                }
                String user = pathSegments.get(pathSegments.size() - 2);
                String repo = pathSegments.get(pathSegments.size() - 1);

                pos = repo.lastIndexOf(".");
                if (pos != -1) {
                    repo = repo.substring(0, pos);
                }
                gitModuleMap.put(path, user + "/" + repo);
            }
        }
        return gitModuleMap;
    }
}
