package com.skku.zip.domain.locations.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubPath {
    private int trafficType;
    private Minutes sectionTime;
    private String startName;
    private String endName;
    private String laneName;
}
