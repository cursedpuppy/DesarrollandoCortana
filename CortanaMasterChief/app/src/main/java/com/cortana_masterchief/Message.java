package com.cortana_masterchief;


public class Message {
    private String id;
    private String modelo;
    private String usuario;
    private long timestamp;

    public Message() {
        // Constructor vacío requerido por Firebase
    }

    public Message(String id, String modelo, String usuario, long timestamp) {
        this.id = id;
        this.modelo = modelo;
        this.usuario = usuario;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
