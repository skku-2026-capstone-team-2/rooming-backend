package com.skku.zip.domain.locations.entity.value;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Path {
    private Minutes totalTime;
    private int transferCount;
    private List<SubPath> subPaths;
}
