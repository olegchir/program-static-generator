package com.olegchir.jug.site.parser.jbreak2018parser;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.olegchir.jug.site.parser.jbreak2018parser.LangUtil.escapeHtml;

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

    public void htmlEscape() {
        this.name = escapeHtml(this.name);
        for (Speaker speaker : speakers) {
            speaker.htmlEscape();
        }
        for (int i=0; i < description.size(); i++) {
            description.set(i, LangUtil.escapeChevronQuotes(description.get(i)));
        }
    }

    public void replaceWith(Talk talk) {
        if (!StringUtils.isEmpty(talk.name)) {
            this.name = talk.name;
        }

        if (null != talk.speakers) {
            this.speakers = talk.speakers;
        }

        if (!StringUtils.isEmpty(talk.url)) {
            this.url = talk.url;
        }

        if (null != talk.description) {
            this.description = talk.description;
        }
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
