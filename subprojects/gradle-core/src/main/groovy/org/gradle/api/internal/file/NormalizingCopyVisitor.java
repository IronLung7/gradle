/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.file;

import org.gradle.api.file.CopyAction;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.RelativePath;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A {@link CopySpecVisitor} which cleans up the tree as it is visited. Removes duplicate and empty directories and
 * adds in missing directories.
 */
public class NormalizingCopyVisitor implements CopySpecVisitor {
    private final CopySpecVisitor visitor;
    private final Set<RelativePath> visitedDirs = new HashSet<RelativePath>();
    private final Map<RelativePath, FileVisitDetails> pendingDirs = new HashMap<RelativePath, FileVisitDetails>();

    public NormalizingCopyVisitor(CopySpecVisitor visitor) {
        this.visitor = visitor;
    }

    public void startVisit(CopyAction action) {
        visitor.startVisit(action);
    }

    public void endVisit() {
        visitedDirs.clear();
        pendingDirs.clear();
        visitor.endVisit();
    }

    public void visitSpec(ReadableCopySpec spec) {
        visitor.visitSpec(spec);
    }

    private void maybeVisit(RelativePath path) {
        if (path == null || path.getParent() == null || !visitedDirs.add(path)) {
            return;
        }
        maybeVisit(path.getParent());
        FileVisitDetails dir = pendingDirs.remove(path);
        if (dir == null) {
            dir = new FileVisitDetailsImpl(path);
        }
        visitor.visitDir(dir);
    }

    public void visitFile(FileVisitDetails fileDetails) {
        maybeVisit(fileDetails.getRelativePath().getParent());
        visitor.visitFile(fileDetails);
    }

    public void visitDir(FileVisitDetails dirDetails) {
        RelativePath path = dirDetails.getRelativePath();
        if (!visitedDirs.contains(path)) {
            pendingDirs.put(path, dirDetails);
        }
    }

    public boolean getDidWork() {
        return visitor.getDidWork();
    }

    private static class FileVisitDetailsImpl extends AbstractFileTreeElement implements FileVisitDetails {
        private final RelativePath path;
        private long lastModified = System.currentTimeMillis();

        private FileVisitDetailsImpl(RelativePath path) {
            this.path = path;
        }

        @Override
        public String getDisplayName() {
            return path.toString();
        }

        public void stopVisiting() {
            throw new UnsupportedOperationException();
        }

        public File getFile() {
            throw new UnsupportedOperationException();
        }

        public boolean isDirectory() {
            throw new UnsupportedOperationException();
        }

        public long getLastModified() {
            return lastModified;
        }

        public long getSize() {
            throw new UnsupportedOperationException();
        }

        public InputStream open() {
            throw new UnsupportedOperationException();
        }

        public RelativePath getRelativePath() {
            return path;
        }
    }
}
