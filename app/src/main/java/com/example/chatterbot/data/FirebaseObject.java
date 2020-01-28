package com.example.chatterbot.data;

public class FirebaseObject {

    String message;
    String messageTranslation;
    boolean outcoming;
    String date;

    public FirebaseObject() {
    }

    public FirebaseObject(String message, String messageTransation, boolean outcoming, String date) {
        this.message = message;
        this.messageTranslation = messageTransation;
        this.outcoming = outcoming;
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageTranslation() {
        return messageTranslation;
    }

    public void setMessageTranslation(String messageTranslation) {
        this.messageTranslation = messageTranslation;
    }

    public boolean isOutcoming() {
        return outcoming;
    }

    public void setOutcoming(boolean outcoming) {
        this.outcoming = outcoming;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "FirebaseObject{" +
                "message='" + message + '\'' +
                ", messageTranslation='" + messageTranslation + '\'' +
                ", outcoming=" + outcoming +
                ", date='" + date + '\'' +
                '}';
    }
}
