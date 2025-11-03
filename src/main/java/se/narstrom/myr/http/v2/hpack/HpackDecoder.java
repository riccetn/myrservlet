package se.narstrom.myr.http.v2.hpack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import se.narstrom.myr.http.semantics.Field;
import se.narstrom.myr.http.semantics.FieldName;
import se.narstrom.myr.http.semantics.FieldValue;
import se.narstrom.myr.http.semantics.Fields;

public final class HpackDecoder {
	private final List<Field> fields = new ArrayList<>();
	private final DynamicTable dynamicTable = new DynamicTable(StaticTable.size() + 1, 10);
	private int index = 0;
	private byte[] data;

	public void setData(byte[] data) {
		this.data = data;
		this.index = 0;
		this.fields.clear();
	}

	public Fields getFields() {
		return new Fields(fields);
	}


	public Fields decode(final byte[] data) {
		setData(data);
		decode();
		return getFields();
	}

	public void decode() {
		while (index < data.length) {
			if ((data[index] & 0x80) == 0x80) {
				decodeIndexedHeader();
			} else if ((data[index] & 0xC0) == 0x40) {
				decodeLiteralHeaderWithIndexing();
			} else if ((data[index] & 0xF0) == 0x00) {
				decodeLiteralHeaderWithoutIndexing();
			} else if ((data[index] & 0xF0) == 0x10) {
				decodeLiteralHeaderWithoutIndexing();
			}
		}
	}

	private void decodeIndexedHeader() {
		final int tableIndex = decodeInteger7();
		final Field field = getFieldFromTable(tableIndex);
		fields.add(field);
	}

	private void decodeLiteralHeaderWithIndexing() {
		final int tableIndex = decodeInteger6();
		final FieldName name;
		if (tableIndex == 0) {
			name = new FieldName(decodeString());
		} else {
			name = getFieldFromTable(tableIndex).name();
		}
		final FieldValue value = new FieldValue(decodeString());
		final Field field = new Field(name, value);
		dynamicTable.add(field);
		fields.add(field);
	}

	private void decodeLiteralHeaderWithoutIndexing() {
		final int tableIndex = decodeInteger4();
		final FieldName name;
		if (tableIndex == 0) {
			name = new FieldName(decodeString());
		} else {
			name = getFieldFromTable(tableIndex).name();
		}
		final FieldValue value = new FieldValue(decodeString());
		final Field field = new Field(name, value);
		fields.add(field);
	}

	private Field getFieldFromTable(final int tableIndex) {
		if (tableIndex <= StaticTable.size())
			return StaticTable.get(tableIndex);
		else
			return dynamicTable.get(tableIndex);
	}

	private String decodeString() {
		final boolean isHuffmanEncoded = (data[index] & 0x80) == 0x80;
		final int encodingLength = decodeInteger7();
		if (isHuffmanEncoded) {
			return decodeHuffman(encodingLength);
		} else {
			final String value = new String(data, index, encodingLength, StandardCharsets.US_ASCII);
			index += encodingLength;
			return value;
		}
	}

	private int decodeInteger7() {
		final int value = data[index++] & 0x7F;
		if (value == 0x7F) {
			return decodeIntegerLong();
		} else {
			return value;
		}
	}

	private int decodeInteger6() {
		final int value = data[index++] & 0x3F;
		if (value == 0x3F) {
			return decodeIntegerLong();
		} else {
			return value;
		}
	}

	private int decodeInteger4() {
		final int value = data[index++] & 0x0F;
		if (value == 0x0F) {
			return decodeIntegerLong();
		} else {
			return value;
		}
	}

	private int decodeIntegerLong() {
		boolean hasMore = false;
		int value = 0;
		int shift = 0;
		do {
			hasMore = (data[index] & 0x80) == 0x80;
			value |= (data[index] & 0x7F) << shift;
			++index;
			shift += 7;
		} while (hasMore);
		return value;
	}

	private String decodeHuffman(final int encodingLength) {
		throw new UnsupportedOperationException("Not implemented");
	}
}
