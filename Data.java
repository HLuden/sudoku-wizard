package ru.ludens.sudoku;

import java.util.Arrays;

class Data {

    private int[][] content = new int[9][9];
    private int[][][] candidates = new int[9][9][9];
    private int status = 0; // -1 - incorrect, 0 - undefined, 1 - solved (correct)
    private int emptyCells = 81;

    Data() {
        for (int j = 0; j < 9; j++) {
            for (int i = 0; i < 9; i++) {
                candidates[i][j] = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
            }
        }
    }

    @SuppressWarnings("CopyConstructorMissesField")
    Data(Data data) {
        for (int j = 0; j < 9; j++) {
            for (int i = 0; i < 9; i++) {
                this.content[i][j] = data.content[i][j];
                this.candidates[i][j] = Arrays.copyOf(data.candidates[i][j], data.candidates[i][j].length);
            }
        }
        this.status = data.status;
        this.emptyCells = data.emptyCells;
    }

    int get(int x, int y) {
        return content[x][y];
    }

    int getStatus() {
        return status;
    }

    int[] getCandidates(int x, int y) {
        int[] candidatesSet = new int[10];
        int counter = 1;
        for (int i = 0; i < 9; i++) {
            if (candidates[x][y][i] == i + 1) {
                candidatesSet[counter] = i + 1;
                counter++;
            }
        }
        candidatesSet = Arrays.copyOf(candidatesSet, counter);
        return candidatesSet;
    }

    void set(Data data) {
        for (int j = 0; j < 9; j++) {
            for (int i = 0; i < 9; i++) {
                this.content[i][j] = data.content[i][j];
                this.candidates[i][j] = Arrays.copyOf(data.candidates[i][j], data.candidates[i][j].length);
            }
        }
        this.status = data.status;
        this.emptyCells = data.emptyCells;
    }

    void set(int x, int y, int val) {
        if (val > 0 && candidates[x][y][val - 1] != val) {
            throw new IllegalArgumentException("Illegal argument of method \"Data.set(...)\"");
        }
        if (content[x][y] == val) {
            System.out.println("Warning! Set the same value " + val + " by the method \"Data::set\"");
            return;
        }
        int prev = content[x][y];
        content[x][y] = val;
        update(x, y, val, prev);
        if (prev == 0) {
            emptyCells--;
        } else if (val == 0) {
            if (status == 1) {
                status = 0;
            }
            emptyCells++;
        }
        if (emptyCells == 0) {
            status = 1;
            return;
        }
        // Check for correctness
        outerLoop:
        for (int j = 0; j < 9; j++) {
            for (int i = 0; i < 9; i++) {
                if (content[i][j] == 0) {
                    if (getCandidates(i, j).length == 1) {
                        status = -1;
                        break outerLoop;
                    }
                }
            }
        }
    }

    void clear() {
        status = 0;
        emptyCells = 81;
        for (int j = 0; j < 9; j++) {
            for (int i = 0; i < 9; i++) {
                content[i][j] = 0;
                for (int k = 0; k < 9; k++) {
                    candidates[i][j][k] = k + 1;
                }
            }
        }
    }

    private void update(int x, int y, int val, int prev) {
        final int left = (x / 3 * 3) + (x + 1) % 3;
        final int right = (x / 3 * 3) + (x + 2) % 3;
        final int top = (y / 3 * 3) + (y + 1) % 3;
        final int lower = (y / 3 * 3) + (y + 2) % 3;
        if (val > 0) {
            candidates[x][y][val - 1] = 0;
            //Exclude illegal candidates in the raw and the column
            for (int k = 0; k < 9; k++) {
                if (k != x) {
                    candidates[k][y][val - 1] = 0;
                }
                if (k != y) {
                    candidates[x][k][val - 1] = 0;
                }
            }
            //Exclude illegal candidates in the 3x3 flat
            candidates[left][top][val - 1] = 0;
            candidates[left][lower][val - 1] = 0;
            candidates[right][top][val - 1] = 0;
            candidates[right][lower][val - 1] = 0;
        }
        //Refund legal candidates
        if (prev > 0) {
            int[][] mask = new int[9][9];
            for (int j = 0; j < 9; j++) {
                for (int i = 0; i < 9; i++) {
                    if (content[i][j] == prev) {
                        mask[x][j] = 1;
                        mask[i][y] = 1;
                        if (i == left || i == right) {
                            mask[i][top] = 1;
                            mask[i][lower] = 1;
                            mask[x][j / 3 * 3 + (j + 1) % 3] = 1;
                            mask[x][j / 3 * 3 + (j + 2) % 3] = 1;
                        }
                        if (j == top || j == lower) {
                            mask[left][j] = 1;
                            mask[right][j] = 1;
                            mask[i / 3 * 3 + (i + 1) % 3][y] = 1;
                            mask[i / 3 * 3 + (i + 2) % 3][y] = 1;
                        }
                    }
                }
            }
            for (int k = 0; k < 9; k++) {
                if (mask[x][k] != 1) {
                    candidates[x][k][prev - 1] = prev;
                }
                if (mask[k][y] != 1) {
                    candidates[k][y][prev - 1] = prev;
                }
            }
            if (mask[left][top] != 1) {
                candidates[left][top][prev - 1] = prev;
            }
            if (mask[left][lower] != 1) {
                candidates[left][lower][prev - 1] = prev;
            }
            if (mask[right][top] != 1) {
                candidates[right][top][prev - 1] = prev;
            }
            if (mask[right][lower] != 1) {
                candidates[right][lower][prev - 1] = prev;
            }
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(content);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Data)) {
            return false;
        }
        Data other = (Data) obj;
        return Arrays.deepEquals(this.content, other.content);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (int j = 0; j < 9; j++) {
            for (int i = 0; i < 9; i++) {
                s.append(content[i][j]);
                if (i == 8) {
                    s.append("\n");
                    if (j % 3 == 2) {
                        s.append("\n");
                    }
                } else if (i % 3 == 2) {
                    s.append("  ");
                }
            }
        }
        return s.toString();
    }

}
