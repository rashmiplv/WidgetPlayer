package com.android.widgetplayer.model;

//Audio file which stores information regarding the MP3 audio accessed in the storage
public class Audio {
    private String filepath;
    private String name;
    private String album;
    private String artist;

    public String getPath() {
        return filepath;
    }

    public void setPath(String path) {
        this.filepath = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }
}
