package com.unitbv.rawserver;

public interface Handler {
    Response handle(Request request);
}
