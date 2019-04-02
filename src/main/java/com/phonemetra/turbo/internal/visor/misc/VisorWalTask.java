/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.phonemetra.turbo.internal.visor.misc;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import com.phonemetra.turbo.TurboSQLCheckedException;
import com.phonemetra.turbo.TurboSQLException;
import com.phonemetra.turbo.TurboSQLLogger;
import com.phonemetra.turbo.cluster.ClusterNode;
import com.phonemetra.turbo.compute.ComputeJobResult;
import com.phonemetra.turbo.configuration.DataStorageConfiguration;
import com.phonemetra.turbo.configuration.TurboSQLConfiguration;
import com.phonemetra.turbo.internal.GridKernalContext;
import com.phonemetra.turbo.internal.pagemem.wal.WALPointer;
import com.phonemetra.turbo.internal.processors.cache.persistence.GridCacheDatabaseSharedManager;
import com.phonemetra.turbo.internal.processors.cache.persistence.filename.PdsFolderSettings;
import com.phonemetra.turbo.internal.processors.cache.persistence.wal.FileWALPointer;
import com.phonemetra.turbo.internal.processors.cache.persistence.wal.FileWriteAheadLogManager;
import com.phonemetra.turbo.internal.processors.task.GridInternal;
import com.phonemetra.turbo.internal.util.typedef.internal.U;
import com.phonemetra.turbo.internal.visor.VisorJob;
import com.phonemetra.turbo.internal.visor.VisorMultiNodeTask;
import com.phonemetra.turbo.internal.visor.VisorTaskArgument;
import com.phonemetra.turbo.resources.LoggerResource;
import org.jetbrains.annotations.Nullable;

/**
 * Performs WAL cleanup clusterwide.
 */
@GridInternal
public class VisorWalTask extends VisorMultiNodeTask<VisorWalTaskArg, VisorWalTaskResult, Collection<String>> {
    /** */
    private static final long serialVersionUID = 0L;

    /** Pattern for segment file names. */
    private static final Pattern WAL_NAME_PATTERN = Pattern.compile("\\d{16}\\.wal");

    /** Pattern for compacted segment file names. */
    private static final Pattern WAL_SEGMENT_FILE_COMPACTED_PATTERN = Pattern.compile("\\d{16}\\.wal\\.zip");

    /** WAL archive file filter. */
    private static final FileFilter WAL_ARCHIVE_FILE_FILTER = new FileFilter() {
        @Override public boolean accept(File file) {
            return !file.isDirectory() && (WAL_NAME_PATTERN.matcher(file.getName()).matches() ||
                    WAL_SEGMENT_FILE_COMPACTED_PATTERN.matcher(file.getName()).matches());
        }
    };

    /** {@inheritDoc} */
    @Override protected VisorWalJob job(VisorWalTaskArg arg) {
        return new VisorWalJob(arg, debug);
    }

    /** {@inheritDoc} */
    @Override protected Collection<UUID> jobNodes(VisorTaskArgument<VisorWalTaskArg> arg) {
        Collection<ClusterNode> srvNodes = turboSQL.cluster().forServers().nodes();
        Collection<UUID> ret = new ArrayList<>(srvNodes.size());

        VisorWalTaskArg taskArg = arg.getArgument();

        Set<String> nodeIds = taskArg.getConsistentIds() != null ? new HashSet<>(arg.getArgument().getConsistentIds())
                                : null;

        if (nodeIds == null) {
            for (ClusterNode node : srvNodes)
                ret.add(node.id());
        }
        else {
            for (ClusterNode node : srvNodes) {
                if (nodeIds.contains(node.consistentId().toString()))
                    ret.add(node.id());
            }
        }

        return ret;
    }

    /** {@inheritDoc} */
    @Nullable @Override protected VisorWalTaskResult reduce0(List<ComputeJobResult> results) throws TurboSQLException {
        Map<String, Exception> exRes = U.newHashMap(0);
        Map<String, Collection<String>> res = U.newHashMap(results.size());
        Map<String, VisorClusterNode> nodesInfo = U.newHashMap(results.size());

        for (ComputeJobResult result: results){
            ClusterNode node = result.getNode();

            String nodeId = node.consistentId().toString();

            if(result.getException() != null)
                exRes.put(nodeId, result.getException());
            else if (result.getData() != null) {
                Collection<String> data = result.getData();

                if(data != null)
                    res.put(nodeId, data);
            }

            nodesInfo.put(nodeId, new VisorClusterNode(node));
        }

        return new VisorWalTaskResult(res, exRes, nodesInfo);
    }

    /**
     * Performs WAL cleanup per node.
     */
    private static class VisorWalJob extends VisorJob<VisorWalTaskArg, Collection<String>> {
        /** */
        private static final long serialVersionUID = 0L;

        /** Auto injected logger */
        @LoggerResource
        private transient TurboSQLLogger log;

        /**
         *  @param arg WAL task argument.
         *  @param debug Debug flag.
         */
        public VisorWalJob(VisorWalTaskArg arg, boolean debug) {
            super(arg, debug);
        }

