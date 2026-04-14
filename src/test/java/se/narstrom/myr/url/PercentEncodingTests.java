package se.narstrom.myr.url;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import se.narstrom.myr.web.url.PercentEncoded;

class PercentEncodingTests {

	@Test
	void encodeTest() {
		assertEquals("%23", PercentEncoded.percentEncode((byte) 0x23));
		assertEquals("%7F", PercentEncoded.percentEncode((byte) 0x7F));
	}

	@Test
	void decodeTest() {
		assertEquals("%%s%1G", PercentEncoded.percentDecodeToString("%25%s%1G"));
		assertArrayEquals(new byte[] { (byte) 0xE2, (byte) 0x80, (byte) 0xBD, 0x25, 0x2E }, PercentEncoded.percentDecode("‽%25%2E").toByteArray());
	}
}
