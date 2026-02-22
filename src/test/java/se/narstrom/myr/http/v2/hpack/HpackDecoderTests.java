package se.narstrom.myr.http.v2.hpack;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import se.narstrom.myr.http.semantics.Fields;

public class HpackDecoderTests {
	// C.2.1. Literal Header Field with Indexing
	@Test
	public void testLiteralFieldWithIndexing() {
		final byte[] data = { 0x40, 0x0a, 0x63, 0x75, 0x73, 0x74, 0x6f, 0x6d, 0x2d, 0x6b, 0x65, 0x79, 0x0d, 0x63, 0x75, 0x73, 0x74, 0x6f, 0x6d, 0x2d, 0x68, 0x65, 0x61, 0x64, 0x65, 0x72 };
		final HpackDecoder decoder = new HpackDecoder();
		decoder.decode(data);
		final Fields fields = decoder.getFields();

		assertEquals(1, fields.getFieldMap().size());
		assertEquals("custom-header", fields.getField("custom-key"));
	}

	// C.2.2. Literal Header Field without Indexing
	@Test
	public void testLiteralFieldWithoutIndexing() {
		final byte[] data = { 0x04, 0x0c, 0x2f, 0x73, 0x61, 0x6d, 0x70, 0x6c, 0x65, 0x2f, 0x70, 0x61, 0x74, 0x68 };
		final HpackDecoder decoder = new HpackDecoder();
		decoder.decode(data);
		final Fields fields = decoder.getFields();

		assertEquals(1, fields.getFieldMap().size());
		assertEquals("/sample/path", fields.getField(":path"));
	}

	// C.2.3. Literal Header Field Never Indexed
	@Test
	public void testLiteralFieldNeverIndexed() {
		final byte[] data = { 0x10, 0x08, 0x70, 0x61, 0x73, 0x73, 0x77, 0x6f, 0x72, 0x64, 0x06, 0x73, 0x65, 0x63, 0x72, 0x65, 0x74 };
		final HpackDecoder decoder = new HpackDecoder();
		decoder.decode(data);
		final Fields fields = decoder.getFields();

		assertEquals(1, fields.getFieldMap().size());
		assertEquals("secret", fields.getField("password"));
	}

	// C.2.4. Indexed Header Field
	@Test
	public void testIndexedField() {
		final byte[] data = { (byte) 0x82 };
		final HpackDecoder decoder = new HpackDecoder();
		decoder.decode(data);
		final Fields fields = decoder.getFields();

		assertEquals(1, fields.getFieldMap().size());
		assertEquals("GET", fields.getField(":method"));
	}

	// C.3. Request Examples without Huffman Coding
	@Test
	public void testRequests() {
		final HpackDecoder decoder = new HpackDecoder();

		final byte[] data1 = { (byte) 0x82, (byte) 0x86, (byte) 0x84, 0x41, 0x0f, 0x77, 0x77, 0x77, 0x2e, 0x65, 0x78, 0x61, 0x6d, 0x70, 0x6c, 0x65, 0x2e, 0x63, 0x6f, 0x6d };
		final Fields fields1 = decoder.decode(data1);

		assertEquals(4, fields1.getFieldMap().size());
		assertEquals("GET", fields1.getField(":method"));
		assertEquals("http", fields1.getField(":scheme"));
		assertEquals("/", fields1.getField(":path"));
		assertEquals("www.example.com", fields1.getField(":authority"));

		final byte[] data2 = { (byte) 0x82, (byte) 0x86, (byte) 0x84, (byte) 0xbe, 0x58, 0x08, 0x6e, 0x6f, 0x2d, 0x63, 0x61, 0x63, 0x68, 0x65 };
		final Fields fields2 = decoder.decode(data2);

		assertEquals(5, fields2.getFieldMap().size());
		assertEquals("GET", fields2.getField(":method"));
		assertEquals("http", fields2.getField(":scheme"));
		assertEquals("/", fields2.getField(":path"));
		assertEquals("www.example.com", fields2.getField(":authority"));
		assertEquals("no-cache", fields2.getField("cache-control"));

		final byte[] data3 = { (byte) 0x82, (byte) 0x87, (byte) 0x85, (byte) 0xbf, 0x40, 0x0a, 0x63, 0x75, 0x73, 0x74, 0x6f, 0x6d, 0x2d, 0x6b, 0x65, 0x79, 0x0c, 0x63, 0x75, 0x73, 0x74, 0x6f, 0x6d,
				0x2d, 0x76, 0x61, 0x6c, 0x75, 0x65 };
		final Fields fields3 = decoder.decode(data3);

		assertEquals(5, fields3.getFieldMap().size());
		assertEquals("GET", fields3.getField(":method"));
		assertEquals("https", fields3.getField(":scheme"));
		assertEquals("/index.html", fields3.getField(":path"));
		assertEquals("www.example.com", fields3.getField(":authority"));
		assertEquals("custom-value", fields3.getField("custom-key"));
	}

