package net.simonvt.trakt.api.entity;

public class Person {

    private String name;

    private String job;

    private String character;

    private Images images;

    private Boolean executive;

    public String getName() {
        return name;
    }

    public String getJob() {
        return job;
    }

    public String getCharacter() {
        return character;
    }

    public Images getImages() {
        return images;
    }

    public Boolean isExecutive() {
        return executive;
    }
}
