package com.example.stackoverflow.service;

import lombok.Getter;

@Getter
public enum JavaError {
    NULL_POINTER_EXCEPTION("NullPointerException", "null", "dereference", "NullReference"),
    ARRAY_INDEX_OUT_OF_BOUNDS("ArrayIndexOutOfBoundsException", "index", "array", "out of bounds"),
    CLASS_CAST_EXCEPTION("ClassCastException", "cast", "type", "incompatible"),
    ARITHMETIC_EXCEPTION("ArithmeticException", "divide by zero", "overflow", "underflow"),
    IO_EXCEPTION("IOException", "I/O", "file", "stream"),
    FILE_NOT_FOUND_EXCEPTION("FileNotFoundException", "not found","file","path"),
    NUMBER_FORMAT_EXCEPTION("NumberFormatException", "parse", "number", "string"),
    OUT_OF_MEMORY_ERROR("OutOfMemoryError", "heap", "memory", "overflow");

    private final String errorName;
    private final String[] keywords;

    JavaError(String errorName, String... keywords) {
        this.errorName = errorName;
        this.keywords = keywords;
    }
}