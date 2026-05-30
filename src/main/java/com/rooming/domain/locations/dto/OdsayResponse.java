package com.rooming.domain.locations.dto;

import lombok.Data;

import java.util.List;

public class OdsayResponse {

    private Result result;

    @Data
    public static class Result {
        private List<Path> path;
    }

    @Data
    public static class Path {
        private Info info;
        private List<SubPath> subPath;
    }

    @Data
    public static class Info {
        private int totalTime;
        private int transferCount;
    }

    @Data
    public static class SubPath {
        private int trafficType;
        private int sectionTime;
        private String startName;
        private String endName;
        private List<Lane> lane;
    }

    @Data
    public static class Lane {
        private String busNo;
        private String name;
    }
}