/*
 * Copyright (C) 2018-2020  LightChaosman
 *
 * BLCMM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package blcmm.utilities.hex;

import blcmm.utilities.GlobalLogger;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author LightChaosman
 */
public class HexEditor {

    public static final class HexResult {

        public final HexResultStatus result;
        public final File backupFile;

        public HexResult(HexResultStatus result, File backupFile) {
            this.result = result;
            this.backupFile = backupFile;
        }

        @Override
        public String toString() {
            return result.toString();
        }

    }

    public static enum HexResultStatus {
        HEXEDIT_SUCCESFUL,//
        ERROR_UNKNOWN,//
        HEXEDIT_ALREADY_DONE,
        ERROR_MULTIPLE_MATCHES,
        ERROR_UNKNOWN_BYTE_PATTERN_FOUND,
        MULTIPLE_MATCHES
    }

    public static HexInspectResult[] inspectFile(File inputFile, HexDictionary.HexQuery query) {
        return inspectFile(inputFile, HexDictionary.getHexEdits(query));
    }

    public static HexInspectResult[] inspectFile(File inputFile, HexEdit... hexEdits) {
        ArrayList<HexInspectResult> result = new ArrayList<>();
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(inputFile));) {
            scanFile(bis, result, hexEdits);
            bis.close();
        } catch (IOException ex) {
            GlobalLogger.log("IO exception during hex inspection:");
            GlobalLogger.log(ex);
            return new HexInspectResult[0];
        }
        return result.toArray(new HexInspectResult[0]);
    }

    /**
     * Performs the specified hex edits on the given file, replacing it, and
     * creating a backup in the process.
     *
     * @param fileToHexEdit
     * @param query
     * @return
     * @throws java.io.IOException
     */
    public static HexResult performHexEdits(File fileToHexEdit, HexDictionary.HexQuery query) throws IOException {
        return performHexEdits(fileToHexEdit, false, query);
    }

    public static HexResult performHexEdits(File fileToHexEdit, boolean force, HexDictionary.HexQuery query) throws IOException {
        return performHexEdits(fileToHexEdit, fileToHexEdit, force, query);
    }

    /**
     * Performs the specified hex edits on the given file, replacing it, and
     * creating a backup in the process.
     *
     * @param fileToHexEdit
     * @param replacements
     * @return
     * @throws java.io.IOException
     */
    public static HexResult performHexEdits(File fileToHexEdit, HexEdit... replacements) throws IOException {
        return performHexEdits(fileToHexEdit, fileToHexEdit, false, replacements);
    }

    public static HexResult performHexEdits(File fileToHexEdit, boolean force, HexEdit... replacements) throws IOException {
        return performHexEdits(fileToHexEdit, fileToHexEdit, force, replacements);
    }

    public static HexResult performHexEdits(File fileToHexEdit, File outputFile, HexDictionary.HexQuery query) throws IOException {
        return performHexEdits(fileToHexEdit, outputFile, HexDictionary.getHexEdits(query));
    }

    public static HexResult performHexEdits(File fileToHexEdit, File outputFile, boolean force, HexDictionary.HexQuery query) throws IOException {
        return performHexEdits(fileToHexEdit, outputFile, force, HexDictionary.getHexEdits(query));
    }

    /**
     * Performs the specified hex edits on the given file, and outputs the
     * result to outputFile. If outputFile is the same as inputFile, a backup of
     * the original will be made, in case such a backup did not already exist.
     *
     * @param fileToHexEdit
     * @param outputFile
     * @param replacements
     * @return
     * @throws java.io.IOException
     */
    public static HexResult performHexEdits(File fileToHexEdit, File outputFile, HexEdit... replacements) throws IOException {
        return performHexEditSingleScan(fileToHexEdit, new File(fileToHexEdit.getAbsolutePath() + ".bk"), outputFile, false, replacements);
    }

    public static HexResult performHexEdits(File fileToHexEdit, File outputFile, boolean force, HexEdit... replacements) throws IOException {
        return performHexEditSingleScan(fileToHexEdit, new File(fileToHexEdit.getAbsolutePath() + ".bk"), outputFile, force, replacements);
    }

    /**
     * Will perform the specified hexedits on the input file. If the input file
     * and output file are the same, the input file will be overwritten. In that
     * case, if backupOfOriginalFile did not yet exists, a backup of the
     * unedited file will be stored there.
     *
     * @param inputFile
     * @param backupOfOriginalFile
     * @param outputFile
     * @param force
     * @param hexEdits
     * @return
     */
    private static HexResult performHexEditSingleScan(File inputFile, File backupOfOriginalFile, File outputFile, boolean force, HexEdit... hexEdits) throws IOException {
        File temp = new File(inputFile.getAbsolutePath() + ".temp");

        if (temp.exists()) {
            temp.delete();
        }
        temp.createNewFile();
        temp.setExecutable(inputFile.canExecute(), false);

        Result result = new Result();
        try (//
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(inputFile));
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(temp));) {
            scanFile(bis, bos, result, force, hexEdits);
            bos.close();
            bis.close();
        } catch (IOException ex) {
            GlobalLogger.log("IO exception during hex edit:");
            GlobalLogger.log(ex);
            throw ex;
        }
        return finalizeEdit(result, inputFile, outputFile, backupOfOriginalFile, temp, hexEdits);
    }

    private static void scanFile(final BufferedInputStream bis, List<HexInspectResult> result, HexEdit... hexEdits) throws IOException {
        scanFile(bis, null, result, false, hexEdits);
    }

    private static void scanFile(final BufferedInputStream bis, final BufferedOutputStream bos, Object result, boolean force, HexEdit... hexEdits) throws IOException {
        //We use bos==null to check if we are actually editing or just inspecting.
        //A bit ugly perhaps, but since those are the only two needed operations, it'll do
        //Since we most likely will be doing an inspect prior to the edit, this could be optimized,
        //by having basically a duplicate of this method for just AdressEdits, and not have a negative buffer in there.
        //But that'll likely shave off miliseconds at best, at the cost of non-trivial code duplication
        //
        int buffersize = 0;//how many bytes at the end of our current chunk do we need to be able to scan forward?
        int negativeBufferSize = 0;//how many bytes at the start of our current chunk do we need to be able to scan backward?
        for (HexEdit search : hexEdits) {
            buffersize = Math.max(buffersize, search.requiredBufferSize());
            negativeBufferSize = Math.max(negativeBufferSize, search.requiredNegativeBufferSize());
        }
        int byteBufferSize = 1024 * 128;
        while (negativeBufferSize + buffersize > byteBufferSize / 2 - 1) {
            byteBufferSize *= 2;
        }
        HashMap<HexEdit, Collection<HexResultStatus>> resultmap = new HashMap<>();
        for (HexEdit edit : hexEdits) {
            resultmap.put(edit, new ArrayList<>());
        }

        byte[] bytebuffer = new byte[byteBufferSize];
        int numberOfBytesCopiedFromLastIteration = 0;
        int globalOffset = 0;//Keeps track of our global offset trough the iterations
        int read;//how many bytes we read in this iteration
        while ((read = bis.read(bytebuffer, numberOfBytesCopiedFromLastIteration, byteBufferSize - numberOfBytesCopiedFromLastIteration)) != -1) {
            final int effectiveArraySize = numberOfBytesCopiedFromLastIteration + read;
            final int startIndex;
            final int endIndex;
            if (globalOffset == 0) {
                //The initial iteration
                startIndex = 0;//Risk an exception in the first iteration if negative offsets exist.
                endIndex = effectiveArraySize - buffersize;
            } else if (read == byteBufferSize - numberOfBytesCopiedFromLastIteration) {
                //A full read, intermediate iteration
                startIndex = negativeBufferSize;
                endIndex = effectiveArraySize - buffersize;
            } else {
                //The last iteration. Continue until the end. Risk an exception to not report a false negative.
                startIndex = negativeBufferSize;
                endIndex = effectiveArraySize;
            }

            for (HexEdit edit : hexEdits) {
                if (bos == null) {//no proper abstraction since only two use cases.
                    ((List) result).addAll(getInspectResults(bytebuffer, edit, globalOffset, startIndex, endIndex));
                } else {
                    HexResultStatus resultOfSearch = searchAndReplace(bytebuffer, edit, globalOffset, startIndex, endIndex, force);
                    updateResult(resultOfSearch, (Result) result, edit, resultmap);
                }
            }
            //This is set here, so it applies to all but the first iteration
            numberOfBytesCopiedFromLastIteration = buffersize + negativeBufferSize;
            //Flush the scanned and edited bytes of this iteration
            if (bos != null) {
                bos.write(bytebuffer, 0, effectiveArraySize - numberOfBytesCopiedFromLastIteration);
            }
            for (int i = 0; i < numberOfBytesCopiedFromLastIteration; i++) {
                bytebuffer[i] = bytebuffer[effectiveArraySize - numberOfBytesCopiedFromLastIteration + i];
                //Since bytebuffer is at least twice the size of numberOfBytesCopiedFromLastIteration, this won't go wrong
            }
            globalOffset += (endIndex - startIndex);
        }
        //Since we did not write the tail of the last iteration, and it was copied to the start at the end of said iteration, flush the start of our buffer
        if (bos != null) {
            bos.write(bytebuffer, 0, numberOfBytesCopiedFromLastIteration);
        }
    }

    private static void updateResult(HexResultStatus resultOfSearch, Result result, HexEdit edit, Map<HexEdit, Collection<HexResultStatus>> resultmap) {
        if (resultOfSearch == null) {
            return;
        }
        switch (resultOfSearch) {
            case MULTIPLE_MATCHES:
                result.multipleMatches++;
                break;
            case HEXEDIT_SUCCESFUL:
                result.succesfulEdits++;
                break;
            case HEXEDIT_ALREADY_DONE:
                result.alreadyReplaceds++;
                break;
            case ERROR_UNKNOWN_BYTE_PATTERN_FOUND:
                result.unknownPattern++;
                break;
            default:
                result.error++;
        }
        resultmap.get(edit).add(resultOfSearch);
        if (resultmap.get(edit).size() > 1) {
            result.multipleMatches++;
        }
    }

    /**
     * Returns the result of the hex edit and handles the filename juggling
     * based on the result.
     *
     * @param result The object containing the metadata of the result
     * @param resultingFile The edited instance of the file
     * @param replacements The performed edits
     * @param backupOfOriginalFile The file that will contain the backup of the
     * original unedited file
     * @param inputFile The file that was edited
     * @param outputFile The place the output should go to
     * @return
     */
    private static HexResult finalizeEdit(Result result, File inputFile, File outputFile, File backupOfOriginalFile, File resultingFile, HexEdit[] replacements) throws IOException {
        if (result.error > 0) {
            resultingFile.delete();
            return new HexResult(HexResultStatus.ERROR_UNKNOWN, backupOfOriginalFile);
        } else if (result.multipleMatches > 0) {
            resultingFile.delete();
            return new HexResult(HexResultStatus.ERROR_MULTIPLE_MATCHES, backupOfOriginalFile);
        } else if (result.unknownPattern > 0) {
            resultingFile.delete();
            return new HexResult(HexResultStatus.ERROR_UNKNOWN_BYTE_PATTERN_FOUND, backupOfOriginalFile);
        }
        if (result.alreadyReplaceds == replacements.length) {
            resultingFile.delete();
            return new HexResult(HexResultStatus.HEXEDIT_ALREADY_DONE, backupOfOriginalFile);
        } else if (result.succesfulEdits + result.alreadyReplaceds == replacements.length) {
            if (!backupOfOriginalFile.exists()) {
                if (inputFile.equals(outputFile)) {
                    Files.move(inputFile.toPath(), backupOfOriginalFile.toPath());
                } else {
                    //No need to make a backup
                }
            }
            Files.move(resultingFile.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            resultingFile.delete();
            return new HexResult(HexResultStatus.HEXEDIT_SUCCESFUL, backupOfOriginalFile);
        }
        return new HexResult(HexResultStatus.ERROR_UNKNOWN, backupOfOriginalFile);
    }

    /**
     *
     * @param bytes
     * @param search
     * @param GlobalOffset We have already searched trough this many bytes in
     * previous calls to this method.
     * @param indexToStartSearching We start searching again from this index
     * @param indexToStopSearching We stop searching beyond this index
     * @param force
     * @return
     */
    private static HexResultStatus searchAndReplace(byte[] bytes, HexEdit search, int GlobalOffset, int indexToStartSearching, int indexToStopSearching, boolean force) {
        List<HexResultStatus> results = new ArrayList<>();
        outer:
        for (int idx = indexToStartSearching; idx < indexToStopSearching; idx++) {
            if (search.match(bytes, idx, GlobalOffset)) {
                HexResultStatus res = search.replace(bytes, idx, GlobalOffset, force);
                results.add(res);
            }
        }
        switch (results.size()) {
            case 0:
                return null;
            case 1:
                return results.get(0);
            default:
                return HexResultStatus.MULTIPLE_MATCHES;
        }
    }

    /**
     *
     * @param bytes
     * @param search
     * @param GlobalOffset We have already searched trough this many bytes in
     * previous calls to this method.
     * @param indexToStartSearching We start searching again from this index
     * @param indexToStopSearching We stop searching beyond this index
     * @return
     */
    private static List<HexInspectResult> getInspectResults(byte[] bytes, HexEdit search, int GlobalOffset, int indexToStartSearching, int indexToStopSearching) {
        List<HexInspectResult> results = new ArrayList<>();
        outer:
        for (int idx = indexToStartSearching; idx < indexToStopSearching; idx++) {
            if (search.match(bytes, idx, GlobalOffset)) {
                results.add(search.inspect(bytes, idx, GlobalOffset));
            }
        }
        return results;
    }

    private static class Result {

        int succesfulEdits = 0;
        int alreadyReplaceds = 0;
        int unknownPattern = 0;
        int multipleMatches = 0;
        int error = 0;
    }

}
