/*
 * JAVE - A Java Audio/Video Encoder (based on FFMPEG)
 * 
 * Copyright (C) 2008-2009 Carlo Pelliccia (www.sauronsoftware.it)
 * 
 * This program is free software: you can redistribute it and/or modify
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.sauronsoftware.jave;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main class of the package. Instances can encode audio and video streams.
 *
 * @author Carlo Pelliccia
 */
public class Encoder {

    /**
     * This regexp is used to parse the ffmpeg output about the supported
     * formats.
     */
    private static final Pattern FORMAT_PATTERN = Pattern
            .compile("^\\s*([D ])([E ])\\s+([\\w,]+)\\s+.+$");
    /**
     * This regexp is used to parse the ffmpeg output about the included
     * encoders/decoders.
     */
    private static final Pattern ENCODER_DECODER_PATTERN = Pattern.compile(
            "^\\s*([D ])([E ])([AVS]).{3}\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    /**
     * This regexp is used to parse the ffmpeg output about the ongoing encoding
     * process.
     */
    private static final Pattern PROGRESS_INFO_PATTERN = Pattern.compile(
            "\\s*(\\w+)\\s*=\\s*(\\S+)\\s*", Pattern.CASE_INSENSITIVE);
    /**
     * This regexp is used to parse the ffmpeg output about the size of a video
     * stream.
     */
    private static final Pattern SIZE_PATTERN = Pattern.compile(
            "(\\d+)x(\\d+)", Pattern.CASE_INSENSITIVE);
    /**
     * This regexp is used to parse the ffmpeg output about the frame rate value
     * of a video stream.
     */
    private static final Pattern FRAME_RATE_PATTERN = Pattern.compile(
            "([\\d.]+)\\s+(?:fps|tbr)", Pattern.CASE_INSENSITIVE);
    /**
     * This regexp is used to parse the ffmpeg output about the bit rate value
     * of a stream.
     */
    private static final Pattern BIT_RATE_PATTERN = Pattern.compile(
            "(\\d+)\\s+kb/s", Pattern.CASE_INSENSITIVE);
    /**
     * This regexp is used to parse the ffmpeg output about the sampling rate of
     * an audio stream.
     */
    private static final Pattern SAMPLING_RATE_PATTERN = Pattern.compile(
            "(\\d+)\\s+Hz", Pattern.CASE_INSENSITIVE);
    /**
     * This regexp is used to parse the ffmpeg output about the channels number
     * of an audio stream.
     */
    private static final Pattern CHANNELS_PATTERN = Pattern.compile(
            "(mono|stereo)", Pattern.CASE_INSENSITIVE);
    /**
     * This regexp is used to parse the ffmpeg output about the success of an
     * encoding operation.
     */
    private static final Pattern SUCCESS_PATTERN = Pattern.compile(
            "^\\s*video\\:\\S+\\s+audio\\:\\S+\\s+subtitle\\:\\S+\\s+global headers\\:\\S+.*$",
            Pattern.CASE_INSENSITIVE);
    /**
     * The locator of the ffmpeg executable used by this encoder.
     */
    private final FFMPEGLocator locator;

    /**
     * It builds an encoder using a {@link DefaultFFMPEGLocator} instance to
     * locate the ffmpeg executable to use.
     */
    public Encoder() {
        this.locator = new DefaultFFMPEGLocator();
    }

    /**
     * It builds an encoder with a custom {@link FFMPEGLocator}.
     *
     * @param locator The locator picking up the ffmpeg executable used by the
     * encoder.
     */
    public Encoder(FFMPEGLocator locator) {
        this.locator = locator;
    }

    /**
     * Returns a list with the names of all the audio decoders bundled with the
     * ffmpeg distribution in use. An audio stream can be decoded only if a
     * decoder for its format is available.
     *
     * @return A list with the names of all the included audio decoders.
     * @throws EncoderException If a problem occurs calling the underlying
     * ffmpeg executable.
     */
    public String[] getAudioDecoders() throws EncoderException {
        ArrayList<String> res = new ArrayList<String>();
        FFMPEGExecutor ffmpeg = locator.createExecutor();
        ffmpeg.addArgument("-formats");
        try {
            ffmpeg.execute();
            RBufferedReader reader = null;
            reader = new RBufferedReader(new InputStreamReader(ffmpeg
                    .getInputStream()));
            String line;
            boolean evaluate = false;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                if (evaluate) {
                    Matcher matcher = ENCODER_DECODER_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        String decoderFlag = matcher.group(1);
                        String audioVideoFlag = matcher.group(3);
                        if ("D".equals(decoderFlag)
                                && "A".equals(audioVideoFlag)) {
                            String name = matcher.group(4);
                            res.add(name);
                        }
                    } else {
                        break;
                    }
                } else if (line.trim().equals("Codecs:")) {
                    evaluate = true;
                }
            }
        } catch (IOException e) {
            throw new EncoderException(e);
        } finally {
            ffmpeg.destroy();
        }
        int size = res.size();
        String[] ret = new String[size];
        for (int i = 0; i < size; i++) {
            ret[i] = res.get(i);
        }
        return ret;
    }

    /**
     * Returns a list with the names of all the audio encoders bundled with the
     * ffmpeg distribution in use. An audio stream can be encoded using one of
     * these encoders.
     *
     * @return A list with the names of all the included audio encoders.
     * @throws EncoderException If a problem occurs calling the underlying
     * ffmpeg executable.
     */
    public String[] getAudioEncoders() throws EncoderException {
        ArrayList<String> res = new ArrayList<String>();
        FFMPEGExecutor ffmpeg = locator.createExecutor();
        ffmpeg.addArgument("-formats");
        try {
            ffmpeg.execute();
            RBufferedReader reader = null;
            reader = new RBufferedReader(new InputStreamReader(ffmpeg
                    .getInputStream()));
            String line;
            boolean evaluate = false;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                if (evaluate) {
                    Matcher matcher = ENCODER_DECODER_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        String encoderFlag = matcher.group(2);
                        String audioVideoFlag = matcher.group(3);
                        if ("E".equals(encoderFlag)
                                && "A".equals(audioVideoFlag)) {
                            String name = matcher.group(4);
                            res.add(name);
                        }
                    } else {
                        break;
                    }
                } else if (line.trim().equals("Codecs:")) {
                    evaluate = true;
                }
            }
        } catch (IOException e) {
            throw new EncoderException(e);
        } finally {
            ffmpeg.destroy();
        }
        int size = res.size();
        String[] ret = new String[size];
        for (int i = 0; i < size; i++) {
            ret[i] = res.get(i);
        }
        return ret;
    }

    /**
     * Returns a list with the names of all the video decoders bundled with the
     * ffmpeg distribution in use. A video stream can be decoded only if a
     * decoder for its format is available.
     *
     * @return A list with the names of all the included video decoders.
     * @throws EncoderException If a problem occurs calling the underlying
     * ffmpeg executable.
     */
    public String[] getVideoDecoders() throws EncoderException {
        ArrayList<String> res = new ArrayList<String>();
        FFMPEGExecutor ffmpeg = locator.createExecutor();
        ffmpeg.addArgument("-formats");
        try {
            ffmpeg.execute();
            RBufferedReader reader = null;
            reader = new RBufferedReader(new InputStreamReader(ffmpeg
                    .getInputStream()));
            String line;
            boolean evaluate = false;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                if (evaluate) {
                    Matcher matcher = ENCODER_DECODER_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        String decoderFlag = matcher.group(1);
                        String audioVideoFlag = matcher.group(3);
                        if ("D".equals(decoderFlag)
                                && "V".equals(audioVideoFlag)) {
                            String name = matcher.group(4);
                            res.add(name);
                        }
                    } else {
                        break;
                    }
                } else if (line.trim().equals("Codecs:")) {
                    evaluate = true;
                }
            }
        } catch (IOException e) {
            throw new EncoderException(e);
        } finally {
            ffmpeg.destroy();
        }
        int size = res.size();
        String[] ret = new String[size];
        for (int i = 0; i < size; i++) {
            ret[i] = res.get(i);
        }
        return ret;
    }

    /**
     * Returns a list with the names of all the video encoders bundled with the
     * ffmpeg distribution in use. A video stream can be encoded using one of
     * these encoders.
     *
     * @return A list with the names of all the included video encoders.
     * @throws EncoderException If a problem occurs calling the underlying
     * ffmpeg executable.
     */
    public String[] getVideoEncoders() throws EncoderException {
        ArrayList<String> res = new ArrayList<String>();
        FFMPEGExecutor ffmpeg = locator.createExecutor();
        ffmpeg.addArgument("-formats");
        try {
            ffmpeg.execute();
            RBufferedReader reader = null;
            reader = new RBufferedReader(new InputStreamReader(ffmpeg
                    .getInputStream()));
            String line;
            boolean evaluate = false;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                if (evaluate) {
                    Matcher matcher = ENCODER_DECODER_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        String encoderFlag = matcher.group(2);
                        String audioVideoFlag = matcher.group(3);
                        if ("E".equals(encoderFlag)
                                && "V".equals(audioVideoFlag)) {
                            String name = matcher.group(4);
                            res.add(name);
                        }
                    } else {
                        break;
                    }
                } else if (line.trim().equals("Codecs:")) {
                    evaluate = true;
                }
            }
        } catch (IOException e) {
            throw new EncoderException(e);
        } finally {
            ffmpeg.destroy();
        }
        int size = res.size();
        String[] ret = new String[size];
        for (int i = 0; i < size; i++) {
            ret[i] = res.get(i);
        }
        return ret;
    }

    /**
     * Returns a list with the names of all the file formats supported at
     * encoding time by the underlying ffmpeg distribution. A multimedia file
     * could be encoded and generated only if the specified format is in this
     * list.
     *
     * @return A list with the names of all the supported file formats at
     * encoding time.
     * @throws EncoderException If a problem occurs calling the underlying
     * ffmpeg executable.
     */
    public String[] getSupportedEncodingFormats() throws EncoderException {
        ArrayList<String> res = new ArrayList<String>();
        FFMPEGExecutor ffmpeg = locator.createExecutor();
        ffmpeg.addArgument("-formats");
        try {
            ffmpeg.execute();
            RBufferedReader reader = null;
            reader = new RBufferedReader(new InputStreamReader(ffmpeg
                    .getInputStream()));
            String line;
            boolean evaluate = false;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                if (evaluate) {
                    Matcher matcher = FORMAT_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        String encoderFlag = matcher.group(2);
                        if ("E".equals(encoderFlag)) {
                            String aux = matcher.group(3);
                            StringTokenizer st = new StringTokenizer(aux, ",");
                            while (st.hasMoreTokens()) {
                                String token = st.nextToken().trim();
                                if (!res.contains(token)) {
                                    res.add(token);
                                }
                            }
                        }
                    } else {
                        break;
                    }
                } else if (line.trim().equals("File formats:")) {
                    evaluate = true;
                }
            }
        } catch (IOException e) {
            throw new EncoderException(e);
        } finally {
            ffmpeg.destroy();
        }
        int size = res.size();
        String[] ret = new String[size];
        for (int i = 0; i < size; i++) {
            ret[i] = res.get(i);
        }
        return ret;
    }

    /**
     * Returns a list with the names of all the file formats supported at
     * decoding time by the underlying ffmpeg distribution. A multimedia file
     * could be open and decoded only if its format is in this list.
     *
     * @return A list with the names of all the supported file formats at
     * decoding time.
     * @throws EncoderException If a problem occurs calling the underlying
     * ffmpeg executable.
     */
    public String[] getSupportedDecodingFormats() throws EncoderException {
        ArrayList<String> res = new ArrayList<String>();
        FFMPEGExecutor ffmpeg = locator.createExecutor();
        ffmpeg.addArgument("-formats");
        try {
            ffmpeg.execute();
            RBufferedReader reader = null;
            reader = new RBufferedReader(new InputStreamReader(ffmpeg
                    .getInputStream()));
            String line;
            boolean evaluate = false;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                if (evaluate) {
                    Matcher matcher = FORMAT_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        String decoderFlag = matcher.group(1);
                        if ("D".equals(decoderFlag)) {
                            String aux = matcher.group(3);
                            StringTokenizer st = new StringTokenizer(aux, ",");
                            while (st.hasMoreTokens()) {
                                String token = st.nextToken().trim();
                                if (!res.contains(token)) {
                                    res.add(token);
                                }
                            }
                        }
                    } else {
                        break;
                    }
                } else if (line.trim().equals("File formats:")) {
                    evaluate = true;
                }
            }
        } catch (IOException e) {
            throw new EncoderException(e);
        } finally {
            ffmpeg.destroy();
        }
        int size = res.size();
        String[] ret = new String[size];
        for (int i = 0; i < size; i++) {
            ret[i] = res.get(i);
        }
        return ret;
    }

    /**
     * Private utility. Parse a line and try to match its contents against the
     * {@link Encoder#PROGRESS_INFO_PATTERN} pattern. It the line can be parsed,
     * it returns a hashtable with progress informations, otherwise it returns
     * null.
     *
     * @param line The line from the ffmpeg output.
     * @return A hashtable with the value reported in the line, or null if the
     * given line can not be parsed.
     */
    private HashMap<String, String> parseProgressInfoLine(String line) {
        HashMap<String, String> table = null;
        Matcher m = PROGRESS_INFO_PATTERN.matcher(line);
        while (m.find()) {
            if (table == null) {
                table = new HashMap<String, String>();
            }
            String key = m.group(1);
            String value = m.group(2);
            table.put(key, value);
        }
        return table;
    }

    /**
     * Re-encode a multimedia file.
     *
     * @param multimediaObject The source multimedia file. It cannot be null. Be sure this
     * file can be decoded (see      {@link Encoder#getSupportedDecodingFormats()},
	 *            {@link Encoder#getAudioDecoders()} and
     * {@link Encoder#getVideoDecoders()}).
     * @param target The target multimedia re-encoded file. It cannot be null.
     * If this file already exists, it will be overwrited.
     * @param attributes A set of attributes for the encoding process.
     * @throws IllegalArgumentException If both audio and video parameters are
     * null.
     * @throws InputFormatException If the source multimedia file cannot be
     * decoded.
     * @throws EncoderException If a problems occurs during the encoding
     * process.
     */
    public void encode(MultimediaObject multimediaObject, File target, EncodingAttributes attributes)
            throws IllegalArgumentException, InputFormatException,
            EncoderException {
        encode(multimediaObject, target, attributes, null);
    }

    /**
     * Re-encode a multimedia file.
     *
     * @param multimediaObject The source multimedia file. It cannot be null. Be sure this
     * file can be decoded (see      {@link Encoder#getSupportedDecodingFormats()},
	 *            {@link Encoder#getAudioDecoders()} and
     * {@link Encoder#getVideoDecoders()}).
     * @param target The target multimedia re-encoded file. It cannot be null.
     * If this file already exists, it will be overwrited.
     * @param attributes A set of attributes for the encoding process.
     * @param listener An optional progress listener for the encoding process.
     * It can be null.
     * @throws IllegalArgumentException If both audio and video parameters are
     * null.
     * @throws InputFormatException If the source multimedia file cannot be
     * decoded.
     * @throws EncoderException If a problems occurs during the encoding
     * process.
     */
    public void encode(MultimediaObject multimediaObject, File target, EncodingAttributes attributes,
            EncoderProgressListener listener) throws IllegalArgumentException,
            InputFormatException, EncoderException {
        String formatAttribute = attributes.getFormat();
        Float offsetAttribute = attributes.getOffset();
        Float durationAttribute = attributes.getDuration();
        AudioAttributes audioAttributes = attributes.getAudioAttributes();
        VideoAttributes videoAttributes = attributes.getVideoAttributes();
        if (audioAttributes == null && videoAttributes == null) {
            throw new IllegalArgumentException(
                    "Both audio and video attributes are null");
        }
        target = target.getAbsoluteFile();
        target.getParentFile().mkdirs();
        FFMPEGExecutor ffmpeg = locator.createExecutor();
        if (offsetAttribute != null) {
            ffmpeg.addArgument("-ss");
            ffmpeg.addArgument(String.valueOf(offsetAttribute.floatValue()));
        }
        ffmpeg.addArgument("-i");
        ffmpeg.addArgument(multimediaObject.getFile().getAbsolutePath());
        if (durationAttribute != null) {
            ffmpeg.addArgument("-t");
            ffmpeg.addArgument(String.valueOf(durationAttribute.floatValue()));
        }
        if (videoAttributes == null) {
            ffmpeg.addArgument("-vn");
        } else {
            String codec = videoAttributes.getCodec();
            if (codec != null) {
                ffmpeg.addArgument("-vcodec");
                ffmpeg.addArgument(codec);
            }
            String tag = videoAttributes.getTag();
            if (tag != null) {
                ffmpeg.addArgument("-vtag");
                ffmpeg.addArgument(tag);
            }
            Integer bitRate = videoAttributes.getBitRate();
            if (bitRate != null) {
                ffmpeg.addArgument("-vb");
                ffmpeg.addArgument(String.valueOf(bitRate.intValue()));
            }
            Integer frameRate = videoAttributes.getFrameRate();
            if (frameRate != null) {
                ffmpeg.addArgument("-r");
                ffmpeg.addArgument(String.valueOf(frameRate.intValue()));
            }
            VideoSize size = videoAttributes.getSize();
            if (size != null) {
                ffmpeg.addArgument("-s");
                ffmpeg.addArgument(String.valueOf(size.getWidth()) + "x"
                        + String.valueOf(size.getHeight()));
            }

            if (videoAttributes.isFaststart())
            {
                ffmpeg.addArgument("-movflags");
                ffmpeg.addArgument("faststart");
            }

            if (videoAttributes.getX264Profile() != null)
            {
                ffmpeg.addArgument("-profile:v");
                ffmpeg.addArgument(videoAttributes.getX264Profile().getModeName());
            }

            if (videoAttributes.getVideoFilters().size() > 0)
            {
                for (VideoFilter videoFilter : videoAttributes.getVideoFilters())
                {
                    ffmpeg.addArgument("-vf");
                    ffmpeg.addArgument(videoFilter.getExpression());
                }
            }
        }
        if (audioAttributes == null) {
            ffmpeg.addArgument("-an");
        } else {
            String codec = audioAttributes.getCodec();
            if (codec != null) {
                if (codec.equals("aac")) {
                    codec = "libvo_aacenc";
                }
                ffmpeg.addArgument("-acodec");
                ffmpeg.addArgument(codec);
            }
            Integer bitRate = audioAttributes.getBitRate();
            if (bitRate != null) {
                ffmpeg.addArgument("-ab");
                ffmpeg.addArgument(String.valueOf(bitRate.intValue()));
            }
            Integer channels = audioAttributes.getChannels();
            if (channels != null) {
                ffmpeg.addArgument("-ac");
                ffmpeg.addArgument(String.valueOf(channels.intValue()));
            }
            Integer samplingRate = audioAttributes.getSamplingRate();
            if (samplingRate != null) {
                ffmpeg.addArgument("-ar");
                ffmpeg.addArgument(String.valueOf(samplingRate.intValue()));
            }
            Integer volume = audioAttributes.getVolume();
            if (volume != null) {
                ffmpeg.addArgument("-vol");
                ffmpeg.addArgument(String.valueOf(volume.intValue()));
            }
        }
        if(formatAttribute != null) {
            ffmpeg.addArgument("-f");
            ffmpeg.addArgument(formatAttribute);
        }
        ffmpeg.addArgument("-y");
        ffmpeg.addArgument(target.getAbsolutePath());
        try {
            ffmpeg.execute();
        } catch (IOException e) {
            throw new EncoderException(e);
        }
        try {
            String lastWarning = null;
            long duration;
            long progress = 0;
            RBufferedReader reader = new RBufferedReader(
                    new InputStreamReader(ffmpeg.getErrorStream()));
            MultimediaInfo info = multimediaObject.getInfo();
            if (durationAttribute != null) {
                duration = (long) Math
                        .round((durationAttribute * 1000L));
            } else {
                duration = info.getDuration();
                if (offsetAttribute != null) {
                    duration -= (long) Math
                            .round((offsetAttribute * 1000L));
                }
            }
            if (listener != null) {
                listener.sourceInfo(info);
            }
            int step = 0;
            int lineNR = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNR++;
                if (step == 0) {
                    if (line.startsWith("WARNING: ")) {
                        if (listener != null) {
                            listener.message(line);
                        }
                    } else if (!line.startsWith("Output #0")) {
                        // throw new EncoderException(line);
                    } else {
                        step++;
                    }
                }
                if (step == 1) {
                    if (line.startsWith("WARNING: ")) {
                        if (listener != null) {
                            listener.message(line);
                        }
                    } else if (!line.startsWith("Output #0")) {
                        // throw new EncoderException(line);
                    } else {
                        step++;
                    }
                } else if (step == 2) {
                    if (!line.startsWith("  ")) {
                        step++;
                    }
                }
                if (step == 3) {
                    if (!line.startsWith("Stream mapping:")) {
                        throw new EncoderException(line);
                    } else {
                        step++;
                    }
                } else if (step == 4) {
                    if (!line.startsWith("  ")) {
                        step++;
                    }
                }
                if (line.startsWith("frame="))
                {
                    try
                    {
                        line = line.trim();
                        if (line.length() > 0) {
                            HashMap<String, String> table = parseProgressInfoLine(line);
                            if (table == null) {
                                if (listener != null) {
                                    listener.message(line);
                                }
                                lastWarning = line;
                            } else {
                                if (listener != null) {
                                    String time = table.get("time");
                                    if (time != null) {
                                        String dParts[] = time.split(":");
                                        // HH:MM:SS.xx

                                        Double seconds = Double.parseDouble(dParts[dParts.length-1]);
                                        if(dParts.length > 1)
                                        {
                                            seconds += Double.parseDouble(dParts[dParts.length-2]) * 60;
                                            if(dParts.length > 2)
                                            {
                                                seconds += Double.parseDouble(dParts[dParts.length-3]) * 60 * 60;
                                            }
                                        }

                                        int perm = (int) Math.round((seconds * 1000L * 1000L)
                                                / (double) duration);
                                        if (perm > 1000) {
                                            perm = 1000;
                                        }
                                        listener.progress(perm);
                                    }
                                }
                                lastWarning = null;
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                    }
                }
            }
            if (lastWarning != null) {
                if (!SUCCESS_PATTERN.matcher(lastWarning).matches()) {
                    throw new EncoderException("No match for: " + SUCCESS_PATTERN + " in " + lastWarning);
                }
            }
        } catch (IOException e) {
            throw new EncoderException(e);
        } finally {
            ffmpeg.destroy();
        }
    }
}