        /** {@inheritDoc} */
        @Nullable @Override protected Collection<String> run(@Nullable VisorWalTaskArg arg) throws TurboSQLException {
            try {
                GridKernalContext cctx = turboSQL.context();

                GridCacheDatabaseSharedManager dbMgr = (GridCacheDatabaseSharedManager)cctx.cache().context().database();
                FileWriteAheadLogManager wal = (FileWriteAheadLogManager)cctx.cache().context().wal();

                if (dbMgr == null || arg == null || wal == null)
                    return null;

                switch (arg.getOperation()) {
                    case DELETE_UNUSED_WAL_SEGMENTS:
                        return deleteUnusedWalSegments(dbMgr, wal);

                    case PRINT_UNUSED_WAL_SEGMENTS:
                    default:
                        return getUnusedWalSegments(dbMgr, wal);

                }
            }
            catch (TurboSQLCheckedException e){
                U.error(log, "Failed to perform WAL task", e);

                throw new TurboSQLException("Failed to perform WAL task", e);
            }
        }

        /**
         * Get unused wal segments.
         *
         * @param  wal Database manager.
         * @return {@link Collection<String>} of absolute paths of unused WAL segments.
         * @throws TurboSQLCheckedException if failed.
         */
        Collection<String> getUnusedWalSegments(
            GridCacheDatabaseSharedManager dbMgr,
            FileWriteAheadLogManager wal
        ) throws TurboSQLCheckedException{
            WALPointer lowBoundForTruncate = dbMgr.checkpointHistory().firstCheckpointPointer();

            if (lowBoundForTruncate == null)
                return Collections.emptyList();

            int maxIdx = resolveMaxReservedIndex(wal, lowBoundForTruncate);

            File[] walFiles = getWalArchiveDir().listFiles(WAL_ARCHIVE_FILE_FILTER);

            Collection<String> res = new ArrayList<>(walFiles != null && walFiles.length > 0 ? walFiles.length - 1 : 0);

            if(walFiles != null && walFiles.length > 0) {
                sortWalFiles(walFiles);

                // Obtain index of last archived WAL segment, it will not be deleted.
                long lastArchIdx = getIndex(walFiles[walFiles.length - 1]);

                for (File f : walFiles) {
                    long fileIdx = getIndex(f);

                    if (fileIdx < maxIdx && fileIdx < lastArchIdx)
                        res.add(f.getAbsolutePath());
                    else
                        break;
                }
            }

            return res;
        }

        /**
         * Delete unused wal segments.
         *
         * @param dbMgr Database manager.
         * @return {@link Collection<String>} of deleted WAL segment's files.
         * @throws TurboSQLCheckedException if failed.
         */
        Collection<String> deleteUnusedWalSegments(
            GridCacheDatabaseSharedManager dbMgr,
            FileWriteAheadLogManager wal
        ) throws TurboSQLCheckedException {
            WALPointer lowBoundForTruncate = dbMgr.checkpointHistory().firstCheckpointPointer();

            if (lowBoundForTruncate == null)
                return Collections.emptyList();

            int maxIdx = resolveMaxReservedIndex(wal, lowBoundForTruncate);

            File[] walFiles = getWalArchiveDir().listFiles(WAL_ARCHIVE_FILE_FILTER);

            dbMgr.onWalTruncated(lowBoundForTruncate);

            int num = wal.truncate(null, lowBoundForTruncate);

            if (walFiles != null) {
                sortWalFiles(walFiles);

                Collection<String> res = new ArrayList<>(num);

                for (File walFile: walFiles) {
                    if (getIndex(walFile) < maxIdx && num > 0)
                        res.add(walFile.getAbsolutePath());
                    else
                        break;

                    num--;
                }

                return res;
            }
            else
                return Collections.emptyList();

        }

        /**
         *
         */
        private int resolveMaxReservedIndex(FileWriteAheadLogManager wal, WALPointer lowBoundForTruncate) {
            FileWALPointer low = (FileWALPointer)lowBoundForTruncate;

            int resCnt = wal.reserved(null, lowBoundForTruncate);

            long highIdx = low.index();

            return (int)(highIdx - resCnt + 1);
        }

        /**
         * Get WAL archive directory from configuration.
         *
         * @return WAL archive directory.
         * @throws TurboSQLCheckedException if failed.
         */
        private File getWalArchiveDir() throws TurboSQLCheckedException {
            TurboSQLConfiguration igCfg = turboSQL.context().config();

            DataStorageConfiguration dsCfg = igCfg.getDataStorageConfiguration();

            PdsFolderSettings resFldrs = turboSQL.context().pdsFolderResolver().resolveFolders();

            String consId = resFldrs.folderName();

            File dir;

            if (dsCfg.getWalArchivePath() != null) {
                File workDir0 = new File(dsCfg.getWalArchivePath());

                dir = workDir0.isAbsolute() ?
                        new File(workDir0, consId) :
                        new File(U.resolveWorkDirectory(igCfg.getWorkDirectory(), dsCfg.getWalArchivePath(), false),
                                consId);
            }
            else
                dir = new File(U.resolveWorkDirectory(igCfg.getWorkDirectory(),
                        DataStorageConfiguration.DFLT_WAL_ARCHIVE_PATH, false), consId);

            if (!dir.exists())
                throw new TurboSQLCheckedException("WAL archive directory does not exists" + dir.getAbsolutePath());

            return dir;
        }


        /**
         * Sort WAL files according their indices.
         *
         * @param files Array of WAL segment files.
         */
        private void sortWalFiles(File[] files) {
            Arrays.sort(files, new Comparator<File>() {
                @Override public int compare(File o1, File o2) {
                    return Long.compare(getIndex(o1), getIndex(o2));
                }
            });
        }
    }

    /**
     * Get index from WAL segment file.
     *
     * @param file WAL segment file.
     * @return Index of WAL segment file.
     */
    private static long getIndex(File file) {
        return Long.parseLong(file.getName().substring(0, 16));
    }
}