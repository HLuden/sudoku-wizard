package ru.ludens.sudoku;

class listOfVariantsEntry {

    private Data data;
    private int marker; // 0 - not analyzed yet; 1 - analyzed

    listOfVariantsEntry(Data data) {
        this.data = data;
        this.marker = 0;
    }

    Data getData() {
        return data;
    }

    int getMarker() {
        return marker;
    }

    void setMarker() {
        marker = 1;
    }

}
