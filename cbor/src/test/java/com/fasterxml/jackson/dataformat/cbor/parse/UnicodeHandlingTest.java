package com.fasterxml.jackson.dataformat.cbor.parse;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.cbor.*;
import com.fasterxml.jackson.dataformat.cbor.testutil.ThrottledInputStream;

public class UnicodeHandlingTest extends CBORTestBase
{
    private final CBORFactory F = new CBORFactory();

    public void testShortUnicodeWithSurrogates() throws IOException
    {
        _testLongUnicodeWithSurrogates(28, false);
        _testLongUnicodeWithSurrogates(28, true);
        _testLongUnicodeWithSurrogates(53, false);
        _testLongUnicodeWithSurrogates(230, false);
    }

    public void testLongUnicodeWithSurrogates() throws IOException
    {
        _testLongUnicodeWithSurrogates(900, false);
        _testLongUnicodeWithSurrogates(9600, false);
        _testLongUnicodeWithSurrogates(9600, true);
    }

    private void _testLongUnicodeWithSurrogates(int length,
        boolean throttling) throws IOException
    {
        final String SURROGATE_CHARS = "\ud834\udd1e";
        StringBuilder sb = new StringBuilder(length+200);
        while (sb.length() < length) {
            sb.append(SURROGATE_CHARS);
            sb.append(sb.length());
            if ((sb.length() & 1) == 1) {
                sb.append("\u00A3");
            } else {
                sb.append("\u3800");
            }
        }
        final String TEXT = sb.toString();
        final String quoted = quote(TEXT);
        byte[] data = cborDoc(F, quoted);

        CBORParser p = _parser(data, throttling);
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(TEXT, p.getText());
        assertNull(p.nextToken());
        p.close();

        // Then same but skipping
        p = _parser(data, throttling);
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertNull(p.nextToken());
        p.close();

        // Also, verify that it works as field name
        data = cborDoc(F, "{"+quoted+":true}");

        p = _parser(data, throttling);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(TEXT, p.getCurrentName());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());
        p.close();

        // and skipping
        p = _parser(data, throttling);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

    @SuppressWarnings("resource")
    private CBORParser _parser(byte[] data, boolean throttling) throws IOException
    {
        if (throttling) {
            return F.createParser(new ThrottledInputStream(data, 3));
        }
        return F.createParser(data);
    }
}
