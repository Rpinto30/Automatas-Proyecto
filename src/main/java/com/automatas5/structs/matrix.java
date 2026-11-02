package com.automatas5.structs;


public class Matrix {
    // ------------------------------------------------------------------
    // Inner classes
    /** Matrix node: stores a char and its 4 neighbors. */
    private static class Node {
        final int x;
        final int y;
        char value;
        Node up, down, left, right;

        Node(int x, int y, char value) {
            this.x = x;
            this.y = y;
            this.value = value;
        }
    }

    /** Header of a row or a column. Points to the first node of that line. */
    private static class Header {
        final int index;
        Header prev, next;
        Node first; 

        Header(int index) {
            this.index = index;
        }
    }

    /** Doubly linked list of headers (sorted by index). */
    private static class HeaderList {
        Header first, last;
        int count = 0;

        void addLast() {
            Header h = new Header(count);
            if (last == null) {
                first = last = h;
            } else {
                last.next = h;
                h.prev = last;
                last = h;
            }
            count++;
        }

        void removeLast() {
            if (last == null) return;
            last = last.prev;
            if (last == null) {
                first = null;
            } else {
                last.next = null;
            }
            count--;
        }

        Header get(int index) {
            if (index < count / 2) {
                Header h = first;
                while (h != null && h.index != index) h = h.next;
                return h;
            } else {
                Header h = last;
                while (h != null && h.index != index) h = h.prev;
                return h;
            }
        }
    }

    // ------------------------------------------------------------------
    // Fields

    private final HeaderList rows = new HeaderList();
    private final HeaderList columns = new HeaderList();
    private int size;

    // ------------------------------------------------------------------
    // Constructor

    /**
     * @param size initial size of the terrain (size x size). Must be > 0.
     */
    public Matrix(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be greater than 0");
        }
        this.size = 0;
        setNewSize(size);
    }

    // ------------------------------------------------------------------
    // Public API

    /**
     * Rescales the matrix to newSize x newSize.
     * - If it grows: new (empty) headers are added.
     * - If it shrinks: the extra headers are removed together with their nodes.
     */
    public void setNewSize(int newSize) {
        if (newSize <= 0) {
            throw new IllegalArgumentException("Size must be greater than 0");
        }

        if (newSize > size) {
            while (rows.count < newSize) rows.addLast();
            while (columns.count < newSize) columns.addLast();
        } else if (newSize < size) {
            while (rows.count > newSize) {
                Header row = rows.last;
                for (Node n = row.first; n != null; n = n.right) {
                    unlinkFromColumn(n);
                }
                rows.removeLast();
            }
            
            while (columns.count > newSize) {
                Header col = columns.last;
                for (Node n = col.first; n != null; n = n.down) {
                    unlinkFromRow(n);
                }
                columns.removeLast();
            }
        }
        this.size = newSize;
    }

    /**
     * Inserts a char at (x, y). If the coordinate is already occupied,
     * it only replaces the value.
     */
    public void insertInCoord(int x, int y, char value) {
        validate(x, y);

        Header rowHeader = rows.get(y);
        Header colHeader = columns.get(x);

        // Find the position in the row (sorted by x)
        Node prevInRow = null;
        Node currInRow = rowHeader.first;
        while (currInRow != null && currInRow.x < x) {
            prevInRow = currInRow;
            currInRow = currInRow.right;
        }
        if (currInRow != null && currInRow.x == x) {
            currInRow.value = value; // already existed: just replace
            return;
        }

        // Find the position in the column (sorted by y)
        Node prevInCol = null;
        Node currInCol = colHeader.first;
        while (currInCol != null && currInCol.y < y) {
            prevInCol = currInCol;
            currInCol = currInCol.down;
        }

        Node node = new Node(x, y, value);

        // Link into the row
        node.left = prevInRow;
        node.right = currInRow;
        if (prevInRow == null) rowHeader.first = node;
        else prevInRow.right = node;
        if (currInRow != null) currInRow.left = node;

        // Link into the column
        node.up = prevInCol;
        node.down = currInCol;
        if (prevInCol == null) colHeader.first = node;
        else prevInCol.down = node;
        if (currInCol != null) currInCol.up = node;
    }

    /**
     * Returns the char at (x, y).
     * If the coordinate is empty, returns '\0' (null character).
     */
    public char getInCoord(int x, int y) {
        validate(x, y);

        Node n = rows.get(y).first;
        while (n != null && n.x < x) n = n.right;

        return (n != null && n.x == x) ? n.value : '\0';
    }

    /** Current size of the terrain (size x size). */
    public int getSize() {
        return size;
    }

    // ------------------------------------------------------------------
    // Private helpers

    private void validate(int x, int y) {
        if (x < 0 || x >= size || y < 0 || y >= size) {
            throw new IndexOutOfBoundsException(
                "Coordinate (" + x + ", " + y + ") out of range [0, " + (size - 1) + "]");
        }
    }

    /** Removes the node from its column list, updating the header if needed. */
    private void unlinkFromColumn(Node n) {
        if (n.up != null) n.up.down = n.down;
        else columns.get(n.x).first = n.down;
        if (n.down != null) n.down.up = n.up;
    }

    /** Removes the node from its row list, updating the header if needed. */
    private void unlinkFromRow(Node n) {
        if (n.left != null) n.left.right = n.right;
        else rows.get(n.y).first = n.right;
        if (n.right != null) n.right.left = n.left;
    }
}