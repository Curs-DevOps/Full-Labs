package com.unitbv.rawserver;

public class Response {
    private String body;
    private int status;
    private String contentType;

    public Response(int status, String contentType, String body) {
        this.body = body;
        this.status = status;
        this.contentType = contentType;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }


    @Override
    public String toString() {
        return "Response{" +
                "body='" + body + '\'' +
                ", status=" + status +
                ", contentType='" + contentType + '\'' +
                '}';
    }

}
