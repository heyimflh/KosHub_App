package com.koshub.psdku.models;

import java.util.List;

public class AiFaqItem {
    private String id;
    private String title;
    private String role; // "general", "student", "owner"
    private List<String> keywords;
    private String answer;

    public AiFaqItem(String id, String title, String role, List<String> keywords, String answer) {
        this.id = id;
        this.title = title;
        this.role = role;
        this.keywords = keywords;
        this.answer = answer;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getRole() {
        return role;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public String getAnswer() {
        return answer;
    }
}
