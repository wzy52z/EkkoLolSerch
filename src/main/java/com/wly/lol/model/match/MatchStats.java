package com.wly.lol.model.match;
import lombok.Data;

@Data
public class MatchStats {
    private int kills;
    private int deaths;
    private int assists;
    private boolean win;
}