package com.olegchir.jug.site.parser.jbreak2018parser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Talk {
    private String id;
    private String name;
    private List<Speaker> speakers = new ArrayList<>();
    private String url;
    private List<String> description;
    private List<String> conferences;
    private int talkDay = -1;
    private String timeString;
    private Date time;

    public void replaceWith(Talk talk) {
        this.name = talk.name;
        this.speakers = talk.speakers;
        this.url = talk.url;
        this.description = talk.description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Speaker> getSpeakers() {
        return speakers;
    }

    public void setSpeakers(List<Speaker> speakers) {
        this.speakers = speakers;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getConferences() {
        return conferences;
    }

    public void setConferences(List<String> conferences) {
        this.conferences = conferences;
    }

    public int getTalkDay() {
        return talkDay;
    }

    public void setTalkDay(int talkDay) {
        this.talkDay = talkDay;
    }

    public String getTimeString() {
        return timeString;
    }

    public void setTimeString(String timeString) {
        this.timeString = timeString;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }
}