	// C.5. Response Examples without Huffman Coding
	@Test
	public void testResponses() {
		final HpackDecoder decoder = new HpackDecoder();

		final byte[] data1 = { 0x48, 0x03, 0x33, 0x30, 0x32, 0x58, 0x07, 0x70, 0x72, 0x69, 0x76, 0x61, 0x74, 0x65, 0x61, 0x1d, 0x4d, 0x6f, 0x6e, 0x2c, 0x20, 0x32, 0x31, 0x20, 0x4f, 0x63, 0x74, 0x20,
				0x32, 0x30, 0x31, 0x33, 0x20, 0x32, 0x30, 0x3a, 0x31, 0x33, 0x3a, 0x32, 0x31, 0x20, 0x47, 0x4d, 0x54, 0x6e, 0x17, 0x68, 0x74, 0x74, 0x70, 0x73, 0x3a, 0x2f, 0x2f, 0x77, 0x77, 0x77,
				0x2e, 0x65, 0x78, 0x61, 0x6d, 0x70, 0x6c, 0x65, 0x2e, 0x63, 0x6f, 0x6d };
		final Fields fields1 = decoder.decode(data1);

		assertEquals(4, fields1.getFieldMap().size());
		assertEquals("302", fields1.getField(":status"));
		assertEquals("private", fields1.getField("cache-control"));
		assertEquals("Mon, 21 Oct 2013 20:13:21 GMT", fields1.getField("date"));
		assertEquals("https://www.example.com", fields1.getField("location"));

		final byte[] data2 = { 0x48, 0x03, 0x33, 0x30, 0x37, (byte) 0xc1, (byte) 0xc0, (byte) 0xbf };
		final Fields fields2 = decoder.decode(data2);

		assertEquals(4, fields2.getFieldMap().size());
		assertEquals("307", fields2.getField(":status"));
		assertEquals("private", fields2.getField("cache-control"));
		assertEquals("Mon, 21 Oct 2013 20:13:21 GMT", fields2.getField("date"));
		assertEquals("https://www.example.com", fields2.getField("location"));

		final byte[] data3 = { (byte) 0x88, (byte) 0xc1, 0x61, 0x1d, 0x4d, 0x6f, 0x6e, 0x2c, 0x20, 0x32, 0x31, 0x20, 0x4f, 0x63, 0x74, 0x20, 0x32, 0x30, 0x31, 0x33, 0x20, 0x32, 0x30, 0x3a, 0x31, 0x33,
				0x3a, 0x32, 0x32, 0x20, 0x47, 0x4d, 0x54, (byte) 0xc0, 0x5a, 0x04, 0x67, 0x7a, 0x69, 0x70, 0x77, 0x38, 0x66, 0x6f, 0x6f, 0x3d, 0x41, 0x53, 0x44, 0x4a, 0x4b, 0x48, 0x51, 0x4b, 0x42,
				0x5a, 0x58, 0x4f, 0x51, 0x57, 0x45, 0x4f, 0x50, 0x49, 0x55, 0x41, 0x58, 0x51, 0x57, 0x45, 0x4f, 0x49, 0x55, 0x3b, 0x20, 0x6d, 0x61, 0x78, 0x2d, 0x61, 0x67, 0x65, 0x3d, 0x33, 0x36,
				0x30, 0x30, 0x3b, 0x20, 0x76, 0x65, 0x72, 0x73, 0x69, 0x6f, 0x6e, 0x3d, 0x31 };
		final Fields fields3 = decoder.decode(data3);

		assertEquals(6, fields3.getFieldMap().size());
		assertEquals("200", fields3.getField(":status"));
		assertEquals("private", fields3.getField("cache-control"));
		assertEquals("Mon, 21 Oct 2013 20:13:22 GMT", fields3.getField("date"));
		assertEquals("https://www.example.com", fields3.getField("location"));
		assertEquals("gzip", fields3.getField("content-encoding"));
		assertEquals("foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1", fields3.getField("set-cookie"));
	}
}
