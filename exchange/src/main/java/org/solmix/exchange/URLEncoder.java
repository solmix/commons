package org.solmix.exchange;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.util.BitSet;

/**
 * Encodes unsafe characters as a sequence of %XX hex-encoded bytes.
 *
 * This is typically done when encoding components of URLs. See {@link URLEncoders} for pre-configured
 * URLEncoder instances.
 */
public class URLEncoder {

    private static final char[] HEX_CODE = "0123456789ABCDEF".toCharArray();

    private final BitSet safeChars;

    private final CharsetEncoder encoder;

    private final StringBuilderPercentEncoderOutputHandler stringHandler;

    private final ByteBuffer encodedBytes;

    private final CharBuffer unsafeCharsToEncode;

    /**
     * @param safeChars      the set of chars to NOT encode, stored as a bitset with the int positions corresponding to
     *                       those chars set to true. Treated as read only.
     * @param charsetEncoder charset encoder to encode characters with. Make sure to not re-use CharsetEncoder instances
     *                       across threads.
     */
    URLEncoder(BitSet safeChars, CharsetEncoder charsetEncoder) {
        this.safeChars = safeChars;
        this.encoder = charsetEncoder;
        this.stringHandler = new StringBuilderPercentEncoderOutputHandler();
        int maxBytesPerChar = 1 + (int) encoder.maxBytesPerChar();
        encodedBytes = ByteBuffer.allocate(maxBytesPerChar * 2);
        unsafeCharsToEncode = CharBuffer.allocate(2);
    }

    /**
     * Encode the input and pass output chars to a handler.
     *
     * @param input   input string
     * @param handler handler to call on each output character
     * @throws MalformedInputException      if encoder is configured to report errors and malformed input is detected
     * @throws UnmappableCharacterException if encoder is configured to report errors and an unmappable character is
     *                                      detected
     */
    private void encode(CharSequence input, StringBuilderPercentEncoderOutputHandler handler)
            throws MalformedInputException, UnmappableCharacterException {
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            int cp = Character.codePointAt(String.valueOf(c), 0);
            if (safeChars.get(cp)) {
                handler.onOutputChar(c);
                continue;
            }
            unsafeCharsToEncode.clear();
            unsafeCharsToEncode.append(c);
            if (Character.isHighSurrogate(c)) {
                if (input.length() > i + 1) {
                    char lowSurrogate = input.charAt(i + 1);
                    if (Character.isLowSurrogate(lowSurrogate)) {
                        unsafeCharsToEncode.append(lowSurrogate);
                        i++;
                    } else {
                        throw new IllegalArgumentException("invalid UTF-16: character "
                                + i + " is a high surrogate (\\u"
                                + Integer.toHexString(cp) + "), but char " + (i + 1)
                                + " is not a low surrogate (\\u"
                                + Integer.toHexString(Character.codePointAt(String.valueOf(lowSurrogate), 0)) + ")");
                    }
                } else {
                    throw new IllegalArgumentException("invalid UTF-16: the last character in the input string "
                            + "was a high surrogate (\\u" + Integer.toHexString(cp) + ")");
                }
            }
            flushUnsafeCharBuffer(handler);
        }
    }

    /**
     * Encode the input and return the resulting text as a String.
     *
     * @param input input string
     * @return the input string with every character that's not in safeChars turned into its byte representation via the
     * instance's encoder and then percent-encoded
     * @throws MalformedInputException      if encoder is configured to report errors and malformed input is detected
     * @throws UnmappableCharacterException if encoder is configured to report errors and an unmappable character is
     *                                      detected
     */
    public String encode(CharSequence input) throws MalformedInputException, UnmappableCharacterException {
        if (input == null) {
            return null;
        }
        stringHandler.reset();
        stringHandler.ensureCapacity(input.length());
        encode(input, stringHandler);
        return stringHandler.getContents();
    }

    /**
     * Encode unsafeCharsToEncode to bytes as per charsetEncoder, then percent-encode those bytes into output.
     *
     * Side effects: unsafeCharsToEncode will be read from and cleared. encodedBytes will be cleared and written to.
     *
     * @param handler where the encoded versions of the contents of unsafeCharsToEncode will be written
     */
    private void flushUnsafeCharBuffer(StringBuilderPercentEncoderOutputHandler handler)
            throws MalformedInputException, UnmappableCharacterException {
        // need to read from the char buffer, which was most recently written to
        unsafeCharsToEncode.flip();
        encodedBytes.clear();
        encoder.reset();
        CoderResult result = encoder.encode(unsafeCharsToEncode, encodedBytes, true);
        throwIfError(result);
        result = encoder.flush(encodedBytes);
        throwIfError(result);
        encodedBytes.flip();
        while (encodedBytes.hasRemaining()) {
            byte b = encodedBytes.get();
            handler.onOutputChar('%');
            handler.onOutputChar(HEX_CODE[b >> 4 & 0xF]);
            handler.onOutputChar(HEX_CODE[b & 0xF]);
        }
    }

    /**
     * @param result result to check
     * @throws IllegalStateException        if result is overflow
     * @throws MalformedInputException      if result represents malformed input
     * @throws UnmappableCharacterException if result represents an unmappable character
     */
    private static void throwIfError(CoderResult result) throws MalformedInputException, UnmappableCharacterException {
        if (result.isOverflow()) {
            throw new IllegalStateException("Byte buffer overflow, this should not happen");
        }
        if (result.isMalformed()) {
            throw new MalformedInputException(result.length());
        }
        if (result.isUnmappable()) {
            throw new UnmappableCharacterException(result.length());
        }
    }

    static class StringBuilderPercentEncoderOutputHandler {

        private final StringBuilder stringBuilder;

        StringBuilderPercentEncoderOutputHandler() {
            stringBuilder = new StringBuilder();
        }

        String getContents() {
            return stringBuilder.toString();
        }

        void reset() {
            stringBuilder.setLength(0);
        }

        void ensureCapacity(int length) {
            stringBuilder.ensureCapacity(length);
        }

        void onOutputChar(char c) {
            stringBuilder.append(c);
        }
    }
}
