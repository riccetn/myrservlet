package se.narstrom.myr.http.v2.hpack;

import se.narstrom.myr.http.semantics.Field;

public final class DynamicTable {
	private final Field[] table;

	private int startIndex;

	private int base;

	private int size;

	public DynamicTable(final int startIndex, final int maxSize) {
		this.startIndex = startIndex;
		this.base = maxSize;
		this.size = 0;
		this.table = new Field[maxSize];
	}

	public Field get(final int index) {
		if(index < startIndex || index >= (startIndex + size))
			throw new IndexOutOfBoundsException(index);
		return table[(base + index - startIndex) % table.length];
	}

	public void add(final Field field) {
		if(base == 0) {
			base = table.length-1;
		} else {
			base -= 1;
		}
		table[base] = field;
		if(size != table.length)
			size += 1;
	}
}
