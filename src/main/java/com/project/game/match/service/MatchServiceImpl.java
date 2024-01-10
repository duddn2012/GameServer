package com.project.game.match.service;

import static com.project.game.match.domain.MatchRoom.makeStakedGold;
import static com.project.game.match.vo.MatchStatus.FINISHED;
import static com.project.game.match.vo.MatchStatus.IN_PROGRESS;
import static com.project.game.match.vo.MatchStatus.READY;
import static com.project.game.match.vo.MatchStatus.WAITING;
import static com.project.game.match.vo.PlayerType.ENTRANT;
import static com.project.game.match.vo.PlayerType.HOST;
import static com.project.game.match.vo.PlayerType.togglePlayerType;

import com.project.game.character.domain.Character;
import com.project.game.character.dto.CharacterSkillGetResponse;
import com.project.game.character.exception.CharacterNotFoundException;
import com.project.game.character.exception.CharacterSkillNotFoundException;
import com.project.game.character.repository.CharacterRepository;
import com.project.game.character.repository.CharacterSkillRepository;
import com.project.game.character.service.CharacterService;
import com.project.game.common.domain.Stat;
import com.project.game.match.domain.MatchHistory;
import com.project.game.match.domain.MatchRoom;
import com.project.game.match.dto.MatchRoomEnterRequest;
import com.project.game.match.dto.MatchRoomEnterResponse;
import com.project.game.match.dto.MatchRoomGetResponse;
import com.project.game.match.dto.MatchRoomUpsertResponse;
import com.project.game.match.exception.LevelDifferenceInvalidException;
import com.project.game.match.exception.MatchRoomFullException;
import com.project.game.match.exception.MatchRoomNotFoundException;
import com.project.game.match.exception.MatchRoomTurnInvalidException;
import com.project.game.match.exception.PlayerTypeInvalidException;
import com.project.game.match.exception.PlayerTypeNotHostException;
import com.project.game.match.repository.MatchHistoryRepository;
import com.project.game.match.repository.MatchRoomRepository;
import com.project.game.match.vo.PlayerType;
import com.project.game.play.dto.PlayEndResponse;
import com.project.game.play.dto.PlayGreetingResponse;
import com.project.game.play.dto.PlayReadyRequest;
import com.project.game.play.dto.PlayReadyResponse;
import com.project.game.play.dto.PlayStartResponse;
import com.project.game.play.dto.PlayTurnRequest;
import com.project.game.play.dto.PlayTurnResponse;
import com.project.game.play.exception.MatchStatusInvalidException;
import com.project.game.skill.domain.Skill;
import com.project.game.skill.domain.SkillEffect;
import com.project.game.skill.dto.SkillNotFoundException;
import com.project.game.skill.repository.SkillEffectRepository;
import com.project.game.skill.repository.SkillRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService{

    private final MatchRoomRepository matchRoomRepository;
    private final CharacterRepository characterRepository;
    private final CharacterSkillRepository characterSkillRepository;
    private final CharacterService characterService;
    private final SkillRepository skillRepository;
    private final SkillEffectRepository skillEffectRepository;
    private final MatchHistoryRepository matchHistoryRepository;

    @Override
    public Slice<MatchRoomGetResponse> findMatchRoomList(Pageable pageable) {
        Slice<MatchRoom> matchRooms = matchRoomRepository.findAllByPaging(pageable);
        List<MatchRoomGetResponse> responseList = matchRooms.stream().map(matchRoom -> new MatchRoomGetResponse(matchRoom)).collect(
            Collectors.toList());

        return new SliceImpl<>(responseList, pageable, matchRooms.hasNext());
    }

    @Override
    public MatchRoomUpsertResponse saveMatchRoom(Long characterId) {
        Character host = characterRepository.findById(characterId)
            .orElseThrow(() -> new CharacterNotFoundException(characterId));
        MatchRoom matchRoom = matchRoomRepository.save(MatchRoom.builder().host(host).matchStatus(WAITING).stakedGold(makeStakedGold(
            host.getLevelId())).build());

        return new MatchRoomUpsertResponse(matchRoom);
    }

    @Override
    @Transactional
    public MatchRoomEnterResponse matchRoomEnter(Long characterId,
        MatchRoomEnterRequest matchRoomEnterRequest) {
        Character entrant = characterRepository.findById(characterId)
            .orElseThrow(() -> new CharacterNotFoundException(characterId));
        MatchRoom matchRoom = matchRoomRepository.findById(matchRoomEnterRequest.getMatchRoomId()).orElseThrow(()-> new MatchRoomNotFoundException(
            matchRoomEnterRequest.getMatchRoomId()));

        //입장 가능 여부 확인
        //조건 1. 방에 entrant 자리는 비어 있어야함.
        if(matchRoom.getEntrant() != null){
            throw new MatchRoomFullException(matchRoom.getMatchRoomId());
        }

        //조건 2. 레벨 차이가 2 이하이여야 함.
        Integer levelDifference = matchRoom.getHost().getLevelId() - entrant.getLevelId();
        if(Math.abs(levelDifference) > 2){
            throw new LevelDifferenceInvalidException(levelDifference);
        }

        //입장 처리
        matchRoom.setEntrant(entrant);

        return new MatchRoomEnterResponse(matchRoom);
    }

    @Override
    @Transactional
    public PlayReadyResponse ready(Long characterId, Long matchId, PlayReadyRequest playReadyRequest) {
        MatchRoom matchRoom = matchRoomRepository.findById(matchId)
            .orElseThrow(() -> new MatchRoomNotFoundException(matchId));
        PlayerType playerType = matchRoom.getPlayerType(characterId);

        Boolean selfReadyStatus = playReadyRequest.getToggleSelfReadyStatus();
        Boolean opponentReadyStatus = playReadyRequest.getOpponentReadyStatus();
        Boolean hostReadyStatus;
        Boolean entrantReadyStatus;

        //플레이어 타입 별 준비 상태 set
        if(playerType.equals(HOST)) {
            hostReadyStatus = selfReadyStatus;
            entrantReadyStatus = opponentReadyStatus;
        }
        else if(playerType.equals(ENTRANT)){
            hostReadyStatus = opponentReadyStatus;
            entrantReadyStatus = selfReadyStatus;
        }
        else{
            throw new PlayerTypeInvalidException(characterId);
        }

        //매치 방 상태 변경
        if(hostReadyStatus && entrantReadyStatus){
            matchRoom.setMatchStatus(READY);
        }else {
            matchRoom.setMatchStatus(WAITING);
        }

        return new PlayReadyResponse(hostReadyStatus, entrantReadyStatus, matchRoom.getMatchStatus());
    }

    @Override
    public PlayGreetingResponse greeting(Long characterId) {
        return new PlayGreetingResponse(characterId + "님이 입장하였습니다.");
    }

    @Override
    @Transactional
    public PlayStartResponse start(Long characterId, Long matchId) {
        MatchRoom matchRoom = matchRoomRepository.findById(matchId)
            .orElseThrow(() -> new MatchRoomNotFoundException(matchId));

        Character host = matchRoom.getHost();
        Character entrant = matchRoom.getEntrant();

        //READY 이외의 상태일 경우 예외 처리
        if(!matchRoom.getMatchStatus().equals(READY)){
            throw new MatchStatusInvalidException(matchId);
        }

        //Player1, Player2 스탯과 스킬 리스트
        Stat hostTotalStat = characterService.getCharacterTotalStat(host);
        Stat entrantTotalStat = characterService.getCharacterTotalStat(entrant);

        List<CharacterSkillGetResponse> hostSkillList = characterService.getCharacterSkills(
            host.getCharacterId());
        List<CharacterSkillGetResponse> entrantSkillList = characterService.getCharacterSkills(
            entrant.getCharacterId());

        //선제 공격 플레이어 선정(spd 높은 플레이어)
        if(hostTotalStat.getSpd() > entrantTotalStat.getSpd()){
            matchRoom.setTurnOwner(HOST);
        }else{
            matchRoom.setTurnOwner(ENTRANT);
        }

        //Match 상태 업데이트
        matchRoom.setHostStatAndStartHp(hostTotalStat);
        matchRoom.setEntrantStatAndStartHp(entrantTotalStat);
        matchRoom.setMatchStatus(IN_PROGRESS);

        return new PlayStartResponse(hostTotalStat, entrantTotalStat, hostSkillList, entrantSkillList, matchRoom.getTurnOwner());
    }

    @Override
    @Transactional
    public PlayTurnResponse gameTurn(Long characterId, Long matchId,
        PlayTurnRequest playTurnRequest) {
        MatchRoom matchRoom = matchRoomRepository.findById(matchId)
            .orElseThrow(() -> new MatchRoomNotFoundException(matchId));
        Long skillId = playTurnRequest.getSkillId();
        Skill skill = skillRepository.findById(skillId).orElseThrow(()-> new SkillNotFoundException());

        PlayerType playerType = matchRoom.getPlayerType(characterId);

        //turn owner 요청자 비교 검증
        if(matchRoom.getTurnOwner() != playerType){
            throw new MatchRoomTurnInvalidException(characterId);
        }

        //IN_PROGRESS 이외의 상태일 경우 예외 처리
        if(!matchRoom.getMatchStatus().equals(IN_PROGRESS)){
            throw new MatchStatusInvalidException(matchId);
        }

        //skill 보유 여부 검증
        if(!characterSkillRepository.existsByCharacterCharacterIdAndSkillSkillId(characterId, skillId)){
            throw new CharacterSkillNotFoundException();
        }

        //skill 효과 처리
        List<SkillEffect> skillEffects = skillEffectRepository.findBySkillSkillId(skillId);

        for (SkillEffect effect : skillEffects) {
            if(playerType == ENTRANT){
                matchRoom.effectSkillCastByEntrant(effect);
            } else if (playerType == HOST) {
                matchRoom.effectSkillCastByHost(effect);
            }
        }


        //만약 둘 중 한 플레이어의 체력이 0또는 0 이하가 될 경우 게임 종료 상태 반환
        if(matchRoom.isGameOverWithStat()){
            return new PlayTurnResponse(true);
        }else{
            PlayerType toggleTurnOwner = matchRoom.getToggleTurnOwner();
            matchRoom.setTurnOwner(toggleTurnOwner);
            matchRoom.setMatchStatus(FINISHED);
            return new PlayTurnResponse(false, matchRoom.getHostStat(), matchRoom.getEntrantStat(),
                toggleTurnOwner, skill.getSkillNm());
        }
    }

    @Override
    @Transactional
    public PlayEndResponse gameEnd(Long characterId, Long matchId) {
        MatchRoom matchRoom = matchRoomRepository.findById(matchId)
            .orElseThrow(() -> new MatchRoomNotFoundException(matchId));

        PlayerType playerType = matchRoom.getPlayerType(characterId);

        //종료된 매치인지 확인
        if(matchRoom.getMatchStatus() != FINISHED){
            throw new MatchStatusInvalidException(matchRoom.getMatchRoomId());
        }

        //host 사용자의 요청만 허용
        if(playerType != HOST){
            throw new PlayerTypeNotHostException();
        }

        //winner & loser 확인
        Character winner = matchRoom.getWinner();
        Character loser = matchRoom.getLoser();

        PlayerType winnerType = matchRoom.getWinnerType();
        PlayerType loserType = togglePlayerType(winnerType);

        Integer winnerGold = winner.getLevelId() * matchRoom.getStakedGold();
        Integer loserGold = loser.getLevelId() * matchRoom.getStakedGold() / 5;
        Integer winnerExp = 100;
        Integer loserExp = 100 /5;

        //매칭 결과 정산 money & exp
        //TODO level up 이벤트 발생
        winner.plusMoney(winnerGold);
        winner.plusExp(winnerExp);
        loser.plusMoney(loserGold);
        loser.plusExp(loserExp);

        //match history 저장
        MatchHistory matchHistory = MatchHistory.builder().matchRoom(matchRoom).winner(winner)
            .loser(loser).stakedGold(matchRoom.getStakedGold()).build();
        matchHistoryRepository.save(matchHistory);

        //매칭 상태 초기화
        matchRoom.setInitMatchRoom();

        return new PlayEndResponse(winnerType, loserType, winner, loser, winnerGold, loserGold, winnerExp, loserExp);

    }
}