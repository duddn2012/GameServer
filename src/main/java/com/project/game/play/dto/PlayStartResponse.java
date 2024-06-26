package com.project.game.play.dto;

import com.project.game.character.dto.CharacterSkillGetResponse;
import com.project.game.common.domain.Stat;
import com.project.game.match.vo.MatchStatus;
import com.project.game.match.vo.PlayerType;
import java.util.List;
import lombok.Getter;

@Getter
public class PlayStartResponse {

    MatchStatus matchStatus;
    Stat hostTotalStat;
    Stat entrantTotalStat;
    List<CharacterSkillGetResponse> hostSkillList;
    List<CharacterSkillGetResponse> entrantSkillList;
    PlayerType turnOwner;
    String message;

    public PlayStartResponse(MatchStatus matchStatus, Stat hostTotalStat, Stat entrantTotalStat,
        List<CharacterSkillGetResponse> hostSkillList,
        List<CharacterSkillGetResponse> entrantSkillList, PlayerType turnOwner) {
        this.matchStatus = matchStatus;
        this.hostTotalStat = hostTotalStat;
        this.entrantTotalStat = entrantTotalStat;
        this.hostSkillList = hostSkillList;
        this.entrantSkillList = entrantSkillList;
        this.turnOwner = turnOwner;
        this.message = "게임이 시작됩니다.\n" + turnOwner.toString() + " 턴!";
    }
}
