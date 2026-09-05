package model;

public class HexEdge implements java.io.Serializable {
    private final int col1, row1;
    private final int col2, row2;

    public HexEdge(int col1, int row1, int col2, int row2) {
        if (col1 < col2 || (col1 == col2 && row1 <= row2)) {
            this.col1 = col1;
            this.row1 = row1;
            this.col2 = col2;
            this.row2 = row2;
        } else {
            this.col1 = col2;
            this.row1 = row2;
            this.col2 = col1;
            this.row2 = row1;
        }
    }

    public int getCol1() {
        return col1;
    }

    public int getRow1() {
        return row1;
    }

    public int getCol2() {
        return col2;
    }

    public int getRow2() {
        return row2;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof HexEdge)) return false;

        HexEdge that = (HexEdge) other;
        return col1 == that.col1 && row1 == that.row1 && col2 == that.col2 && row2 == that.row2;
    }

    @Override
    public int hashCode() {
        int result = col1;
        result = 31 * result + row1;
        result = 31 * result + col2;
        result = 31 * result + row2;
        return result;
    }
}
