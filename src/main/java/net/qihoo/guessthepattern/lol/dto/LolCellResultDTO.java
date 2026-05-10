package net.qihoo.guessthepattern.lol.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LolCellResultDTO {
    /** exact | partial | none | higher | lower | near | unknown */
    private String kind;
    private String color;
    private String arrow;
    private List<String> matched;
}
